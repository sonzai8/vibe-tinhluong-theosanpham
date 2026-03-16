package com.plywood.payroll.service;

import com.plywood.payroll.entity.*;
import com.plywood.payroll.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

    @Transactional
    public Payroll calculatePayroll(int month, int year) {
        log.info("Bắt đầu tính lương tháng {}/{}", month, year);

        // Tạo hoặc lấy bản ghi Payroll
        Payroll payroll = payrollRepository.findByMonthAndYear(month, year)
                .orElseGet(() -> {
                    Payroll p = new Payroll();
                    p.setMonth(month);
                    p.setYear(year);
                    return payrollRepository.save(p);
                });

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
        // Map: teamId -> date -> [{employee, salaryCreditToOriginalTeam}]
        // Lưu trữ: originalTeamId -> date -> rented_fund (tiền lính thuê mang về)
        Map<Long, Map<LocalDate, BigDecimal>> rentedFundMap = new HashMap<>();

        // Lấy tất cả attendance trong tháng
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

        // Với mỗi tổ + ngày có production fund, tính giá trị mỗi đầu người
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
                        // Đây là lính đánh thuê -> tiền về tổ gốc
                        rentedFundMap
                                .computeIfAbsent(originalTeamId, k -> new HashMap<>())
                                .merge(date, valuePerWorker, BigDecimal::add);
                    }
                }

                // Lưu TeamDailyFund để báo cáo
                saveTeamDailyFund(actualTeamId, date, workingFund, BigDecimal.ZERO, headcount);
            }
        }

        // BƯỚC 4: Tổng hợp quỹ mỗi tổ gốc trong từng ngày và chia đều
        // Map: employeeId -> totalSalary
        Map<Long, BigDecimal> employeeSalaryMap = new HashMap<>();

        // Thu thập tất cả ngày làm của mỗi tổ gốc (những người có attendance trong tháng)
        Map<Long, Map<LocalDate, List<DailyAttendance>>> originalTeamDayMap = new HashMap<>();
        for (DailyAttendance att : allAttendances) {
            if (att.getOriginalTeam() == null) continue;
            Long origTeamId = att.getOriginalTeam().getId();
            originalTeamDayMap
                    .computeIfAbsent(origTeamId, k -> new HashMap<>())
                    .computeIfAbsent(att.getAttendanceDate(), k -> new ArrayList<>())
                    .add(att);
        }

        // Với mỗi tổ gốc + ngày, lấy quỹ own + rented rồi chia đều
        for (Map.Entry<Long, Map<LocalDate, List<DailyAttendance>>> teamEntry : originalTeamDayMap.entrySet()) {
            Long origTeamId = teamEntry.getKey();
            for (Map.Entry<LocalDate, List<DailyAttendance>> dateEntry : teamEntry.getValue().entrySet()) {
                LocalDate date = dateEntry.getKey();
                List<DailyAttendance> members = dateEntry.getValue();

                // Own fund: giá trị những thành viên tổ này làm tại chính tổ mình
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

        // BƯỚC 5: Tính phụ cấp chức vụ theo ngày
        Map<Long, BigDecimal> benefitMap = new HashMap<>();
        for (DailyAttendance att : allAttendances) {
            Long empId = att.getEmployee().getId();
            BigDecimal dailyBenefit = att.getEmployee().getRole() != null
                    ? att.getEmployee().getRole().getDailyBenefit()
                    : BigDecimal.ZERO;
            benefitMap.merge(empId, dailyBenefit, BigDecimal::add);
        }

        // BƯỚC 6: Tạo PayrollItem cho từng nhân viên
        Set<Long> involvedEmployees = new HashSet<>();
        involvedEmployees.addAll(employeeSalaryMap.keySet());
        involvedEmployees.addAll(benefitMap.keySet());

        for (Long empId : involvedEmployees) {
            Employee emp = employeeRepository.findById(empId).orElse(null);
            if (emp == null) continue;

            BigDecimal stepSalary = employeeSalaryMap.getOrDefault(empId, BigDecimal.ZERO);
            BigDecimal benefit = benefitMap.getOrDefault(empId, BigDecimal.ZERO);

            PayrollItem item = new PayrollItem();
            item.setPayroll(payroll);
            item.setEmployee(emp);
            item.setTotalStepSalary(stepSalary);
            item.setTotalBenefit(benefit);
            item.setNetSalary(stepSalary.add(benefit));
            payrollItemRepository.save(item);
        }

        log.info("Hoàn thành tính lương tháng {}/{}, có {} nhân viên", month, year, involvedEmployees.size());
        return payroll;
    }

    private BigDecimal calculateSurcharge(ProductQuality quality) {
        if (quality == null || quality.getLayers() == null) return BigDecimal.ZERO;
        return quality.getLayers().stream()
                .map(layer -> layer.getLayer().getSurchargePerLayer().multiply(BigDecimal.valueOf(layer.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void saveTeamDailyFund(Long teamId, LocalDate date, BigDecimal ownFund, BigDecimal rentedFund, int headcount) {
        // Chỉ lưu để báo cáo nếu cần
        log.debug("Team {} ngày {} - Own: {}, Rented: {}, Headcount: {}", teamId, date, ownFund, rentedFund, headcount);
    }

    public List<PayrollItem> getPayrollItems(int month, int year) {
        return payrollRepository.findByMonthAndYear(month, year)
                .map(p -> payrollItemRepository.findByPayrollId(p.getId()))
                .orElse(Collections.emptyList());
    }
}
