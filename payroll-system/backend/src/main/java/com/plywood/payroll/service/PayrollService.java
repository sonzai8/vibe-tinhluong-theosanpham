package com.plywood.payroll.service;

import com.plywood.payroll.dto.response.PayrollItemResponse;
import com.plywood.payroll.dto.response.PayrollResponse;
import com.plywood.payroll.entity.*;
import com.plywood.payroll.exception.ResourceNotFoundException;
import com.plywood.payroll.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final PayrollItemRepository payrollItemRepository;
    private final ProductionRecordRepository productionRecordRepository;
    private final ProductStepRateRepository productStepRateRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final TeamDailyFundRepository teamDailyFundRepository;
    private final EmployeeRepository employeeRepository;
    private final PenaltyBonusRepository penaltyBonusRepository;
    private final EmployeeService employeeService;

    @Transactional
    public PayrollResponse calculatePayroll(int month, int year) {
        log.info("Bắt đầu tính lương tháng {}/{}", month, year);

        // Tạo hoặc lấy bản ghi Payroll
        Payroll payroll = payrollRepository.findByMonthAndYear(month, year)
                .orElseGet(() -> {
                    Payroll p = new Payroll();
                    p.setMonth(month);
                    p.setYear(year);
                    return payrollRepository.save(p);
                });

        // Chỉ cho phép tính lại nếu đang DRAFT
        if (!"DRAFT".equals(payroll.getStatus())) {
            throw new RuntimeException("Bảng lương đã chốt, không thể tính lại");
        }

        // Xóa kết quả cũ nếu tính lại
        payrollItemRepository.deleteByPayrollId(payroll.getId());

        // BƯỚC 1: Lấy tất cả production records trong tháng
        List<ProductionRecord> records = productionRecordRepository.findByMonthAndYear(month, year);

        // BƯỚC 2: Tính quỹ lương cho từng tổ trong từng ngày
        // Map: teamId -> date -> fund
        Map<Long, Map<LocalDate, BigDecimal>> teamDateFundMap = new HashMap<>();

        for (ProductionRecord record : records) {
            Long teamId = record.getTeam().getId();
            LocalDate date = record.getProductionDate();

            // Lấy đơn giá cơ bản
            Optional<ProductStepRate> rateOpt = productStepRateRepository
                    .findEffectiveRate(record.getProduct().getId(), record.getTeam().getProductionStep().getId(), date);

            if (rateOpt.isEmpty()) {
                log.warn("Không tìm thấy đơn giá cho team {} ngày {}", teamId, date);
                continue;
            }

            BigDecimal basePrice = rateOpt.get().getBasePrice();

            // Tính phụ phí chất lượng
            BigDecimal surcharge = calculateSurcharge(record.getQuality());

            BigDecimal totalPricePerUnit = basePrice.add(surcharge);
            BigDecimal stepFund = totalPricePerUnit.multiply(BigDecimal.valueOf(record.getQuantity()));

            teamDateFundMap
                    .computeIfAbsent(teamId, k -> new HashMap<>())
                    .merge(date, stepFund, BigDecimal::add);
        }

        // BƯỚC 3: Luân chuyển tiền lính đánh thuê - tính "giá trị mỗi người" tại tổ thực tế
        Map<Long, Map<LocalDate, BigDecimal>> rentedFundMap = new HashMap<>();
        List<DailyAttendance> allAttendances = dailyAttendanceRepository.findByMonthAndYear(month, year);
        
        // Nhóm theo ngày và tổ thực tế
        Map<Long, Map<LocalDate, List<DailyAttendance>>> actualTeamDayMap = new HashMap<>();
        for (DailyAttendance att : allAttendances) {
            if (att.getActualTeam() == null) continue;
            Long actualTeamId = att.getActualTeam().getId();
            actualTeamDayMap
                    .computeIfAbsent(actualTeamId, k -> new HashMap<>())
                    .computeIfAbsent(att.getAttendanceDate(), k -> new ArrayList<>())
                    .add(att);
        }

        for (Map.Entry<Long, Map<LocalDate, BigDecimal>> teamEntry : teamDateFundMap.entrySet()) {
            Long actualTeamId = teamEntry.getKey();
            for (Map.Entry<LocalDate, BigDecimal> dateEntry : teamEntry.getValue().entrySet()) {
                LocalDate date = dateEntry.getKey();
                BigDecimal workingFund = dateEntry.getValue();

                List<DailyAttendance> workersOnSite = actualTeamDayMap
                        .getOrDefault(actualTeamId, Collections.emptyMap())
                        .getOrDefault(date, Collections.emptyList());

                int headcount = workersOnSite.size();
                if (headcount == 0) continue;

                BigDecimal valuePerWorker = workingFund.divide(BigDecimal.valueOf(headcount), 2, RoundingMode.HALF_UP);

                // Phân phối tiền về đúng tổ gốc của từng thành viên làm ở đây
                for (DailyAttendance att : workersOnSite) {
                    if (att.getOriginalTeam() == null) continue;
                    Long originalTeamId = att.getOriginalTeam().getId();

                    if (!originalTeamId.equals(actualTeamId)) {
                        rentedFundMap
                                .computeIfAbsent(originalTeamId, k -> new HashMap<>())
                                .merge(date, valuePerWorker, BigDecimal::add);
                    }
                }
            }
        }

        // BƯỚC 4: Tổng hợp quỹ mỗi tổ gốc trong từng ngày và chia đều
        Map<Long, BigDecimal> employeeSalaryMap = new HashMap<>();
        Map<Long, Map<LocalDate, List<DailyAttendance>>> originalTeamDayMap = new HashMap<>();
        for (DailyAttendance att : allAttendances) {
            if (att.getOriginalTeam() == null) continue;
            Long origTeamId = att.getOriginalTeam().getId();
            originalTeamDayMap
                    .computeIfAbsent(origTeamId, k -> new HashMap<>())
                    .computeIfAbsent(att.getAttendanceDate(), k -> new ArrayList<>())
                    .add(att);
        }

        for (Map.Entry<Long, Map<LocalDate, List<DailyAttendance>>> teamEntry : originalTeamDayMap.entrySet()) {
            Long origTeamId = teamEntry.getKey();
            for (Map.Entry<LocalDate, List<DailyAttendance>> dateEntry : teamEntry.getValue().entrySet()) {
                LocalDate date = dateEntry.getKey();
                List<DailyAttendance> members = dateEntry.getValue();

                BigDecimal ownFund = BigDecimal.ZERO;
                if (teamDateFundMap.containsKey(origTeamId) && teamDateFundMap.get(origTeamId).containsKey(date)) {
                    long ownWorkerCount = members.stream()
                            .filter(a -> a.getActualTeam() != null && a.getActualTeam().getId().equals(origTeamId))
                            .count();
                    BigDecimal workingFund = teamDateFundMap.get(origTeamId).get(date);
                    int totalAtActualTeam = actualTeamDayMap.getOrDefault(origTeamId, Collections.emptyMap())
                            .getOrDefault(date, Collections.emptyList()).size();
                    if (totalAtActualTeam > 0) {
                        BigDecimal valuePerWorker = workingFund.divide(BigDecimal.valueOf(totalAtActualTeam), 2, RoundingMode.HALF_UP);
                        ownFund = valuePerWorker.multiply(BigDecimal.valueOf(ownWorkerCount));
                    }
                }

                BigDecimal rentedFund = rentedFundMap
                        .getOrDefault(origTeamId, Collections.emptyMap())
                        .getOrDefault(date, BigDecimal.ZERO);

                BigDecimal totalFund = ownFund.add(rentedFund);
                int headcount = members.size();
                if (headcount == 0 || totalFund.compareTo(BigDecimal.ZERO) == 0) continue;

                BigDecimal perPerson = totalFund.divide(BigDecimal.valueOf(headcount), 2, RoundingMode.HALF_UP);

                for (DailyAttendance att : members) {
                    Long empId = att.getEmployee().getId();
                    employeeSalaryMap.merge(empId, perPerson, BigDecimal::add);
                }
            }
        }

        // BƯỚC 5: Tính phụ cấp chức vụ
        Map<Long, BigDecimal> benefitMap = new HashMap<>();
        for (DailyAttendance att : allAttendances) {
            Long empId = att.getEmployee().getId();
            BigDecimal dailyBenefit = att.getEmployee().getRole() != null
                    ? att.getEmployee().getRole().getDailyBenefit()
                    : BigDecimal.ZERO;
            benefitMap.merge(empId, dailyBenefit, BigDecimal::add);
        }

        // BƯỚC 6: Lấy khen thưởng/kỷ luật trong tháng
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        
        Map<Long, BigDecimal> bonusMap = new HashMap<>();
        Map<Long, BigDecimal> penaltyMap = new HashMap<>();
        
        List<PenaltyBonus> penaltyBonuses = penaltyBonusRepository.findAll(); // Lọc ở BE cho nhanh với DB nhỏ
        for (PenaltyBonus pb : penaltyBonuses) {
            if (!pb.getRecordDate().isBefore(startDate) && !pb.getRecordDate().isAfter(endDate)) {
                Long empId = pb.getEmployee().getId();
                if (pb.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
                    bonusMap.merge(empId, pb.getAmount(), BigDecimal::add);
                } else {
                    penaltyMap.merge(empId, pb.getAmount().abs(), BigDecimal::add); // Lưu số dương cho penalty
                }
            }
        }

        // BƯỚC 7: Tạo PayrollItem
        Set<Long> involvedEmployees = new HashSet<>();
        involvedEmployees.addAll(employeeSalaryMap.keySet());
        involvedEmployees.addAll(benefitMap.keySet());

        for (Long empId : involvedEmployees) {
            Employee emp = employeeRepository.findById(empId).orElse(null);
            if (emp == null) continue;

            BigDecimal stepSalary = employeeSalaryMap.getOrDefault(empId, BigDecimal.ZERO);
            BigDecimal benefit = benefitMap.getOrDefault(empId, BigDecimal.ZERO);
            BigDecimal bonus = bonusMap.getOrDefault(empId, BigDecimal.ZERO);
            BigDecimal penalty = penaltyMap.getOrDefault(empId, BigDecimal.ZERO);
            
            BigDecimal netSalary = stepSalary.add(benefit).add(bonus).subtract(penalty);

            PayrollItem item = new PayrollItem();
            item.setPayroll(payroll);
            item.setEmployee(emp);
            item.setTotalStepSalary(stepSalary);
            item.setTotalBenefit(benefit);
            item.setTotalBonus(bonus);
            item.setTotalPenalty(penalty);
            item.setNetSalary(netSalary);
            payrollItemRepository.save(item);
        }

        log.info("Hoàn thành tính lương tháng {}/{}, có {} nhân viên", month, year, involvedEmployees.size());
        return mapToResponse(payroll);
    }

    private BigDecimal calculateSurcharge(ProductQuality quality) {
        if (quality == null || quality.getLayers() == null) return BigDecimal.ZERO;
        return quality.getLayers().stream()
                .map(layer -> layer.getLayer().getSurchargePerLayer().multiply(BigDecimal.valueOf(layer.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<PayrollItemResponse> getPayrollItems(int month, int year) {
        return payrollRepository.findByMonthAndYear(month, year)
                .map(p -> payrollItemRepository.findByPayrollId(p.getId()).stream()
                            .map(this::mapItemToResponse)
                            .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }
    
    @Transactional
    public PayrollResponse confirmPayroll(Long id) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bảng lương", id));
        payroll.setStatus("CONFIRMED");
        return mapToResponse(payrollRepository.save(payroll));
    }

    public PayrollResponse mapToResponse(Payroll entity) {
        if (entity == null) return null;
        PayrollResponse response = new PayrollResponse();
        response.setId(entity.getId());
        response.setMonth(entity.getMonth());
        response.setYear(entity.getYear());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
    
    public PayrollItemResponse mapItemToResponse(PayrollItem entity) {
        if (entity == null) return null;
        PayrollItemResponse response = new PayrollItemResponse();
        response.setId(entity.getId());
        response.setPayroll(mapToResponse(entity.getPayroll()));
        response.setEmployee(employeeService.mapToResponse(entity.getEmployee()));
        response.setTotalStepSalary(entity.getTotalStepSalary());
        response.setTotalBenefit(entity.getTotalBenefit());
        response.setTotalBonus(entity.getTotalBonus());
        response.setTotalPenalty(entity.getTotalPenalty());
        response.setNetSalary(entity.getNetSalary());
        return response;
    }
}
