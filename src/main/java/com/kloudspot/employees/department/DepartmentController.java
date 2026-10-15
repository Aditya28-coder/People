package com.kloudspot.employees.department;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/departments")
@Validated
public class DepartmentController {
    public record DepartmentRequest(@NotBlank @Size(max = 80) String name) {}
    private final DepartmentService service;
    public DepartmentController(DepartmentService service) { this.service = service; }
    @GetMapping List<DepartmentService.DepartmentResponse> list() { return service.list(); }
    @PostMapping ResponseEntity<DepartmentService.DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        var result = service.create(request.name());
        return ResponseEntity.created(URI.create("/api/departments/" + result.id())).body(result);
    }
    @PutMapping("/{id}") DepartmentService.DepartmentResponse rename(@PathVariable @Positive Long id, @Valid @RequestBody DepartmentRequest request) {
        return service.rename(id, request.name());
    }
    @DeleteMapping("/{id}") ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id); return ResponseEntity.noContent().build();
    }
}
