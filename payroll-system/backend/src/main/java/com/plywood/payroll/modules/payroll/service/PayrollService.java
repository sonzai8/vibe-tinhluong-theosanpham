package com.plywood.payroll.modules.payroll.service;
import com.plywood.payroll.modules.organization.repository.TeamDailyFundRepository;
import com.plywood.payroll.modules.production.repository.ProductionRecordRepository;
import com.plywood.payroll.modules.pricing.repository.ProductStepRateRepository;
import com.plywood.payroll.modules.penalty.repository.PenaltyBonusRepository;
import com.plywood.payroll.modules.payroll.repository.PayrollRepository;
import com.plywood.payroll.modules.payroll.repository.PayrollItemRepository;
import com.plywood.payroll.modules.payroll.repository.PayrollConfigRepository;
import com.plywood.payroll.modules.attendance.repository.DailyAttendanceRepository;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.employee.service.EmployeeService;
import com.plywood.payroll.modules.payroll.entity.PayrollItem;
import com.plywood.payroll.modules.payroll.entity.Payroll;
import com.plywood.payroll.modules.pricing.entity.ProductStepRate;
import com.plywood.payroll.modules.production.entity.ProductionRecord;
import com.plywood.payroll.modules.attendance.entity.DailyAttendance;
import com.plywood.payroll.modules.quality.entity.ProductQuality;
import com.plywood.payroll.modules.product.entity.Product;
import com.plywood.payroll.modules.employee.entity.Employee;

