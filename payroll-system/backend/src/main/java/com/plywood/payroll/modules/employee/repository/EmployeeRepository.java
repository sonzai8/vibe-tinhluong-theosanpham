package com.plywood.payroll.modules.employee.repository;

import com.plywood.payroll.modules.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username);
    Optional<Employee> findByZkDeviceId(String zkDeviceId);
    boolean existsByCode(String code);
    boolean existsByCitizenId(String citizenId);
    boolean existsByPhone(String phone);

    Optional<Employee> findByCode(String empCode);
    
    @Query(value = "SELECT e.id, e.code, e.full_name, COALESCE(t.name, 'Không có tổ') as team_name " +
            "FROM employees e " +
            "LEFT JOIN team_process tp ON tp.employee_id = e.id " +
            "AND tp.start_date <= :date AND (tp.end_date IS NULL OR tp.end_date >= :date) " +
            "LEFT JOIN teams t ON t.id = tp.team_id " +
            "WHERE e.status = 'ACTIVE' " +
            "AND (:teamId IS NULL OR t.id = :teamId) " +
            "AND NOT EXISTS (SELECT 1 FROM daily_attendance da WHERE da.employee_id = e.id AND da.attendance_date = :date) " +
            "AND (LOWER(e.full_name) LIKE LOWER(:search) OR LOWER(e.code) LIKE LOWER(:search)) " +
            "LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Object[]> findUnrecordedEmployeesNative(
            @Param("date") LocalDate date,
            @Param("teamId") Long teamId,
            @Param("search") String search,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query("SELECT e FROM Employee e " +
            "LEFT JOIN TeamProcess tp ON tp.employee = e " +
            "AND tp.startDate <= :date AND (tp.endDate IS NULL OR tp.endDate >= :date) " +
            "WHERE e.status = 'ACTIVE' " +
            "AND (:teamId IS NULL OR tp.team.id = :teamId) " +
            "AND NOT EXISTS (SELECT 1 FROM DailyAttendance da WHERE da.employee = e AND da.attendanceDate = :date) " +
            "AND (LOWER(e.fullName) LIKE LOWER(:search) OR LOWER(e.code) LIKE LOWER(:search))")
    List<Employee> findUnrecordedEmployees(
            @Param("date") LocalDate date,
            @Param("teamId") Long teamId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT e FROM Employee e JOIN TeamProcess tp ON tp.employee = e WHERE tp.team.id = :teamId " +
            "AND tp.startDate <= CURRENT_DATE AND (tp.endDate IS NULL OR tp.endDate >= CURRENT_DATE)")
    List<Employee> findAllByTeamId(@Param("teamId") Long teamId);
    
    long countByStatus(String status);
    
    long countByDepartment_IdAndStatus(Long departmentId, String status);

    @Query("SELECT e.code FROM Employee e WHERE e.code LIKE :prefix%")
    List<String> findCodesByPrefix(@Param("prefix") String prefix);

    @Query("SELECT e FROM Employee e WHERE e.status = 'ACTIVE' AND (LOWER(e.fullName) LIKE LOWER(:search) OR LOWER(e.code) LIKE LOWER(:search))")
    List<Employee> search(@Param("search") String search);
}
