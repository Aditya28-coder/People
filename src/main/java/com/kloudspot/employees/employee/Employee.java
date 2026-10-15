package com.kloudspot.employees.employee;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String fullName;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(nullable = false, length = 20)
    private String phoneNumber;
    @Column(nullable = false, length = 80)
    private String department;
    @Column(nullable = false, length = 100)
    private String designation;
    @Column(nullable = false)
    private LocalDate dateOfJoining;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal salary;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmploymentStatus status;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    @PrePersist
    void beforeInsert() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void beforeUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public LocalDate getDateOfJoining() { return dateOfJoining; }
    public void setDateOfJoining(LocalDate dateOfJoining) { this.dateOfJoining = dateOfJoining; }
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    public EmploymentStatus getStatus() { return status; }
    public void setStatus(EmploymentStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
