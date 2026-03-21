package com.plywood.payroll.modules.attendance.service;

import com.plywood.payroll.modules.attendance.dto.request.DailyAttendanceRequest;
import com.plywood.payroll.modules.attendance.entity.AttendanceDefinition;
import com.plywood.payroll.modules.attendance.entity.DailyAttendance;
import com.plywood.payroll.modules.attendance.repository.AttendanceDefinitionRepository;
import com.plywood.payroll.modules.attendance.repository.DailyAttendanceRepository;
import com.plywood.payroll.modules.employee.entity.Employee;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.organization.entity.Team;
import com.plywood.payroll.modules.organization.repository.TeamRepository;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.employee.service.EmployeeService;
import com.plywood.payroll.modules.organization.service.TeamService;
import com.plywood.payroll.modules.attendance.service.AttendanceDefinitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyAttendanceServiceTest {

    @Mock private DailyAttendanceRepository attendanceRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private AttendanceDefinitionRepository definitionRepository;
    @Mock private EmployeeService employeeService;
    @Mock private TeamService teamService;
    @Mock private AttendanceDefinitionService definitionService;

    @InjectMocks
    private DailyAttendanceService attendanceService;

    private Employee employee;
    private Team team;
    private AttendanceDefinition definition;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);

        team = new Team();
        team.setId(1L);

        definition = new AttendanceDefinition();
        definition.setId(1L);
        definition.setCode("NG");
    }

    @Test
    void testCreate_Success() {
        DailyAttendanceRequest request = new DailyAttendanceRequest();
        request.setEmployeeId(1L);
        request.setAttendanceDate(LocalDate.now());
        request.setActualTeamId(1L);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(definitionRepository.findByCode("NG")).thenReturn(Optional.of(definition));
        when(attendanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        attendanceService.create(request);

        verify(attendanceRepository, times(1)).save(any());
    }

    @Test
    void testCreate_EmployeeNotFound() {
        DailyAttendanceRequest request = new DailyAttendanceRequest();
        request.setEmployeeId(99L);

        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attendanceService.create(request));
    }

    @Test
    void testDelete_Success() {
        when(attendanceRepository.existsById(1L)).thenReturn(true);
        attendanceService.delete(1L);
        verify(attendanceRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDelete_NotFound() {
        when(attendanceRepository.existsById(1L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> attendanceService.delete(1L));
    }
}