import com.plywood.payroll.modules.payroll.dto.response.PayrollItemResponse;
import com.plywood.payroll.modules.payroll.dto.response.PayrollResponse;
import com.plywood.payroll.modules.payroll.entity.PayrollConfig;
import com.plywood.payroll.modules.penalty.entity.PenaltyBonus;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.payroll.dto.response.PayrollDailyDetailResponse;
// REMOVED_WILDCARD_REPO_IMPORT - please update manually
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
    private final PayrollConfigRepository payrollConfigRepository;
    private final EmployeeService employeeService;

    private static record RecordCalculation(long quantity, BigDecimal priceHigh, BigDecimal priceLow, BigDecimal surcharge) {}

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

        if (!"DRAFT".equals(payroll.getStatus())) {
            throw new RuntimeException("Bảng lương đã chốt, không thể tính lại");
        }

        // payrollItemRepository.deleteByPayrollId(payroll.getId());
        // Thay vì xóa tất cả, chúng ta chỉ cập nhật những cái chưa chốt

        // Lấy cấu hình chuyên cần hiệu lực cho tháng này
        int minAttendanceDays = Integer.parseInt(
            payrollConfigRepository.findEffectiveConfig("MIN_ATTENDANCE_DAYS", month, year)
                .map(PayrollConfig::getConfigValue)
                .orElse("22")
        );

        BigDecimal filmSurcharge1 = new BigDecimal(
            payrollConfigRepository.findEffectiveConfig("FILM_SURCHARGE_1_SIDE", month, year)
                .map(PayrollConfig::getConfigValue).orElse("0")
        );
        BigDecimal filmSurcharge2 = new BigDecimal(
            payrollConfigRepository.findEffectiveConfig("FILM_SURCHARGE_2_SIDE", month, year)
                .map(PayrollConfig::getConfigValue).orElse("0")
        );

        // BƯỚC 1: Lấy dữ liệu sản lượng và chấm công
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<ProductionRecord> records = productionRecordRepository.findByFilters(startDate, endDate, null, null);
        List<DailyAttendance> allAttendances = dailyAttendanceRepository.findByFilters(null, null, month, year, null, null, null);

        // Xét chuyên cần
        Map<Long, Long> attendanceCountMap = allAttendances.stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId(), Collectors.counting()));

        // BƯỚC 2: Tổ chức sản lượng theo Team và Ngày
        Map<Long, Map<LocalDate, List<RecordCalculation>>> teamDateCalcMap = new HashMap<>();
        for (ProductionRecord record : records) {
            Long teamId = record.getTeam().getId();
            LocalDate date = record.getProductionDate();

            Optional<ProductStepRate> rateOpt = productStepRateRepository
                    .findEffectiveRate(record.getProduct().getId(), record.getTeam().getProductionStep().getId(), record.getQuality().getId(), date);

            if (rateOpt.isEmpty()) {
                log.warn("Không tìm thấy đơn giá cho sản phẩm {} công đoạn {} chất lượng {} ngày {}", 
                    record.getProduct().getId(), record.getTeam().getProductionStep().getId(), record.getQuality().getId(), date);
                continue;
            }

            teamDateCalcMap
                    .computeIfAbsent(teamId, k -> new HashMap<>())
                    .computeIfAbsent(date, k -> new ArrayList<>())
                    .add(new RecordCalculation(record.getQuantity(), rateOpt.get().getPriceHigh(), rateOpt.get().getPriceLow(), 
                        calculateSurcharge(record.getProduct(), record.getQuality(), filmSurcharge1, filmSurcharge2)));
        }

        // BƯỚC 3: Tính thu nhập hàng ngày cho từng nhân viên
        // Map: employeeId -> LocalDate -> Salary
        Map<Long, Map<LocalDate, BigDecimal>> employeeDailySalaryMap = new HashMap<>();

        // Nhóm attendance theo ngày và tổ thực tế để biết ai làm việc ở đâu
        Map<Long, Map<LocalDate, List<DailyAttendance>>> actualTeamDayWorkers = new HashMap<>();
        for (DailyAttendance att : allAttendances) {
            if (att.getActualTeam() == null) continue;
            actualTeamDayWorkers
                    .computeIfAbsent(att.getActualTeam().getId(), k -> new HashMap<>())
                    .computeIfAbsent(att.getAttendanceDate(), k -> new ArrayList<>())
                    .add(att);
        }

        // Với mỗi tổ có sản lượng, chia đều lương cho những người thực tế làm việc tại đó
        for (Map.Entry<Long, Map<LocalDate, List<RecordCalculation>>> teamEntry : teamDateCalcMap.entrySet()) {
            Long actualTeamId = teamEntry.getKey();
            for (Map.Entry<LocalDate, List<RecordCalculation>> dateEntry : teamEntry.getValue().entrySet()) {
                LocalDate date = dateEntry.getKey();
                List<RecordCalculation> calcs = dateEntry.getValue();

                List<DailyAttendance> workersOnSite = actualTeamDayWorkers
                        .getOrDefault(actualTeamId, Collections.emptyMap())
                        .getOrDefault(date, Collections.emptyList());

                int headcount = workersOnSite.size();
                if (headcount == 0) continue;

                for (DailyAttendance att : workersOnSite) {
                    Long empId = att.getEmployee().getId();
                    boolean isDiligent = attendanceCountMap.getOrDefault(empId, 0L) >= minAttendanceDays;

                    BigDecimal dailyEarnings = BigDecimal.ZERO;
                    for (RecordCalculation c : calcs) {
                        BigDecimal rate = isDiligent ? c.priceHigh : c.priceLow;
                        BigDecimal share = rate.add(c.surcharge)
                                .multiply(BigDecimal.valueOf(c.quantity))
                                .divide(BigDecimal.valueOf(headcount), 2, RoundingMode.HALF_UP);
                        dailyEarnings = dailyEarnings.add(share);
                    }

                    employeeDailySalaryMap
                            .computeIfAbsent(empId, k -> new HashMap<>())
                            .merge(date, dailyEarnings, BigDecimal::add);
                }
            }
        }

        // BƯỚC 4: Tổng hợp lương tháng
        Map<Long, BigDecimal> employeeSalaryMap = new HashMap<>();
        for (Map.Entry<Long, Map<LocalDate, BigDecimal>> empEntry : employeeDailySalaryMap.entrySet()) {
            BigDecimal totalMonthSalary = empEntry.getValue().values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            employeeSalaryMap.put(empEntry.getKey(), totalMonthSalary);
        }

        // BƯỚC 5: Tính phụ cấp chức vụ (theo số ngày đi làm thực tế)
        Map<Long, BigDecimal> benefitMap = new HashMap<>();
        for (DailyAttendance att : allAttendances) {
            Long empId = att.getEmployee().getId();
            BigDecimal dailyBenefit = att.getEmployee().getRole() != null
                    ? att.getEmployee().getRole().getDailyBenefit()
                    : BigDecimal.ZERO;
            benefitMap.merge(empId, dailyBenefit, BigDecimal::add);
        }

        // BƯỚC 6: Lấy khen thưởng/kỷ luật trong tháng
        Map<Long, BigDecimal> bonusMap = new HashMap<>();
        Map<Long, BigDecimal> penaltyMap = new HashMap<>();
        
        List<PenaltyBonus> penaltyBonuses = penaltyBonusRepository.findAll();
        for (PenaltyBonus pb : penaltyBonuses) {
            if (!pb.getRecordDate().isBefore(startDate) && !pb.getRecordDate().isAfter(endDate)) {
                Long empId = pb.getEmployee().getId();
                if (pb.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
                    bonusMap.merge(empId, pb.getAmount(), BigDecimal::add);
                } else {
                    penaltyMap.merge(empId, pb.getAmount().abs(), BigDecimal::add);
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

            // Chỉ tạo/cập nhật nếu chưa CONFIRMED
            PayrollItem item = payrollItemRepository.findByPayrollIdAndEmployeeId(payroll.getId(), empId)
                    .orElse(new PayrollItem());
            
            if ("CONFIRMED".equals(item.getStatus())) continue;

            item.setPayroll(payroll);
            item.setEmployee(emp);
            item.setTotalStepSalary(stepSalary);
            item.setTotalBenefit(benefit);
            item.setTotalBonus(bonus);
            item.setTotalPenalty(penalty);
            item.setNetSalary(netSalary);
            item.setStatus("DRAFT");
            payrollItemRepository.save(item);
        }

        log.info("Hoàn thành tính lương tháng {}/{}, có {} nhân viên", month, year, involvedEmployees.size());
        return mapToResponse(payroll);
    }

    private BigDecimal calculateSurcharge(Product product, ProductQuality quality, BigDecimal surcharge1, BigDecimal surcharge2) {
        BigDecimal totalSurcharge = BigDecimal.ZERO;
        
        // 1. Phụ phí chất lượng (Layers)
        if (quality != null && quality.getLayers() != null) {
            totalSurcharge = quality.getLayers().stream()
                .map(layer -> layer.getLayer().getSurchargePerLayer().multiply(BigDecimal.valueOf(layer.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 2. Phụ phí Phủ phim
        if (product != null && product.getFilmCoatingType() != null) {
            switch (product.getFilmCoatingType()) {
                case SIDE_1 -> totalSurcharge = totalSurcharge.add(surcharge1);
                case SIDE_2 -> totalSurcharge = totalSurcharge.add(surcharge2);
                case NONE -> {}
            }
        }
        
        return totalSurcharge;
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
        
        if (entity.getPayroll() != null) {
            response.setPayrollId(entity.getPayroll().getId());
            response.setMonth(entity.getPayroll().getMonth());
            response.setYear(entity.getPayroll().getYear());
            response.setStatus(entity.getPayroll().getStatus());
        }
        
        if (entity.getEmployee() != null) {
            response.setEmployeeName(entity.getEmployee().getFullName());
            response.setEmployeeCode(entity.getEmployee().getCode());
            if (entity.getEmployee().getDepartment() != null) {
                response.setDepartmentId(entity.getEmployee().getDepartment().getId());
                response.setDepartmentName(entity.getEmployee().getDepartment().getName());
            }
            if (entity.getEmployee().getTeam() != null) {
                response.setTeamId(entity.getEmployee().getTeam().getId());
                response.setTeamName(entity.getEmployee().getTeam().getName());
            }
        }
        
        response.setProductSalary(entity.getTotalStepSalary());
        response.setBenefitSalary(entity.getTotalBenefit());
        
        // totalPenaltyBonus = bonus - penalty
        BigDecimal bonus = entity.getTotalBonus() != null ? entity.getTotalBonus() : BigDecimal.ZERO;
        BigDecimal penalty = entity.getTotalPenalty() != null ? entity.getTotalPenalty() : BigDecimal.ZERO;
        response.setTotalPenaltyBonus(bonus.subtract(penalty));
        
        response.setTotalSalary(entity.getNetSalary());
        response.setStatus(entity.getStatus());
        return response;
    }

    @Transactional(readOnly = true)
    public List<PayrollDailyDetailResponse> getDailyDetails(Long payrollItemId) {
        PayrollItem item = payrollItemRepository.findById(payrollItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi lương", payrollItemId));
        
        Employee employee = item.getEmployee();
        Payroll payroll = item.getPayroll();
        
        LocalDate startDate = LocalDate.of(payroll.getYear(), payroll.getMonth(), 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        // Lấy tất cả chấm công để tính chuyên cần (giống calculatePayroll)
        List<DailyAttendance> allAttendances = dailyAttendanceRepository.findByFilters(null, null, payroll.getMonth(), payroll.getYear(), null, null, null);
        Map<Long, Long> attendanceCountMap = allAttendances.stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId(), Collectors.counting()));

        // Chấm công riêng của nhân viên này
        List<DailyAttendance> attendances = dailyAttendanceRepository.findByEmployeeIdAndAttendanceDateBetween(
                employee.getId(), startDate, endDate);
        
        List<PenaltyBonus> penaltyBonuses = penaltyBonusRepository.findByEmployeeIdAndRecordDateBetween(
                employee.getId(), startDate, endDate);
        
        List<ProductionRecord> records = productionRecordRepository.findByProductionDateBetween(startDate, endDate);
        
        Map<LocalDate, DailyAttendance> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(DailyAttendance::getAttendanceDate, a -> a));
        
        Map<LocalDate, List<PenaltyBonus>> pbMap = penaltyBonuses.stream()
                .collect(Collectors.groupingBy(PenaltyBonus::getRecordDate));

        List<PayrollDailyDetailResponse> details = new ArrayList<>();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DailyAttendance att = attendanceMap.get(date);
            List<PenaltyBonus> pbs = pbMap.getOrDefault(date, Collections.emptyList());
            // Lấy cấu hình phụ phí cho tháng này
            BigDecimal filmSurcharge1 = new BigDecimal(
                payrollConfigRepository.findEffectiveConfig("FILM_SURCHARGE_1_SIDE", payroll.getMonth(), payroll.getYear())
                    .map(PayrollConfig::getConfigValue).orElse("0")
            );
            BigDecimal filmSurcharge2 = new BigDecimal(
                payrollConfigRepository.findEffectiveConfig("FILM_SURCHARGE_2_SIDE", payroll.getMonth(), payroll.getYear())
                    .map(PayrollConfig::getConfigValue).orElse("0")
            );

            // Lấy cấu hình chuyên cần
            int minAttendanceDays = Integer.parseInt(
                payrollConfigRepository.findEffectiveConfig("MIN_ATTENDANCE_DAYS", payroll.getMonth(), payroll.getYear())
                    .map(PayrollConfig::getConfigValue)
                    .orElse("22")
            );
            
            BigDecimal productSalary = BigDecimal.ZERO;
            if (att != null && att.getActualTeam() != null) {
                Long teamId = att.getActualTeam().getId();
                LocalDate finalDate = date;
                List<ProductionRecord> dayRecords = records.stream()
                        .filter(r -> r.getProductionDate().equals(finalDate) && r.getTeam().getId().equals(teamId))
                        .collect(Collectors.toList());
                
                boolean isDiligent = attendanceCountMap.getOrDefault(employee.getId(), 0L) >= minAttendanceDays;

                for (ProductionRecord r : dayRecords) {
                    BigDecimal surcharge = calculateSurcharge(r.getProduct(), r.getQuality(), BigDecimal.ZERO, BigDecimal.ZERO);
                    Optional<ProductStepRate> rateOpt = productStepRateRepository
                            .findEffectiveRate(r.getProduct().getId(), r.getTeam().getProductionStep().getId(), r.getQuality().getId(), finalDate);
                    
                    if (rateOpt.isPresent()) {
                        BigDecimal rate = isDiligent ? rateOpt.get().getPriceHigh() : rateOpt.get().getPriceLow();
                        BigDecimal totalMoney = rate.add(surcharge).multiply(BigDecimal.valueOf(r.getQuantity()));
                        
                        long headcount = dailyAttendanceRepository.findByActualTeamIdAndAttendanceDate(teamId, finalDate).size();
                        if (headcount > 0) {
                            productSalary = productSalary.add(totalMoney.divide(BigDecimal.valueOf(headcount), 2, RoundingMode.HALF_UP));
                        }
                    }
                }
            }

            BigDecimal bonus = pbs.stream().map(PenaltyBonus::getAmount).filter(a -> a.compareTo(BigDecimal.ZERO) >= 0).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal penalty = pbs.stream().map(PenaltyBonus::getAmount).filter(a -> a.compareTo(BigDecimal.ZERO) < 0).map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);

            details.add(PayrollDailyDetailResponse.builder()
                    .date(date)
                    .attendanceSymbol(att != null && att.getAttendanceDefinition() != null ? att.getAttendanceDefinition().getCode() : "-")
                    .teamName(att != null && att.getActualTeam() != null ? att.getActualTeam().getName() : "N/A")
                    .productSalary(productSalary)
                    .benefitSalary(att != null ? (employee.getRole() != null ? employee.getRole().getDailyBenefit() : BigDecimal.ZERO) : BigDecimal.ZERO)
                    .bonus(bonus)
                    .penalty(penalty)
                    .note(att != null ? "" : "Nghỉ")
                    .build());
        }
        
        return details;
    }

    @Transactional
    public void confirmByTeam(int year, int month, Long teamId) {
        Payroll payroll = payrollRepository.findByMonthAndYear(month, year)
                .orElseThrow(() -> new ResourceNotFoundException("Bảng lương", 0L));
        
        List<PayrollItem> items = payrollItemRepository.findByPayrollId(payroll.getId());
        for (PayrollItem item : items) {
            if (item.getEmployee().getTeam() != null && item.getEmployee().getTeam().getId().equals(teamId)) {
                item.setStatus("CONFIRMED");
                payrollItemRepository.save(item);
            }
        }
    }
}
