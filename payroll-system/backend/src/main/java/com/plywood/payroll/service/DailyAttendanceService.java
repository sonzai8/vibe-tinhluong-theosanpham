package com.plywood.payroll.service;

import com.plywood.payroll.dto.request.DailyAttendanceRequest;
import com.plywood.payroll.dto.response.DailyAttendanceResponse;
import com.plywood.payroll.entity.DailyAttendance;
import com.plywood.payroll.entity.Employee;
import com.plywood.payroll.entity.Team;
import com.plywood.payroll.exception.ResourceNotFoundException;
import com.plywood.payroll.repository.DailyAttendanceRepository;
import com.plywood.payroll.repository.EmployeeRepository;
import com.plywood.payroll.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyAttendanceService {

    private final DailyAttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;
    private final EmployeeService employeeService;
    private final TeamService teamService;

    public List<DailyAttendanceResponse> getByMonthAndYear(int month, int year) {
        return attendanceRepository.findByMonthAndYear(month, year).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<DailyAttendanceResponse> getByDate(LocalDate date) {
        return attendanceRepository.findByAttendanceDate(date).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DailyAttendanceResponse getById(Long id) {
        return attendanceRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Điểm danh", id));
    }

    @Transactional
    public DailyAttendanceResponse create(DailyAttendanceRequest request) {
        return mapToResponse(attendanceRepository.save(mapRequestToEntity(request, new DailyAttendance())));
    }

    @Transactional
    public DailyAttendanceResponse update(Long id, DailyAttendanceRequest request) {
        DailyAttendance att = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Điểm danh", id));
        return mapToResponse(attendanceRepository.save(mapRequestToEntity(request, att)));
    }
    
    @Transactional
    public List<DailyAttendanceResponse> saveBatch(List<DailyAttendanceRequest> requests) {
        List<DailyAttendance> attendances = requests.stream()
                .map(req -> mapRequestToEntity(req, new DailyAttendance()))
                .collect(Collectors.toList());
                
        return attendanceRepository.saveAll(attendances).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        if (!attendanceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Điểm danh", id);
        }
        attendanceRepository.deleteById(id);
    }
    
    private DailyAttendance mapRequestToEntity(DailyAttendanceRequest request, DailyAttendance att) {
        Employee emp = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", request.getEmployeeId()));
                
        Team origTeam = null;
        if (request.getOriginalTeamId() != null) {
            origTeam = teamRepository.findById(request.getOriginalTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tổ biên chế", request.getOriginalTeamId()));
        }
        
        Team actualTeam = null;
        if (request.getActualTeamId() != null) {
            actualTeam = teamRepository.findById(request.getActualTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tổ thực tế", request.getActualTeamId()));
        }
        
        att.setAttendanceDate(request.getAttendanceDate());
        att.setEmployee(emp);
        att.setOriginalTeam(origTeam);
        att.setActualTeam(actualTeam);
        
        return att;
    }

    public DailyAttendanceResponse mapToResponse(DailyAttendance entity) {
        if (entity == null) return null;
        DailyAttendanceResponse response = new DailyAttendanceResponse();
        response.setId(entity.getId());
        response.setAttendanceDate(entity.getAttendanceDate());
        response.setEmployee(employeeService.mapToResponse(entity.getEmployee()));
        response.setOriginalTeam(teamService.mapToResponse(entity.getOriginalTeam()));
        response.setActualTeam(teamService.mapToResponse(entity.getActualTeam()));
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
