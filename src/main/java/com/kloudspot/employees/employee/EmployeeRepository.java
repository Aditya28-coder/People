package com.kloudspot.employees.employee;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    long countByDepartmentIgnoreCase(String department);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Employee e set e.department = :newName, e.updatedAt = :updatedAt, e.version = e.version + 1 where lower(e.department) = lower(:oldName)")
    int renameDepartment(String oldName, String newName, java.time.Instant updatedAt);

    @Query("select distinct e.department from Employee e order by e.department")
    List<String> findDepartments();

    @Query("select new com.kloudspot.employees.employee.EmployeeSummary(count(e), " +
        "coalesce(sum(case when e.status = com.kloudspot.employees.employee.EmploymentStatus.ACTIVE then 1L else 0L end), 0L), " +
        "coalesce(sum(case when e.status = com.kloudspot.employees.employee.EmploymentStatus.INACTIVE then 1L else 0L end), 0L), " +
        "count(distinct e.department)) from Employee e")
    EmployeeSummary summarize();
}
