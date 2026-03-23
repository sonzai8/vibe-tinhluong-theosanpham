package com.plywood.payroll.modules.organization.repository;

import com.plywood.payroll.modules.organization.entity.TeamDailyFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeamDailyFundRepository extends JpaRepository<TeamDailyFund, Long> {
    Optional<TeamDailyFund> findByTeamIdAndFundDate(Long teamId, LocalDate date);

    @Query("SELECT f FROM TeamDailyFund f WHERE f.team.id = :teamId AND EXTRACT(MONTH FROM f.fundDate) = :month AND EXTRACT(YEAR FROM f.fundDate) = :year")
    List<TeamDailyFund> findByTeamAndMonth(@Param("teamId") Long teamId, @Param("month") int month, @Param("year") int year);
}
