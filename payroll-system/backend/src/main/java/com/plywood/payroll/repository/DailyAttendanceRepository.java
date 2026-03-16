package com.plywood.payroll.repository;

import com.plywood.payroll.entity.DailyAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyAttendanceRepository extends JpaRepository<DailyAttendance, Long> {
    List<DailyAttendance> findByAttendanceDate(LocalDate date);

    @Query("SELECT a FROM DailyAttendance a WHERE FUNCTION('MONTH', a.attendanceDate) = :month AND FUNCTION('YEAR', a.attendanceDate) = :year")
    List<DailyAttendance> findByMonthAndYear(@Param("month") int month, @Param("year") int year);

    // Lấy danh sách biên chế tổ gốc có đi làm trong ngày (kể cả người đi mượn tổ khác)
    List<DailyAttendance> findByOriginalTeamIdAndAttendanceDate(Long teamId, LocalDate date);

    // Lấy danh sách người thực tế làm việc tại tổ trong ngày (bao gồm cả người được mượn)
    List<DailyAttendance> findByActualTeamIdAndAttendanceDate(Long teamId, LocalDate date);
}
