package com.kloudspot.employees.department;

import jakarta.persistence.*;
import java.util.Locale;

@Entity
@Table(name = "departments")
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 80)
    private String name;
    @Column(nullable = false, unique = true, length = 80)
    private String nameKey;
    @Version private long version;
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name.trim(); this.nameKey = this.name.toLowerCase(Locale.ROOT); }
}
