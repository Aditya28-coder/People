package com.kloudspot.employees.config;

import com.kloudspot.employees.employee.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class DemoData implements CommandLineRunner {
    private final EmployeeRepository repository;
    private final EmployeeService service;
    public DemoData(EmployeeRepository repository, EmployeeService service) {
        this.repository = repository;
        this.service = service;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (repository.count() > 0) return;
        String[][] rows = {
            {"Aarav Mehta", "Engineering", "Senior Backend Engineer", "2023-06-12", "1800000", "ACTIVE"},
            {"Aditi Rao", "Design", "Product Designer", "2024-02-19", "1200000", "ACTIVE"},
            {"Arjun Kapoor", "Engineering", "Frontend Engineer", "2024-07-08", "1400000", "ACTIVE"},
            {"Diya Nair", "People", "People Operations Lead", "2022-11-14", "1500000", "ACTIVE"},
            {"Ishaan Shah", "Marketing", "Growth Manager", "2023-09-04", "1350000", "ACTIVE"},
            {"Kavya Iyer", "Engineering", "QA Engineer", "2024-04-22", "1050000", "ACTIVE"},
            {"Meera Joshi", "Finance", "Financial Analyst", "2023-01-16", "1150000", "INACTIVE"},
            {"Neel Verma", "Engineering", "Backend Engineer", "2025-01-13", "1300000", "ACTIVE"},
            {"Priya Menon", "Design", "Design Lead", "2022-08-01", "1900000", "ACTIVE"},
            {"Rohan Desai", "Sales", "Account Executive", "2024-03-11", "1100000", "ACTIVE"},
            {"Saanvi Gupta", "Marketing", "Content Strategist", "2024-09-02", "950000", "ACTIVE"},
            {"Vihaan Reddy", "Engineering", "DevOps Engineer", "2023-05-15", "1600000", "ACTIVE"},
            {"Ananya Sharma", "People", "Talent Acquisition Specialist", "2025-02-03", "1000000", "ACTIVE"},
            {"Dev Patel", "Finance", "Finance Manager", "2022-04-18", "1750000", "ACTIVE"},
            {"Ira Mukherjee", "Sales", "Sales Development Representative", "2024-12-09", "850000", "INACTIVE"},
            {"Kabir Sinha", "Engineering", "Platform Engineer", "2023-10-23", "1550000", "ACTIVE"},
            {"Nisha Kumar", "Design", "UX Researcher", "2024-06-17", "1250000", "ACTIVE"},
            {"Om Prakash", "Marketing", "SEO Specialist", "2025-04-07", "900000", "ACTIVE"},
            {"Riya Bose", "Engineering", "Engineering Manager", "2022-03-14", "2400000", "ACTIVE"},
            {"Sahil Jain", "Sales", "Sales Manager", "2023-08-21", "1700000", "INACTIVE"},
            {"Tara Bhat", "People", "HR Business Partner", "2024-01-08", "1400000", "ACTIVE"},
            {"Ved Malhotra", "Finance", "Accounts Associate", "2025-06-02", "750000", "ACTIVE"},
            {"Zara Khan", "Engineering", "Full Stack Engineer", "2024-10-14", "1450000", "ACTIVE"},
            {"Yash Agarwal", "Engineering", "Graduate Engineer", "2025-07-14", "800000", "ACTIVE"}
        };
        int index = 0;
        for (String[] row : rows) {
            service.create(new EmployeeRequest(row[0], row[0].toLowerCase(java.util.Locale.ROOT).replace(' ', '.') + "@example.com",
                "+91 98765 " + (43000 + index++), row[1], row[2], LocalDate.parse(row[3]),
                new BigDecimal(row[4]), EmploymentStatus.valueOf(row[5])));
        }
    }
}
