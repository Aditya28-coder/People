package com.kloudspot.employees.employee;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@Validated
@Tag(name = "Employees", description = "Employee CRUD, search, filtering, pagination, and CSV export")
public class EmployeeController {
    private final EmployeeService service;
    public EmployeeController(EmployeeService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "List employees; page numbers start at zero")
    public PageResponse<EmployeeResponse> list(
        @RequestParam(required = false) @Size(max = 100) String search,
        @RequestParam(required = false) @Size(max = 80) String department,
        @RequestParam(required = false) EmploymentStatus status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "fullName") String sortBy,
        @RequestParam(defaultValue = "asc") String direction) {
        return service.list(search, department, status, page, size, sortBy, direction);
    }

    @GetMapping("/summary")
    @Operation(summary = "Global employee counts, independent of current list filters")
    public EmployeeSummary summary() { return service.summary(); }

    @GetMapping("/departments")
    @Operation(summary = "Distinct departments currently used by employees")
    public List<String> departments() { return service.departments(); }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export all matching employees to CSV, without pagination")
    public ResponseEntity<byte[]> export(
        @RequestParam(required = false) @Size(max = 100) String search,
        @RequestParam(required = false) @Size(max = 80) String department,
        @RequestParam(required = false) EmploymentStatus status,
        @RequestParam(defaultValue = "fullName") String sortBy,
        @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employees.csv")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(service.exportCsv(search, department, status, sortBy, direction).getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single employee")
    public EmployeeResponse get(@PathVariable @Min(1) Long id) { return service.get(id); }

    @PostMapping
    @Operation(summary = "Create an employee; Employee ID is generated automatically")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        var employee = service.create(request);
        return ResponseEntity.created(URI.create("/api/employees/" + employee.id())).body(employee);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace the editable fields of an employee")
    public EmployeeResponse update(@PathVariable @Min(1) Long id, @Valid @RequestBody EmployeeRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an employee permanently")
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
