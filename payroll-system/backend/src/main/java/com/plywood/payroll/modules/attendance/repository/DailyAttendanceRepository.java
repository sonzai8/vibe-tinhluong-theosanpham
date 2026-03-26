package com.plywood.payroll.modules.attendance.repository;

import com.plywood.payroll.modules.attendance.entity.DailyAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DailyAttendanceRepository extends JpaRepository<DailyAttendance, Long>, JpaSpecificationExecutor<DailyAttendance> {
    List<DailyAttendance> findByAttendanceDate(LocalDate date);

    @Query("SELECT a FROM DailyAttendance a WHERE " +
            "(:fromDate IS NULL OR a.attendanceDate >= :fromDate) AND " +
            "(:toDate IS NULL OR a.attendanceDate <= :toDate) AND " +
            "(:date IS NULL OR a.attendanceDate = :date) AND " +
            "(:date IS NULL OR a.attendanceDate = :date) AND " +
            "(:deptIdsEmpty = true OR a.employee.department.id IN :departmentIds) AND " +
            "(:teamIdsEmpty = true OR a.actualTeam.id IN :teamIds)")
    List<DailyAttendance> findByFilters(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("date") LocalDate date,
            @Param("departmentIds") List<Long> departmentIds,
            @Param("deptIdsEmpty") boolean deptIdsEmpty,
            @Param("teamIds") List<Long> teamIds,
            @Param("teamIdsEmpty") boolean teamIdsEmpty);

    // Lấy danh sách biên chế tổ gốc có đi làm trong ngày (kể cả người đi mượn tổ khác)
    List<DailyAttendance> findByOriginalTeamIdAndAttendanceDate(Long teamId, LocalDate date);

    // Lấy danh sách người thực tế làm việc tại tổ trong ngày (bao gồm cả người được mượn)
    List<DailyAttendance> findByActualTeamIdAndAttendanceDate(Long teamId, LocalDate date);

    List<DailyAttendance> findByEmployeeIdAndAttendanceDateBetween(Long employeeId, LocalDate start, LocalDate end);
    Optional<DailyAttendance> findByAttendanceDateAndEmployeeId(LocalDate date, Long employeeId);
}
