package com.kloudspot.employees.employee;

import com.kloudspot.employees.common.DuplicateEmailException;
import com.kloudspot.employees.common.EmployeeNotFoundException;
import com.kloudspot.employees.department.DepartmentService;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeService {
    private static final Set<String> SORT_FIELDS = Set.of("id", "fullName", "email", "department", "designation", "dateOfJoining", "salary", "status");
    private final EmployeeRepository repository;
    private final DepartmentService departmentService;

    public EmployeeService(EmployeeRepository repository, DepartmentService departmentService) {
        this.repository = repository; this.departmentService = departmentService;
    }

    public PageResponse<EmployeeResponse> list(String search, String department, EmploymentStatus status,
                                               int page, int size, String sortBy, String direction) {
        var pageable = PageRequest.of(page, size, sort(sortBy, direction));
        return PageResponse.from(repository.findAll(EmployeeSpecifications.matching(search, department, status), pageable)
            .map(EmployeeResponse::from));
    }

    public EmployeeResponse get(Long id) { return EmployeeResponse.from(requireEmployee(id)); }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        String email = normalizeEmail(request.email());
        if (repository.existsByEmailIgnoreCase(email)) throw new DuplicateEmailException();
        Employee employee = new Employee();
        apply(employee, request, email);
        return EmployeeResponse.from(repository.saveAndFlush(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = requireEmployee(id);
        String email = normalizeEmail(request.email());
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) throw new DuplicateEmailException();
        apply(employee, request, email);
        return EmployeeResponse.from(repository.saveAndFlush(employee));
    }

    @Transactional
    public void delete(Long id) { repository.delete(requireEmployee(id)); }

    public List<String> departments() { return departmentService.names(); }
    public EmployeeSummary summary() {
        var summary = repository.summarize();
        return new EmployeeSummary(summary.total(), summary.active(), summary.inactive(), departmentService.count());
    }

    public String exportCsv(String search, String department, EmploymentStatus status, String sortBy, String direction) {
        var employees = repository.findAll(EmployeeSpecifications.matching(search, department, status), sort(sortBy, direction));
        StringBuilder csv = new StringBuilder("Employee ID,Full Name,Email,Phone,Department,Designation,Date of Joining,Salary,Status\r\n");
        for (Employee employee : employees) {
            var dto = EmployeeResponse.from(employee);
            csv.append(String.join(",", cell(dto.employeeId()), cell(dto.fullName()), cell(dto.email()),
                cell(dto.phoneNumber()), cell(dto.department()), cell(dto.designation()), cell(dto.dateOfJoining().toString()),
                cell(dto.salary().toPlainString()), cell(dto.status().name()))).append("\r\n");
        }
        return csv.toString();
    }

    private Employee requireEmployee(Long id) {
        return repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private static String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    private void apply(Employee employee, EmployeeRequest request, String email) {
        employee.setFullName(request.fullName().trim());
        employee.setEmail(email);
        employee.setPhoneNumber(request.phoneNumber().trim());
        employee.setDepartment(departmentService.canonicalName(request.department()));
        employee.setDesignation(request.designation().trim());
        employee.setDateOfJoining(request.dateOfJoining());
        employee.setSalary(request.salary());
        employee.setStatus(request.status());
    }

    private static Sort sort(String field, String direction) {
        if (!SORT_FIELDS.contains(field)) throw new IllegalArgumentException("Unsupported sort field: " + field);
        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            throw new IllegalArgumentException("Sort direction must be asc or desc.");
        }
        Sort sort = Sort.by(Sort.Direction.fromString(direction), field);
        return "id".equals(field) ? sort : sort.and(Sort.by("id"));
    }

    // Quoting handles separators/newlines; prefixing neutralizes spreadsheet formula input.
    private static String cell(String value) {
        String trimmed = value.stripLeading();
        if (!trimmed.isEmpty() && "=+-@\t\r\n".indexOf(trimmed.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
