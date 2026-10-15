package com.kloudspot.employees.department;

import com.kloudspot.employees.employee.EmployeeRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class DepartmentService {
    public record DepartmentResponse(Long id, String name, long employeeCount) {}
    private final DepartmentRepository departments;
    private final EmployeeRepository employees;
    public DepartmentService(DepartmentRepository departments, EmployeeRepository employees) {
        this.departments = departments; this.employees = employees;
    }
    public List<DepartmentResponse> list() {
        return departments.findAllByOrderByNameAsc().stream()
            .map(d -> new DepartmentResponse(d.getId(), d.getName(), employees.countByDepartmentIgnoreCase(d.getName()))).toList();
    }
    public List<String> names() { return departments.findAllByOrderByNameAsc().stream().map(Department::getName).toList(); }
    public long count() { return departments.count(); }

    @Transactional
    public String canonicalName(String name) {
        return departments.findByNameKey(key(name)).orElseGet(() -> {
            Department department = new Department(); department.setName(name);
            return departments.saveAndFlush(department);
        }).getName();
    }

    @Transactional
    public DepartmentResponse create(String name) {
        if (departments.findByNameKey(key(name)).isPresent()) throw conflict("A department with this name already exists.");
        Department department = new Department(); department.setName(name);
        departments.saveAndFlush(department);
        return new DepartmentResponse(department.getId(), department.getName(), 0);
    }

    @Transactional
    public DepartmentResponse rename(Long id, String name) {
        Department department = require(id);
        var duplicate = departments.findByNameKey(key(name));
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) throw conflict("A department with this name already exists.");
        employees.renameDepartment(department.getName(), name.trim(), java.time.Instant.now());
        department.setName(name);
        departments.saveAndFlush(department);
        return new DepartmentResponse(id, department.getName(), employees.countByDepartmentIgnoreCase(name.trim()));
    }

    @Transactional
    public void delete(Long id) {
        Department department = require(id);
        if (employees.countByDepartmentIgnoreCase(department.getName()) > 0) throw conflict("Move all employees to another department before deleting this one.");
        departments.delete(department);
    }
    private Department require(Long id) {
        return departments.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found."));
    }
    private static String key(String name) { return name.trim().toLowerCase(Locale.ROOT); }
    private static ResponseStatusException conflict(String detail) { return new ResponseStatusException(HttpStatus.CONFLICT, detail); }
}
