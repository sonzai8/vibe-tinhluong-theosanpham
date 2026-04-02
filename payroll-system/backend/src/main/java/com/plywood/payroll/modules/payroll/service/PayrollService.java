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
import com.plywood.payroll.modules.employee.repository.SalaryProcessRepository;
import com.plywood.payroll.modules.employee.repository.TeamProcessRepository;
import com.plywood.payroll.modules.payroll.entity.Payroll;
import com.plywood.payroll.modules.pricing.entity.ProductStepRate;
import com.plywood.payroll.modules.production.entity.ProductionRecord;
import com.plywood.payroll.modules.attendance.entity.DailyAttendance;
import com.plywood.payroll.modules.quality.entity.ProductQuality;
import com.plywood.payroll.modules.product.entity.Product;
import com.plywood.payroll.modules.employee.entity.Employee;
import com.plywood.payroll.modules.payroll.dto.response.*;
import com.plywood.payroll.modules.payroll.entity.PayrollConfig;
import com.plywood.payroll.modules.penalty.entity.PenaltyBonus;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.organization.entity.Team;
import com.plywood.payroll.modules.organization.repository.TeamRepository;
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
    private final EmployeeRepository employeeRepository;
    private final PenaltyBonusRepository penaltyBonusRepository;
    private final PayrollConfigRepository payrollConfigRepository;
    private final SalaryProcessRepository salaryProcessRepository;
    private final TeamProcessRepository teamProcessRepository;
    private final TeamRepository teamRepository;

    private static record RecordCalculation(long quantity, BigDecimal priceHigh, BigDecimal priceLow,
            BigDecimal surcharge) {
    }

    @Transactional
    public PayrollResponse calculatePayroll(int month, int year) {
        log.info("Bắt đầu tính lương tháng {}/{}", month, year);

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

        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<ProductionRecord> records = productionRecordRepository.findAll((root, query, cb) -> {
            return cb.between(root.get("productionDate"), startDate, endDate);
        });
        List<DailyAttendance> allAttendances = dailyAttendanceRepository.findAll((root, query, cb) -> {
            return cb.between(root.get("attendanceDate"), startDate, endDate);
        });

        Map<Long, BigDecimal> attendanceCountMap = allAttendances.stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId(),
                        Collectors.reducing(BigDecimal.ZERO,
                                a -> BigDecimal.valueOf(a.getAttendanceDefinition() != null
                                        ? a.getAttendanceDefinition().getMultiplier()
                                        : 1.0),
                                BigDecimal::add)));

        Map<Long, Map<LocalDate, List<RecordCalculation>>> teamDateCalcMap = new HashMap<>();
        for (ProductionRecord record : records) {
            Long teamId = record.getTeam().getId();
            LocalDate date = record.getProductionDate();

            Optional<ProductStepRate> rateOpt = productStepRateRepository
                    .findEffectiveRate(record.getProduct().getId(), record.getTeam().getProductionStep().getId(),
                            record.getQuality().getId(), date);

            if (rateOpt.isEmpty()) continue;

            teamDateCalcMap
                    .computeIfAbsent(teamId, k -> new HashMap<>())
                    .computeIfAbsent(date, k -> new ArrayList<>())
                    .add(new RecordCalculation(record.getQuantity(), rateOpt.get().getPriceHigh(),
                            rateOpt.get().getPriceLow(),
                            calculateSurcharge(record.getProduct(), record.getQuality(), filmSurcharge1,
                                    filmSurcharge2)));
        }

        Map<Long, Map<LocalDate, BigDecimal>> employeeDailySalaryMap = new HashMap<>();
        Map<Long, Map<LocalDate, List<DailyAttendance>>> actualTeamDayWorkers = new HashMap<>();
        for (DailyAttendance att : allAttendances) {
            if (att.getActualTeam() == null) continue;
            actualTeamDayWorkers
                    .computeIfAbsent(att.getActualTeam().getId(), k -> new HashMap<>())
                    .computeIfAbsent(att.getAttendanceDate(), k -> new ArrayList<>())
                    .add(att);
        }

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

                if (totalTeamMultiplier.compareTo(BigDecimal.ZERO) <= 0) continue;

                for (DailyAttendance att : workersOnSite) {
                    Long empId = att.getEmployee().getId();
                    BigDecimal userMultiplier = BigDecimal.valueOf(
                            att.getAttendanceDefinition() != null ? att.getAttendanceDefinition().getMultiplier()
                                    : 1.0);
                    boolean isDiligent = attendanceCountMap.getOrDefault(empId, BigDecimal.ZERO).compareTo(BigDecimal.valueOf(minAttendanceDays)) >= 0;

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


        Map<Long, BigDecimal> employeeSalaryMap = new HashMap<>();
        for (Map.Entry<Long, Map<LocalDate, BigDecimal>> empEntry : employeeDailySalaryMap.entrySet()) {
            BigDecimal totalMonthSalary = empEntry.getValue().values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            employeeSalaryMap.put(empEntry.getKey(), totalMonthSalary);
        }

        Map<Long, BigDecimal> benefitMap = new HashMap<>();
        for (DailyAttendance att : allAttendances) {
            Long empId = att.getEmployee().getId();
            BigDecimal dailyBenefit = att.getEmployee().getRole() != null
                    ? att.getEmployee().getRole().getDailyBenefit()
                    : BigDecimal.ZERO;
            BigDecimal dayMultiplier = BigDecimal.valueOf(
                    att.getAttendanceDefinition() != null ? att.getAttendanceDefinition().getMultiplier() : 1.0);
            benefitMap.merge(empId, dailyBenefit.multiply(dayMultiplier), BigDecimal::add);
        }

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

        Set<Long> involvedEmployees = new HashSet<>();
        involvedEmployees.addAll(employeeSalaryMap.keySet());
        involvedEmployees.addAll(benefitMap.keySet());
        involvedEmployees.addAll(allAttendances.stream().map(a -> a.getEmployee().getId()).collect(Collectors.toList()));

        int standardWorkingDays = Integer.parseInt(
                payrollConfigRepository.findEffectiveConfig("STANDARD_WORKING_DAYS", month, year)
                        .map(PayrollConfig::getConfigValue)
                        .orElse("26"));

        for (Long empId : involvedEmployees) {
            Employee emp = employeeRepository.findById(empId).orElse(null);
            if (emp == null) continue;

            List<SalaryProcess> salaries = salaryProcessRepository.findOverlapping(empId, startDate, endDate);
            BigDecimal stepSalary = BigDecimal.ZERO;

            if (salaries.isEmpty()) {
                stepSalary = employeeSalaryMap.getOrDefault(empId, BigDecimal.ZERO);
            } else {
                for (SalaryProcess s : salaries) {
                    LocalDate sStart = s.getStartDate().isBefore(startDate) ? startDate : s.getStartDate();
                    LocalDate sEnd = (s.getEndDate() == null || s.getEndDate().isAfter(endDate)) ? endDate : s.getEndDate();

                    BigDecimal weightedDaysInPeriod = allAttendances.stream()
                            .filter(a -> a.getEmployee().getId().equals(empId))
                            .filter(a -> !a.getAttendanceDate().isBefore(sStart) && !a.getAttendanceDate().isAfter(sEnd))
                            .map(a -> BigDecimal.valueOf(a.getAttendanceDefinition() != null ? a.getAttendanceDefinition().getMultiplier() : 1.0))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    if (s.getSalaryType() == com.plywood.payroll.modules.employee.entity.SalaryType.FIXED_MONTHLY) {
                        if (standardWorkingDays > 0) {
                            stepSalary = stepSalary.add(s.getBaseSalary().multiply(weightedDaysInPeriod).divide(BigDecimal.valueOf(standardWorkingDays), 2, RoundingMode.HALF_UP));
                        }
                    } else if (s.getSalaryType() == com.plywood.payroll.modules.employee.entity.SalaryType.FIXED_DAILY) {
                        stepSalary = stepSalary.add(s.getBaseSalary().multiply(weightedDaysInPeriod));
                    } else {
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
            BigDecimal netSalary = stepSalary.add(benefit).add(bonusMap.getOrDefault(empId, BigDecimal.ZERO)).subtract(penaltyMap.getOrDefault(empId, BigDecimal.ZERO));

            BigDecimal insuranceSalaryConfig = salaries.stream()
                    .sorted(Comparator.comparing(SalaryProcess::getStartDate).reversed())
                    .map(SalaryProcess::getInsuranceSalary)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);

            BigDecimal insuranceSalary = insuranceSalaryConfig.compareTo(netSalary) > 0 ? netSalary : (insuranceSalaryConfig.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : insuranceSalaryConfig);

            PayrollItem item = payrollItemRepository.findByPayrollIdAndEmployeeId(payroll.getId(), empId).orElse(new PayrollItem());
            if ("CONFIRMED".equals(item.getStatus())) continue;

            item.setPayroll(payroll);
            item.setEmployee(emp);
            item.setTotalStepSalary(stepSalary);
            item.setTotalBenefit(benefit);
            item.setTotalBonus(bonusMap.getOrDefault(empId, BigDecimal.ZERO));
            item.setTotalPenalty(penaltyMap.getOrDefault(empId, BigDecimal.ZERO));
            item.setNetSalary(netSalary);
            item.setInsuranceSalary(insuranceSalary);
            item.setCashSalary(netSalary.subtract(insuranceSalary));
            item.setStatus("DRAFT");
            payrollItemRepository.save(item);
        }
        return mapToResponse(payroll);
    }

    private BigDecimal calculateSurcharge(Product product, ProductQuality quality, BigDecimal surcharge1, BigDecimal surcharge2) {
        BigDecimal totalSurcharge = BigDecimal.ZERO;
        if (quality != null && quality.getLayers() != null) {
            totalSurcharge = quality.getLayers().stream()
                    .map(layer -> layer.getLayer().getSurchargePerLayer().multiply(BigDecimal.valueOf(layer.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
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
        Payroll payroll = payrollRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bảng lương", id));
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
            LocalDate lastDay = LocalDate.of(entity.getPayroll().getYear(), entity.getPayroll().getMonth(), 1).plusMonths(1).minusDays(1);
            teamProcessRepository.findEffectiveByDate(entity.getEmployee().getId(), lastDay).ifPresent(tp -> {
                response.setTeamId(tp.getTeam().getId());
                response.setTeamName(tp.getTeam().getName());
            });
        }
        response.setProductSalary(entity.getTotalStepSalary());
        response.setBenefitSalary(entity.getTotalBenefit());
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
        PayrollItem item = payrollItemRepository.findById(payrollItemId).orElseThrow(() -> new ResourceNotFoundException("Bản ghi lương", payrollItemId));
        Employee employee = item.getEmployee();
        Payroll payroll = item.getPayroll();
        LocalDate startDate = LocalDate.of(payroll.getYear(), payroll.getMonth(), 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<DailyAttendance> allAttendances = dailyAttendanceRepository.findAll((root, query, cb) -> cb.between(root.get("attendanceDate"), startDate, endDate));
        Map<Long, BigDecimal> attendanceCountMap = allAttendances.stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId(),
                        Collectors.reducing(BigDecimal.ZERO,
                                a -> BigDecimal.valueOf(a.getAttendanceDefinition() != null ? a.getAttendanceDefinition().getMultiplier() : 1.0),
                                BigDecimal::add)));

        List<DailyAttendance> attendances = dailyAttendanceRepository.findByEmployeeIdAndAttendanceDateBetween(employee.getId(), startDate, endDate);
        List<PenaltyBonus> penaltyBonuses = penaltyBonusRepository.findByEmployeeIdAndRecordDateBetween(employee.getId(), startDate, endDate);
        List<ProductionRecord> records = productionRecordRepository.findByProductionDateBetween(startDate, endDate);

        Map<LocalDate, DailyAttendance> attendanceMap = attendances.stream().collect(Collectors.toMap(DailyAttendance::getAttendanceDate, a -> a));
        Map<LocalDate, List<PenaltyBonus>> pbMap = penaltyBonuses.stream().collect(Collectors.groupingBy(PenaltyBonus::getRecordDate));

        List<PayrollDailyDetailResponse> details = new ArrayList<>();
        BigDecimal filmSurcharge1 = new BigDecimal(payrollConfigRepository.findEffectiveConfig("FILM_SURCHARGE_1_SIDE", payroll.getMonth(), payroll.getYear()).map(PayrollConfig::getConfigValue).orElse("0"));
        BigDecimal filmSurcharge2 = new BigDecimal(payrollConfigRepository.findEffectiveConfig("FILM_SURCHARGE_2_SIDE", payroll.getMonth(), payroll.getYear()).map(PayrollConfig::getConfigValue).orElse("0"));
        int minAttendanceDays = Integer.parseInt(payrollConfigRepository.findEffectiveConfig("MIN_ATTENDANCE_DAYS", payroll.getMonth(), payroll.getYear()).map(PayrollConfig::getConfigValue).orElse("22"));

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DailyAttendance att = attendanceMap.get(date);
            BigDecimal productSalary = BigDecimal.ZERO;
            if (att != null && att.getActualTeam() != null) {
                Long actualTeamId = att.getActualTeam().getId();
                LocalDate finalDate = date;
                List<ProductionRecord> dayRecords = records.stream().filter(r -> r.getProductionDate().equals(finalDate) && r.getTeam().getId().equals(actualTeamId)).collect(Collectors.toList());
                boolean isDiligent = attendanceCountMap.getOrDefault(employee.getId(), BigDecimal.ZERO).compareTo(BigDecimal.valueOf(minAttendanceDays)) >= 0;

                for (ProductionRecord r : dayRecords) {
                    BigDecimal surcharge = calculateSurcharge(r.getProduct(), r.getQuality(), filmSurcharge1, filmSurcharge2);
                    Optional<ProductStepRate> rateOpt = productStepRateRepository.findEffectiveRate(r.getProduct().getId(), r.getTeam().getProductionStep().getId(), r.getQuality().getId(), finalDate);
                    if (rateOpt.isPresent()) {
                        BigDecimal rate = isDiligent ? rateOpt.get().getPriceHigh() : rateOpt.get().getPriceLow();
                        BigDecimal totalMoney = rate.add(surcharge).multiply(BigDecimal.valueOf(r.getQuantity()));
                        List<DailyAttendance> workersInTeam = dailyAttendanceRepository.findByActualTeamIdAndAttendanceDate(actualTeamId, finalDate);
                        BigDecimal totalTeamMultiplier = workersInTeam.stream().map(a -> BigDecimal.valueOf(a.getAttendanceDefinition() != null ? a.getAttendanceDefinition().getMultiplier() : 1.0)).reduce(BigDecimal.ZERO, BigDecimal::add);
                        if (totalTeamMultiplier.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal userMultiplier = BigDecimal.valueOf(att.getAttendanceDefinition() != null ? att.getAttendanceDefinition().getMultiplier() : 1.0);
                            productSalary = productSalary.add(totalMoney.multiply(userMultiplier).divide(totalTeamMultiplier, 2, RoundingMode.HALF_UP));
                        }
                    }
                }
            }
            List<PenaltyBonus> pbs = pbMap.getOrDefault(date, Collections.emptyList());
            BigDecimal bonus = pbs.stream().map(PenaltyBonus::getAmount).filter(a -> a.compareTo(BigDecimal.ZERO) >= 0).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal penalty = pbs.stream().map(PenaltyBonus::getAmount).filter(a -> a.compareTo(BigDecimal.ZERO) < 0).map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);

            details.add(PayrollDailyDetailResponse.builder()
                    .date(date)
                    .attendanceSymbol(att != null && att.getAttendanceDefinition() != null ? att.getAttendanceDefinition().getCode() : "-")
                    .teamName(att != null && att.getActualTeam() != null ? att.getActualTeam().getName() : "N/A")
                    .productSalary(productSalary)
                    .benefitSalary(att != null ? (employee.getRole() != null ? employee.getRole().getDailyBenefit().multiply(BigDecimal.valueOf(att.getAttendanceDefinition() != null ? att.getAttendanceDefinition().getMultiplier() : 1.0)) : BigDecimal.ZERO) : BigDecimal.ZERO)
                    .bonus(bonus)
                    .penalty(penalty)
                    .note(att != null ? "" : "Nghỉ")
                    .build());
        }
        return details;
    }

    @Transactional
    public void confirmByTeam(int year, int month, Long teamId) {
        Payroll payroll = payrollRepository.findByMonthAndYear(month, year).orElseThrow(() -> new ResourceNotFoundException("Bảng lương", 0L));
        List<PayrollItem> items = payrollItemRepository.findByPayrollId(payroll.getId());
        LocalDate checkDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        for (PayrollItem item : items) {
            teamProcessRepository.findEffectiveByDate(item.getEmployee().getId(), checkDate).ifPresent(tp -> {
                if (tp.getTeam().getId().equals(teamId)) {
                    item.setStatus("CONFIRMED");
                    payrollItemRepository.save(item);
                }
            });
        }
    }

    @Transactional
    public void resetMonthlyData(int month, int year) {
        Payroll payroll = payrollRepository.findByMonthAndYear(month, year).orElse(null);
        if (payroll != null && "CONFIRMED".equals(payroll.getStatus())) {
            throw new RuntimeException("Bảng lương tháng " + month + "/" + year + " đã chốt, không thể xóa dữ liệu!");
        }
        if (payroll != null) {
            payrollItemRepository.deleteByPayrollId(payroll.getId());
        }
        log.info("Hoàn thành reset dữ liệu tháng {}/{}", month, year);
    }

    @Transactional(readOnly = true)
    public List<TeamWageResponse> getTeamWages(int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<Team> allTeams = teamRepository.findAll();
        List<ProductionRecord> allRecords = productionRecordRepository.findAll((root, query, cb) -> cb.between(root.get("productionDate"), startDate, endDate));
        List<DailyAttendance> allAttendances = dailyAttendanceRepository.findAll((root, query, cb) -> cb.between(root.get("attendanceDate"), startDate, endDate));

        BigDecimal filmSurcharge1 = new BigDecimal(payrollConfigRepository.findEffectiveConfig("FILM_SURCHARGE_1_SIDE", month, year).map(PayrollConfig::getConfigValue).orElse("0"));
        BigDecimal filmSurcharge2 = new BigDecimal(payrollConfigRepository.findEffectiveConfig("FILM_SURCHARGE_2_SIDE", month, year).map(PayrollConfig::getConfigValue).orElse("0"));
        int minAttendanceDays = Integer.parseInt(payrollConfigRepository.findEffectiveConfig("MIN_ATTENDANCE_DAYS", month, year).map(PayrollConfig::getConfigValue).orElse("22"));

        Map<Long, BigDecimal> attendanceCountMap = allAttendances.stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId(),
                        Collectors.reducing(BigDecimal.ZERO,
                                a -> BigDecimal.valueOf(a.getAttendanceDefinition() != null ? a.getAttendanceDefinition().getMultiplier() : 1.0),
                                BigDecimal::add)));

        List<TeamWageResponse> responseList = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;
            Map<Long, List<DailyAttendance>> teamWorkers = allAttendances.stream()
                    .filter(a -> a.getAttendanceDate().equals(currentDate) && a.getActualTeam() != null)
                    .collect(Collectors.groupingBy(a -> a.getActualTeam().getId()));

            for (Team team : allTeams) {
                Long teamId = team.getId();
                List<RecordCalculation> calcs = allRecords.stream()
                        .filter(r -> r.getProductionDate().equals(currentDate) && r.getTeam().getId().equals(teamId))
                        .map(r -> {
                            Optional<ProductStepRate> rateOpt = productStepRateRepository.findEffectiveRate(r.getProduct().getId(), r.getTeam().getProductionStep().getId(), r.getQuality().getId(), currentDate);
                            return rateOpt.map(rate -> new RecordCalculation(r.getQuantity(), rate.getPriceHigh(), rate.getPriceLow(), calculateSurcharge(r.getProduct(), r.getQuality(), filmSurcharge1, filmSurcharge2))).orElse(null);
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                BigDecimal totalTeamMultiplier = teamWorkers.getOrDefault(teamId, Collections.emptyList()).stream()
                        .map(a -> BigDecimal.valueOf(a.getAttendanceDefinition() != null ? a.getAttendanceDefinition().getMultiplier() : 1.0))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal internalLaborCost = BigDecimal.ZERO;
                BigDecimal borrowedLaborCost = BigDecimal.ZERO;
                List<TeamWageResponse.WorkerWageDetail> details = new ArrayList<>();

                if (totalTeamMultiplier.compareTo(BigDecimal.ZERO) > 0) {
                    for (DailyAttendance att : teamWorkers.get(teamId)) {
                        Employee emp = att.getEmployee();
                        BigDecimal userMultiplier = BigDecimal.valueOf(att.getAttendanceDefinition() != null ? att.getAttendanceDefinition().getMultiplier() : 1.0);
                        boolean isDiligent = attendanceCountMap.getOrDefault(emp.getId(), BigDecimal.ZERO).compareTo(BigDecimal.valueOf(minAttendanceDays)) >= 0;
                        BigDecimal dailyEarnings = BigDecimal.ZERO;
                        for (RecordCalculation c : calcs) {
                            BigDecimal rate = isDiligent ? c.priceHigh : c.priceLow;
                            dailyEarnings = dailyEarnings.add(rate.add(c.surcharge).multiply(BigDecimal.valueOf(c.quantity)).multiply(userMultiplier).divide(totalTeamMultiplier, 2, RoundingMode.HALF_UP));
                        }
                        boolean isBorrowed = att.getOriginalTeam() != null && !att.getOriginalTeam().getId().equals(teamId);
                        if (isBorrowed) borrowedLaborCost = borrowedLaborCost.add(dailyEarnings);
                        else internalLaborCost = internalLaborCost.add(dailyEarnings);

                        details.add(TeamWageResponse.WorkerWageDetail.builder()
                                .employeeId(emp.getId()).employeeName(emp.getFullName()).employeeCode(emp.getCode())
                                .originalTeamName(att.getOriginalTeam() != null ? att.getOriginalTeam().getName() : "N/A")
                                .actualTeamName(team.getName())
                                .amount(isBorrowed ? dailyEarnings.negate() : dailyEarnings)
                                .isBorrowed(isBorrowed)
                                .status(isBorrowed ? "BORROWED" : "INTERNAL")
                                .build());
                    }
                }

                BigDecimal lendLaborCost = BigDecimal.ZERO;
                List<DailyAttendance> lendAttendances = allAttendances.stream()
                        .filter(a -> a.getAttendanceDate().equals(currentDate) && a.getOriginalTeam() != null && a.getOriginalTeam().getId().equals(teamId) && a.getActualTeam() != null && !a.getActualTeam().getId().equals(teamId))
                        .toList();

                for (DailyAttendance att : lendAttendances) {
                    Long altTeamId = att.getActualTeam().getId();
                    List<DailyAttendance> workersInAltTeam = teamWorkers.getOrDefault(altTeamId, Collections.emptyList());
                    BigDecimal sumMult = workersInAltTeam.stream().map(a -> BigDecimal.valueOf(a.getAttendanceDefinition() != null ? a.getAttendanceDefinition().getMultiplier() : 1.0)).reduce(BigDecimal.ZERO, BigDecimal::add);
                    if (sumMult.compareTo(BigDecimal.ZERO) <= 0) continue;

                    List<RecordCalculation> altCalcs = allRecords.stream().filter(r -> r.getProductionDate().equals(currentDate) && r.getTeam().getId().equals(altTeamId)).map(r -> {
                        Optional<ProductStepRate> ro = productStepRateRepository.findEffectiveRate(r.getProduct().getId(), r.getTeam().getProductionStep().getId(), r.getQuality().getId(), currentDate);
                        return ro.map(rate -> new RecordCalculation(r.getQuantity(), rate.getPriceHigh(), rate.getPriceLow(), calculateSurcharge(r.getProduct(), r.getQuality(), filmSurcharge1, filmSurcharge2))).orElse(null);
                    }).filter(Objects::nonNull).collect(Collectors.toList());

                    boolean isDiligent = attendanceCountMap.getOrDefault(att.getEmployee().getId(), BigDecimal.ZERO).compareTo(BigDecimal.valueOf(minAttendanceDays)) >= 0;
                    BigDecimal earned = BigDecimal.ZERO;
                    BigDecimal m = BigDecimal.valueOf(att.getAttendanceDefinition() != null ? att.getAttendanceDefinition().getMultiplier() : 1.0);
                    for (RecordCalculation c : altCalcs) {
                        earned = earned.add((isDiligent ? c.priceHigh : c.priceLow).add(c.surcharge).multiply(BigDecimal.valueOf(c.quantity)).multiply(m).divide(sumMult, 2, RoundingMode.HALF_UP));
                    }
                    lendLaborCost = lendLaborCost.add(earned);
                    details.add(TeamWageResponse.WorkerWageDetail.builder()
                            .employeeId(att.getEmployee().getId()).employeeName(att.getEmployee().getFullName()).employeeCode(att.getEmployee().getCode())
                            .originalTeamName(team.getName())
                            .actualTeamName(att.getActualTeam().getName())
                            .amount(earned)
                            .isBorrowed(false)
                            .status("LEND")
                            .build());
                }

                borrowedLaborCost = borrowedLaborCost.negate();

                BigDecimal teamProductionValue = calcs.stream().map(c -> c.priceHigh.add(c.surcharge).multiply(BigDecimal.valueOf(c.quantity))).reduce(BigDecimal.ZERO, BigDecimal::add);

                if (teamProductionValue.compareTo(BigDecimal.ZERO) > 0 || internalLaborCost.compareTo(BigDecimal.ZERO) > 0 || borrowedLaborCost.compareTo(BigDecimal.ZERO) > 0 || lendLaborCost.compareTo(BigDecimal.ZERO) > 0) {
                    responseList.add(TeamWageResponse.builder()
                            .teamId(teamId).teamName(team.getName()).date(currentDate)
                            .totalTeamIncome(teamProductionValue).internalLaborCost(internalLaborCost)
                            .borrowedLaborCost(borrowedLaborCost).lendLaborCost(lendLaborCost).details(details).build());
                }
            }
        }
        return responseList;
    }
}
