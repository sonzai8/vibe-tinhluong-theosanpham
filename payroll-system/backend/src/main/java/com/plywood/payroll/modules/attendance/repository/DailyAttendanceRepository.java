package com.plywood.payroll.modules.attendance.repository;

import com.plywood.payroll.modules.attendance.entity.DailyAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyAttendanceRepository extends JpaRepository<DailyAttendance, Long> {
    List<DailyAttendance> findByAttendanceDate(LocalDate date);

    @Query("SELECT a FROM DailyAttendance a WHERE " +
            "(:fromDate IS NULL OR a.attendanceDate >= :fromDate) AND " +
            "(:toDate IS NULL OR a.attendanceDate <= :toDate) AND " +
            "(:month IS NULL OR FUNCTION('MONTH', a.attendanceDate) = :month) AND " +
            "(:year IS NULL OR FUNCTION('YEAR', a.attendanceDate) = :year) AND " +
            "(:date IS NULL OR a.attendanceDate = :date) AND " +
            "(:departmentIds IS NULL OR a.employee.department.id IN :departmentIds) AND " +
            "(:teamIds IS NULL OR a.actualTeam.id IN :teamIds)")
    List<DailyAttendance> findByFilters(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("date") LocalDate date,
            @Param("departmentIds") List<Long> departmentIds,
            @Param("teamIds") List<Long> teamIds);

    // Lấy danh sách biên chế tổ gốc có đi làm trong ngày (kể cả người đi mượn tổ khác)
    List<DailyAttendance> findByOriginalTeamIdAndAttendanceDate(Long teamId, LocalDate date);

    // Lấy danh sách người thực tế làm việc tại tổ trong ngày (bao gồm cả người được mượn)
    List<DailyAttendance> findByActualTeamIdAndAttendanceDate(Long teamId, LocalDate date);
}
