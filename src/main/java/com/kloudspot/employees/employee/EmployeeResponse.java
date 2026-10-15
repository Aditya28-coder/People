package com.kloudspot.employees.employee;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EmployeeResponse(
    Long id, String employeeId, String fullName, String email, String phoneNumber,
    String department, String designation, LocalDate dateOfJoining, BigDecimal salary,
    EmploymentStatus status, Instant createdAt, Instant updatedAt
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(employee.getId(), "EMP-%04d".formatted(employee.getId()),
            employee.getFullName(), employee.getEmail(), employee.getPhoneNumber(),
            employee.getDepartment(), employee.getDesignation(), employee.getDateOfJoining(),
            employee.getSalary(), employee.getStatus(), employee.getCreatedAt(), employee.getUpdatedAt());
    }
}
