package com.plywood.payroll.modules.attendance.entity;
import com.plywood.payroll.modules.organization.entity.Team;
import com.plywood.payroll.modules.employee.entity.Employee;
import com.plywood.payroll.shared.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
@Entity
@Table(name = "daily_attendance", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"attendance_date", "employee_id"})
})
public class DailyAttendance extends BaseEntity {
    @Column(nullable = false)
    private LocalDate attendanceDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "original_team_id")
    private Team originalTeam; // Tổ biên chế

    @ManyToOne
    @JoinColumn(name = "actual_team_id")
    private Team actualTeam; // Tổ thực tế làm việc hôm đó (khác originalTeam nếu đi mượn)

    @ManyToOne
    @JoinColumn(name = "attendance_definition_id")
    private AttendanceDefinition attendanceDefinition;
}
