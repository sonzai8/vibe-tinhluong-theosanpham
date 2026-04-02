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
import com.plywood.payroll.modules.employee.entity.SalaryProcess;
import com.plywood.payroll.modules.employee.entity.TeamProcess;
import com.plywood.payroll.modules.employee.repository.SalaryProcessRepository;
import com.plywood.payroll.modules.employee.repository.TeamProcessRepository;
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
import java.util.ArrayList;
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
    private final SalaryProcessRepository salaryProcessRepository;
    private final TeamProcessRepository teamProcessRepository;

    private static record RecordCalculation(long quantity, BigDecimal priceHigh, BigDecimal priceLow,
            BigDecimal surcharge) {
    }

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
                        .orElse("22"));

        BigDecimal filmSurcharge1 = new BigDecimal(
                payrollConfigRepository.findEffectiveConfig("FILM_SURCHARGE_1_SIDE", month, year)
                        .map(PayrollConfig::getConfigValue).orElse("0"));
        BigDecimal filmSurcharge2 = new BigDecimal(
                payrollConfigRepository.findEffectiveConfig("FILM_SURCHARGE_2_SIDE", month, year)
                        .map(PayrollConfig::getConfigValue).orElse("0"));

        // BƯỚC 1: Lấy dữ liệu sản lượng và chấm công
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<ProductionRecord> records = productionRecordRepository.findAll((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.greaterThanOrEqualTo(root.get("productionDate"), startDate));
            predicates.add(cb.lessThanOrEqualTo(root.get("productionDate"), endDate));
            if (query != null) {
                query.orderBy(cb.desc(root.get("productionDate")), cb.desc(root.get("createdAt")));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
        List<DailyAttendance> allAttendances = dailyAttendanceRepository.findAll((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.greaterThanOrEqualTo(root.get("attendanceDate"), startDate));
            predicates.add(cb.lessThanOrEqualTo(root.get("attendanceDate"), endDate));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });

        // Xét chuyên cần
        Map<Long, BigDecimal> attendanceCountMap = allAttendances.stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId(),
                        Collectors.reducing(BigDecimal.ZERO,
                                a -> BigDecimal.valueOf(a.getAttendanceDefinition() != null
                                        ? a.getAttendanceDefinition().getMultiplier()
                                        : 1.0),
                                BigDecimal::add)));

        // BƯỚC 2: Tổ chức sản lượng theo Team và Ngày
        Map<Long, Map<LocalDate, List<RecordCalculation>>> teamDateCalcMap = new HashMap<>();
        for (ProductionRecord record : records) {
            Long teamId = record.getTeam().getId();
            LocalDate date = record.getProductionDate();

            Optional<ProductStepRate> rateOpt = productStepRateRepository
                    .findEffectiveRate(record.getProduct().getId(), record.getTeam().getProductionStep().getId(),
                            record.getQuality().getId(), date);

            if (rateOpt.isEmpty()) {
                log.warn("Không tìm thấy đơn giá cho sản phẩm {} công đoạn {} chất lượng {} ngày {}",
                        record.getProduct().getId(), record.getTeam().getProductionStep().getId(),
                        record.getQuality().getId(), date);
                continue;
            }

            teamDateCalcMap
                    .computeIfAbsent(teamId, k -> new HashMap<>())
                    .computeIfAbsent(date, k -> new ArrayList<>())
                    .add(new RecordCalculation(record.getQuantity(), rateOpt.get().getPriceHigh(),
                            rateOpt.get().getPriceLow(),
                            calculateSurcharge(record.getProduct(), record.getQuality(), filmSurcharge1,
                                    filmSurcharge2)));
        }

        // BƯỚC 3: Tính thu nhập hàng ngày cho từng nhân viên
        // Map: employeeId -> LocalDate -> Salary
        Map<Long, Map<LocalDate, BigDecimal>> employeeDailySalaryMap = new HashMap<>();

        // Nhóm attendance theo ngày và tổ thực tế để biết ai làm việc ở đâu
        Map<Long, Map<LocalDate, List<DailyAttendance>>> actualTeamDayWorkers = new HashMap<>();
        for (DailyAttendance att : allAttendances) {
            if (att.getActualTeam() == null)
                continue;
            actualTeamDayWorkers
                    .computeIfAbsent(att.getActualTeam().getId(), k -> new HashMap<>())
                    .computeIfAbsent(att.getAttendanceDate(), k -> new ArrayList<>())
                    .add(att);
        }

        // Với mỗi tổ có sản lượng, chia đều lương cho những người thực tế làm việc tại
        // đó
        for (Map.Entry<Long, Map<LocalDate, List<RecordCalculation>>> teamEntry : teamDateCalcMap.entrySet()) {
            Long actualTeamId = teamEntry.getKey();
            for (Map.Entry<LocalDate, List<RecordCalculation>> dateEntry : teamEntry.getValue().entrySet()) {
                LocalDate date = dateEntry.getKey();
                List<RecordCalculation> calcs = dateEntry.getValue();

                List<DailyAttendance> workersOnSite = actualTeamDayWorkers
                        .getOrDefault(actualTeamId, Collections.emptyMap())
                        .getOrDefault(date, Collections.emptyList());

                BigDecimal totalTeamMultiplier = workersOnSite.stream()
                        .map(a -> BigDecimal.valueOf(
                                a.getAttendanceDefinition() != null ? a.getAttendanceDefinition().getMultiplier()
                                        : 1.0))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalTeamMultiplier.compareTo(BigDecimal.ZERO) <= 0)
                    continue;

                for (DailyAttendance att : workersOnSite) {
                    Long empId = att.getEmployee().getId();
                    BigDecimal userMultiplier = BigDecimal.valueOf(
                            att.getAttendanceDefinition() != null ? att.getAttendanceDefinition().getMultiplier()
                                    : 1.0);
                    BigDecimal weightedDays = attendanceCountMap.getOrDefault(empId, BigDecimal.ZERO);
                    boolean isDiligent = weightedDays.compareTo(BigDecimal.valueOf(minAttendanceDays)) >= 0;

                    BigDecimal dailyEarnings = BigDecimal.ZERO;
                    for (RecordCalculation c : calcs) {
                        BigDecimal rate = isDiligent ? c.priceHigh : c.priceLow;
                        BigDecimal share = rate.add(c.surcharge)
                                .multiply(BigDecimal.valueOf(c.quantity))
                                .multiply(userMultiplier)
                                .divide(totalTeamMultiplier, 2, RoundingMode.HALF_UP);
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
            BigDecimal dayMultiplier = BigDecimal.valueOf(
                    att.getAttendanceDefinition() != null ? att.getAttendanceDefinition().getMultiplier() : 1.0);
            BigDecimal weightedBenefit = dailyBenefit.multiply(dayMultiplier);
            benefitMap.merge(empId, weightedBenefit, BigDecimal::add);
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
        involvedEmployees
                .addAll(allAttendances.stream().map(a -> a.getEmployee().getId()).collect(Collectors.toList()));

        // Lấy số ngày làm việc tiêu chuẩn (cho lương tháng)
        int standardWorkingDays = Integer.parseInt(
                payrollConfigRepository.findEffectiveConfig("STANDARD_WORKING_DAYS", month, year)
                        .map(PayrollConfig::getConfigValue)
                        .orElse("26"));

        for (Long empId : involvedEmployees) {
            Employee emp = employeeRepository.findById(empId).orElse(null);
            if (emp == null)
                continue;

            // Fetch effective salary and team processes for this employee in this month
            List<SalaryProcess> salaries = salaryProcessRepository.findOverlapping(empId, startDate, endDate);

            BigDecimal stepSalary = BigDecimal.ZERO;

            // TÍNH LƯƠNG CƠ BẢN DỰA TRÊN LOẠI LƯƠNG
            // Với lương tháng cố định, chúng ta cần tính theo từng giai đoạn (nếu có đổi
            // lương mid-month)
            if (salaries.isEmpty()) {
                // Fallback nếu không có history (ví dụ data cũ chưa migrate)
                stepSalary = employeeSalaryMap.getOrDefault(empId, BigDecimal.ZERO);
            } else {
                for (SalaryProcess s : salaries) {
                    LocalDate sStart = s.getStartDate().isBefore(startDate) ? startDate : s.getStartDate();
                    LocalDate sEnd = (s.getEndDate() == null || s.getEndDate().isAfter(endDate)) ? endDate
                            : s.getEndDate();

                    // Tính số công trong giai đoạn này
                    BigDecimal weightedDaysInPeriod = allAttendances.stream()
                            .filter(a -> a.getEmployee().getId().equals(empId))
                            .filter(a -> !a.getAttendanceDate().isBefore(sStart)
                                    && !a.getAttendanceDate().isAfter(sEnd))
                            .map(a -> BigDecimal.valueOf(
                                    a.getAttendanceDefinition() != null ? a.getAttendanceDefinition().getMultiplier()
                                            : 1.0))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    if (s.getSalaryType() == com.plywood.payroll.modules.employee.entity.SalaryType.FIXED_MONTHLY) {
                        if (standardWorkingDays > 0) {
                            BigDecimal periodSalary = s.getBaseSalary()
                                    .multiply(weightedDaysInPeriod)
                                    .divide(BigDecimal.valueOf(standardWorkingDays), 2, RoundingMode.HALF_UP);
                            stepSalary = stepSalary.add(periodSalary);
                        }
                    } else if (s
                            .getSalaryType() == com.plywood.payroll.modules.employee.entity.SalaryType.FIXED_DAILY) {
                        stepSalary = stepSalary.add(s.getBaseSalary().multiply(weightedDaysInPeriod));
                    } else {
                        // PRODUCT_BASED - lương sản phẩm đã được tính theo ngày ở BƯỚC 3-4
                        // Ở đây chúng ta chỉ lấy phần lương sản phẩm thuộc giai đoạn này (nếu
                        // SalaryType của giai đoạn đó là PRODUCT_BASED)
                        BigDecimal productInPeriod = employeeDailySalaryMap.getOrDefault(empId, Collections.emptyMap())
                                .entrySet().stream()
                                .filter(e -> !e.getKey().isBefore(sStart) && !e.getKey().isAfter(sEnd))
                                .map(Map.Entry::getValue)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        stepSalary = stepSalary.add(productInPeriod);
                    }
                }
            }

            BigDecimal benefit = benefitMap.getOrDefault(empId, BigDecimal.ZERO);
            BigDecimal bonus = bonusMap.getOrDefault(empId, BigDecimal.ZERO);
            BigDecimal penalty = penaltyMap.getOrDefault(empId, BigDecimal.ZERO);

            BigDecimal netSalary = stepSalary.add(benefit).add(bonus).subtract(penalty);

            // CHIA TÁCH BẢO HIỂM VÀ TIỀN MẶT (Lấy mức đóng của bản ghi SalaryProcess mới
            // nhất trong tháng)
            BigDecimal insuranceSalaryConfig = salaries.stream()
                    .sorted(Comparator.comparing(SalaryProcess::getStartDate).reversed())
                    .map(SalaryProcess::getInsuranceSalary)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);

            BigDecimal insuranceSalary = insuranceSalaryConfig;
            if (insuranceSalary.compareTo(netSalary) > 0) {
                insuranceSalary = netSalary;
            }
            if (insuranceSalary.compareTo(BigDecimal.ZERO) < 0) {
                insuranceSalary = BigDecimal.ZERO;
            }
            BigDecimal cashSalary = netSalary.subtract(insuranceSalary);

            // Chỉ tạo/cập nhật nếu chưa CONFIRMED
            PayrollItem item = payrollItemRepository.findByPayrollIdAndEmployeeId(payroll.getId(), empId)
                    .orElse(new PayrollItem());

            if ("CONFIRMED".equals(item.getStatus()))
                continue;

            item.setPayroll(payroll);
            item.setEmployee(emp);
            item.setTotalStepSalary(stepSalary);
            item.setTotalBenefit(benefit);
            item.setTotalBonus(bonus);
            item.setTotalPenalty(penalty);
            item.setNetSalary(netSalary);
            item.setInsuranceSalary(insuranceSalary);
            item.setCashSalary(cashSalary);
            item.setStatus("DRAFT");
            payrollItemRepository.save(item);
        }

        log.info("Hoàn thành tính lương tháng {}/{}, có {} nhân viên", month, year, involvedEmployees.size());
        return mapToResponse(payroll);
    }

    private BigDecimal calculateSurcharge(Product product, ProductQuality quality, BigDecimal surcharge1,
            BigDecimal surcharge2) {
        BigDecimal totalSurcharge = BigDecimal.ZERO;

        // 1. Phụ phí chất lượng (Layers)
        if (quality != null && quality.getLayers() != null) {
            totalSurcharge = quality.getLayers().stream()
                    .map(layer -> layer.getLayer().getSurchargePerLayer()
                            .multiply(BigDecimal.valueOf(layer.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 2. Phụ phí Phủ phim
        if (product != null && product.getFilmCoatingType() != null) {
            switch (product.getFilmCoatingType()) {
                case SIDE_1 -> totalSurcharge = totalSurcharge.add(surcharge1);
                case SIDE_2 -> totalSurcharge = totalSurcharge.add(surcharge2);
                case NONE -> {
                }
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
        if (entity == null)
            return null;
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
        if (entity == null)
            return null;
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
            // Lấy tổ hiện tại (hoặc tổ mới nhất trong tháng lương đó)
            LocalDate lastDay = LocalDate.of(entity.getPayroll().getYear(), entity.getPayroll().getMonth(), 1)
                    .plusMonths(1).minusDays(1);
            teamProcessRepository.findEffectiveByDate(entity.getEmployee().getId(), lastDay)
                    .ifPresent(tp -> {
                        response.setTeamId(tp.getTeam().getId());
                        response.setTeamName(tp.getTeam().getName());
                    });
        }

        response.setProductSalary(entity.getTotalStepSalary());
        response.setBenefitSalary(entity.getTotalBenefit());

        // totalPenaltyBonus = bonus - penalty
        BigDecimal bonus = entity.getTotalBonus() != null ? entity.getTotalBonus() : BigDecimal.ZERO;
        BigDecimal penalty = entity.getTotalPenalty() != null ? entity.getTotalPenalty() : BigDecimal.ZERO;
        response.setTotalPenaltyBonus(bonus.subtract(penalty));

        response.setTotalSalary(entity.getNetSalary());
        response.setInsuranceSalary(entity.getInsuranceSalary());
        response.setCashSalary(entity.getCashSalary());
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
        List<DailyAttendance> allAttendances = dailyAttendanceRepository.findAll((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.greaterThanOrEqualTo(root.get("attendanceDate"), startDate));
            predicates.add(cb.lessThanOrEqualTo(root.get("attendanceDate"), endDate));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
        Map<Long, BigDecimal> attendanceCountMap = allAttendances.stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId(),
                        Collectors.reducing(BigDecimal.ZERO,
                                a -> BigDecimal.valueOf(a.getAttendanceDefinition() != null
                                        ? a.getAttendanceDefinition().getMultiplier()
                                        : 1.0),
                                BigDecimal::add)));

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
                    payrollConfigRepository
                            .findEffectiveConfig("FILM_SURCHARGE_1_SIDE", payroll.getMonth(), payroll.getYear())
                            .map(PayrollConfig::getConfigValue).orElse("0"));
            BigDecimal filmSurcharge2 = new BigDecimal(
                    payrollConfigRepository
                            .findEffectiveConfig("FILM_SURCHARGE_2_SIDE", payroll.getMonth(), payroll.getYear())
                            .map(PayrollConfig::getConfigValue).orElse("0"));

            // Lấy cấu hình chuyên cần
            int minAttendanceDays = Integer.parseInt(
                    payrollConfigRepository
                            .findEffectiveConfig("MIN_ATTENDANCE_DAYS", payroll.getMonth(), payroll.getYear())
                            .map(PayrollConfig::getConfigValue)
                            .orElse("22"));

            BigDecimal productSalary = BigDecimal.ZERO;
            if (att != null && att.getActualTeam() != null) {
                Long actualTeamId = att.getActualTeam().getId();
                LocalDate finalDate = date;
                List<ProductionRecord> dayRecords = records.stream()
                        .filter(r -> r.getProductionDate().equals(finalDate)
                                && r.getTeam().getId().equals(actualTeamId))
                        .collect(Collectors.toList());

                boolean isDiligent = attendanceCountMap.getOrDefault(employee.getId(), BigDecimal.ZERO)
                        .compareTo(BigDecimal.valueOf(minAttendanceDays)) >= 0;

                for (ProductionRecord r : dayRecords) {
                    BigDecimal surcharge = calculateSurcharge(r.getProduct(), r.getQuality(), filmSurcharge1,
                            filmSurcharge2);
                    Optional<ProductStepRate> rateOpt = productStepRateRepository
                            .findEffectiveRate(r.getProduct().getId(), r.getTeam().getProductionStep().getId(),
                                    r.getQuality().getId(), finalDate);

                    if (rateOpt.isPresent()) {
                        BigDecimal rate = isDiligent ? rateOpt.get().getPriceHigh() : rateOpt.get().getPriceLow();
                        BigDecimal totalMoney = rate.add(surcharge).multiply(BigDecimal.valueOf(r.getQuantity()));

                        List<DailyAttendance> workersInTeam = dailyAttendanceRepository
                                .findByActualTeamIdAndAttendanceDate(actualTeamId, finalDate);
                        BigDecimal totalTeamMultiplier = workersInTeam.stream()
                                .map(a -> BigDecimal.valueOf(a.getAttendanceDefinition() != null
                                        ? a.getAttendanceDefinition().getMultiplier()
                                        : 1.0))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        if (totalTeamMultiplier.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal userMultiplier = BigDecimal.valueOf(att.getAttendanceDefinition() != null
                                    ? att.getAttendanceDefinition().getMultiplier()
                                    : 1.0);
                            productSalary = productSalary.add(totalMoney.multiply(userMultiplier)
                                    .divide(totalTeamMultiplier, 2, RoundingMode.HALF_UP));
                        }
                    }
                }
            }

            BigDecimal bonus = pbs.stream().map(PenaltyBonus::getAmount).filter(a -> a.compareTo(BigDecimal.ZERO) >= 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal penalty = pbs.stream().map(PenaltyBonus::getAmount).filter(a -> a.compareTo(BigDecimal.ZERO) < 0)
                    .map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);

            details.add(PayrollDailyDetailResponse.builder()
                    .date(date)
                    .attendanceSymbol(att != null && att.getAttendanceDefinition() != null
                            ? att.getAttendanceDefinition().getCode()
                            : "-")
                    .teamName(att != null && att.getActualTeam() != null ? att.getActualTeam().getName() : "N/A")
                    .productSalary(productSalary)
                    .benefitSalary(att != null ? (employee.getRole() != null
                            ? employee.getRole().getDailyBenefit()
                                    .multiply(BigDecimal.valueOf(att.getAttendanceDefinition() != null
                                            ? att.getAttendanceDefinition().getMultiplier()
                                            : 1.0))
                            : BigDecimal.ZERO) : BigDecimal.ZERO)
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
        LocalDate checkDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);

        for (PayrollItem item : items) {
            final Long empId = item.getEmployee().getId();
            Optional<TeamProcess> tp = teamProcessRepository.findEffectiveByDate(empId, checkDate);
            if (tp.isPresent() && tp.get().getTeam().getId().equals(teamId)) {
                item.setStatus("CONFIRMED");
                payrollItemRepository.save(item);
            }
        }
    }

    @Transactional
    public void resetMonthlyData(int month, int year) {
        Payroll payroll = payrollRepository.findByMonthAndYear(month, year)
                .orElse(null);

        // Nếu đã có bản ghi lương và đã chốt thì không cho xóa
        if (payroll != null && "CONFIRMED".equals(payroll.getStatus())) {
            throw new RuntimeException("Bảng lương tháng " + month + "/" + year + " đã chốt, không thể xóa dữ liệu!");
        }

        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        log.info("Bắt đầu reset dữ liệu tháng {}/{}", month, year);

        // 1. Xóa chi tiết lương (PayrollItem)
        if (payroll != null) {
            payrollItemRepository.deleteByPayrollId(payroll.getId());
        }

        // // 2. Xóa chấm công
        // List<DailyAttendance> attendances = dailyAttendanceRepository.findAll((root,
        // query, cb) -> {
        // List<jakarta.persistence.criteria.Predicate> p = new ArrayList<>();
        // p.add(cb.greaterThanOrEqualTo(root.get("attendanceDate"), startDate));
        // p.add(cb.lessThanOrEqualTo(root.get("attendanceDate"), endDate));
        // return cb.and(p.toArray(new jakarta.persistence.criteria.Predicate[0]));
        // });
        // dailyAttendanceRepository.deleteAll(attendances);

        // // 3. Xóa nhật ký sản xuất
        // List<ProductionRecord> records =
        // productionRecordRepository.findByProductionDateBetween(startDate, endDate);
        // productionRecordRepository.deleteAll(records);

        // // 4. Xóa khen thưởng kỷ luật (sử dụng query tìm kiếm theo ngày)
        // List<PenaltyBonus> pbs = penaltyBonusRepository.findByFilters(startDate,
        // endDate, null, null);
        // penaltyBonusRepository.deleteAll(pbs);

        log.info("Hoàn thành reset dữ liệu tháng {}/{}", month, year);
    }
}
