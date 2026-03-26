package com.plywood.payroll.modules.attendance.controller;

import com.plywood.payroll.modules.attendance.dto.request.ZkTecoAttendanceRequest;
import com.plywood.payroll.modules.attendance.entity.DailyAttendance;
import com.plywood.payroll.modules.attendance.entity.AttendanceDefinition;
import com.plywood.payroll.modules.attendance.repository.DailyAttendanceRepository;
import com.plywood.payroll.modules.attendance.repository.AttendanceDefinitionRepository;
import com.plywood.payroll.modules.employee.entity.Employee;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.employee.repository.TeamProcessRepository;
import com.plywood.payroll.modules.employee.entity.TeamProcess;
import com.plywood.payroll.modules.system.service.SystemLogService;
import com.plywood.payroll.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/attendance/zkteco")
@RequiredArgsConstructor
@Tag(name = "ZKTeco Attendance", description = "Nhận dữ liệu từ máy chấm công ZKTeco")
@Slf4j
public class ZkTecoAttendanceController {

    private final EmployeeRepository employeeRepository;
    private final DailyAttendanceRepository attendanceRepository;
    private final AttendanceDefinitionRepository definitionRepository;
    private final TeamProcessRepository teamProcessRepository;
    private final SystemLogService systemLogService;

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Nhận dữ liệu chấm công từ ZKTeco")
    @Transactional
    public ResponseEntity<ApiResponse<String>> receiveAttendance(@RequestBody List<ZkTecoAttendanceRequest> requests) {
        int successCount = 0;
        int errorCount = 0;

        AttendanceDefinition defaultDef = definitionRepository.findByCode("X")
                .orElseGet(() -> definitionRepository.findByCode("NG")
                        .orElse(null));

        for (ZkTecoAttendanceRequest req : requests) {
            String zkId = req.getZkDeviceId();
            LocalDate date = req.getCheckTime().toLocalDate();

            Optional<Employee> empOpt = employeeRepository.findByZkDeviceId(zkId);
            if (empOpt.isPresent()) {
                Employee emp = empOpt.get();
                
                // Kiểm tra xem đã có bản ghi chấm công chưa
                DailyAttendance attendance = attendanceRepository.findByAttendanceDateAndEmployeeId(date, emp.getId())
                        .orElse(new DailyAttendance());
                
                if (attendance.getId() == null) {
                    attendance.setAttendanceDate(date);
                    attendance.setEmployee(emp);
                    
                    // Lấy tổ hiện tại của nhân viên
                    Optional<TeamProcess> tpOpt = teamProcessRepository.findEffectiveByDate(emp.getId(), date);
                    if (tpOpt.isPresent()) {
                        attendance.setOriginalTeam(tpOpt.get().getTeam());
                        attendance.setActualTeam(tpOpt.get().getTeam());
                    }
                    
                    if (attendance.getAttendanceDefinition() == null) {
                        attendance.setAttendanceDefinition(defaultDef);
                    }
                    
                    attendanceRepository.save(attendance);
                    successCount++;
                } else {
                    // Nếu đã có rồi thì không làm gì hoặc có thể update logic In/Out phức tạp hơn sau này
                    successCount++;
                }
            } else {
                errorCount++;
                systemLogService.log("ZKTECO_MAPPING_ERROR", 
                        "Không tìm thấy nhân viên với ZK Device ID: " + zkId, 
                        "Dữ liệu gốc: " + req.toString());
            }
        }

        String message = String.format("Nhận dữ liệu thành công: %d record, lỗi mapping: %d record", successCount, errorCount);
        return ResponseEntity.ok(ApiResponse.success(message, message));
    }
}
