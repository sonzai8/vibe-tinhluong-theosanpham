package com.plywood.payroll.modules.employee.repository;

import com.plywood.payroll.modules.employee.entity.TeamProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeamProcessRepository extends JpaRepository<TeamProcess, Long> {

    @Query("SELECT t FROM TeamProcess t WHERE t.employee.id = :employeeId " +
           "AND t.startDate <= :date AND (t.endDate IS NULL OR t.endDate >= :date)")
    Optional<TeamProcess> findEffectiveByDate(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    @Query("SELECT t FROM TeamProcess t WHERE t.employee.id = :employeeId " +
           "AND t.startDate <= :date AND (t.endDate IS NULL OR t.endDate >= :date) " +
           "ORDER BY t.startDate DESC")
    List<TeamProcess> findEffectiveByDateList(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    @Query("SELECT t FROM TeamProcess t WHERE t.employee.id = :employeeId " +
           "AND t.startDate <= :endDate AND (t.endDate IS NULL OR t.endDate >= :startDate)")
    List<TeamProcess> findOverlapping(@Param("employeeId") Long employeeId, 
                                     @Param("startDate") LocalDate startDate, 
                                     @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM TeamProcess t WHERE t.startDate <= :endDate AND (t.endDate IS NULL OR t.endDate >= :startDate)")
    List<TeamProcess> findAllOverlapping(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<TeamProcess> findByEmployeeIdOrderByStartDateDesc(Long employeeId);

    @Query("SELECT t FROM TeamProcess t WHERE t.team.id = :teamId " +
           "AND t.startDate <= :date AND (t.endDate IS NULL OR t.endDate >= :date)")
    List<TeamProcess> findEffectiveByTeamId(@Param("teamId") Long teamId, @Param("date") LocalDate date);
}
