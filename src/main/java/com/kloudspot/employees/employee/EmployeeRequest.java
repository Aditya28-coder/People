package com.kloudspot.employees.employee;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeRequest(
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 7, max = 20)
    @Pattern(regexp = "^\\+?[0-9][0-9 ()-]{6,19}$", message = "Enter a valid phone number (7–20 characters)") String phoneNumber,
    @NotBlank @Size(max = 80) String department,
    @NotBlank @Size(max = 100) String designation,
    @NotNull LocalDate dateOfJoining,
    @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal salary,
    @NotNull EmploymentStatus status
) {}
