package com.kloudspot.employees.employee;

import com.kloudspot.employees.common.DuplicateEmailException;
import com.kloudspot.employees.common.EmployeeNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {
    @Mock EmployeeRepository repository;
    @Mock com.kloudspot.employees.department.DepartmentService departmentService;
    @InjectMocks EmployeeService service;
    Employee employee;

    @BeforeEach
    void setUp() {
        lenient().when(departmentService.canonicalName(anyString())).thenAnswer(call -> ((String) call.getArgument(0)).trim());
        employee = new Employee();
        ReflectionTestUtils.setField(employee, "id", 7L);
        employee.setFullName("Ananya Sharma");
        employee.setEmail("ananya@example.com");
        employee.setPhoneNumber("+91 98765 43210");
        employee.setDepartment("Engineering");
        employee.setDesignation("Backend Engineer");
        employee.setDateOfJoining(LocalDate.of(2025, 1, 10));
        employee.setSalary(new BigDecimal("1250000.25"));
        employee.setStatus(EmploymentStatus.ACTIVE);
    }

    private EmployeeRequest request(String email) {
        return new EmployeeRequest("  Ananya Sharma  ", email, "+91 98765 43210", " Engineering ",
            " Backend Engineer ", LocalDate.of(2025, 1, 10), new BigDecimal("1250000.25"), EmploymentStatus.ACTIVE);
    }

    @Test
    void createNormalizesEmailAndTrimsFieldsWithoutLosingSalaryPrecision() {
        when(repository.saveAndFlush(any())).thenAnswer(call -> {
            Employee saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });
        var response = service.create(request("  ANANYA@EXAMPLE.COM  "));
        assertThat(response.email()).isEqualTo("ananya@example.com");
        assertThat(response.fullName()).isEqualTo("Ananya Sharma");
        assertThat(response.department()).isEqualTo("Engineering");
        assertThat(response.designation()).isEqualTo("Backend Engineer");
        assertThat(response.salary()).isEqualByComparingTo("1250000.25");
        assertThat(response.employeeId()).isEqualTo("EMP-0001");
        verify(repository).existsByEmailIgnoreCase("ananya@example.com");
    }

    @Test
    void createRejectsDuplicateEmailBeforeSaving() {
        when(repository.existsByEmailIgnoreCase("ananya@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.create(request("ANANYA@example.com"))).isInstanceOf(DuplicateEmailException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void getReturnsTheGeneratedEmployeeId() {
        when(repository.findById(7L)).thenReturn(Optional.of(employee));
        assertThat(service.get(7L).employeeId()).isEqualTo("EMP-0007");
    }

    @Test
    void getRejectsMissingEmployee() {
        when(repository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(7L)).isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void updateKeepsIdentityAndAllowsTheEmployeesOwnEmail() {
        when(repository.findById(7L)).thenReturn(Optional.of(employee));
        when(repository.saveAndFlush(employee)).thenReturn(employee);
        var response = service.update(7L, request("ANANYA@example.com"));
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.email()).isEqualTo("ananya@example.com");
        verify(repository).existsByEmailIgnoreCaseAndIdNot("ananya@example.com", 7L);
    }

    @Test
    void updateRejectsAnotherEmployeesEmail() {
        when(repository.findById(7L)).thenReturn(Optional.of(employee));
        when(repository.existsByEmailIgnoreCaseAndIdNot("taken@example.com", 7L)).thenReturn(true);
        assertThatThrownBy(() -> service.update(7L, request("taken@example.com"))).isInstanceOf(DuplicateEmailException.class);
        assertThat(employee.getEmail()).isEqualTo("ananya@example.com");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateRejectsMissingEmployee() {
        assertThatThrownBy(() -> service.update(99L, request("a@example.com"))).isInstanceOf(EmployeeNotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deleteRemovesAnExistingEmployee() {
        when(repository.findById(7L)).thenReturn(Optional.of(employee));
        service.delete(7L);
        verify(repository).delete(employee);
    }

    @Test
    void deleteRejectsMissingEmployee() {
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(EmployeeNotFoundException.class);
        verify(repository, never()).delete(any(Employee.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listPreservesPageMetadataAndUsesStableSort() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(employee), PageRequest.of(1, 5), 6));
        var response = service.list("ananya", "Engineering", EmploymentStatus.ACTIVE, 1, 5, "fullName", "desc");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(6);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.last()).isTrue();
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("fullName").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void listRejectsUndocumentedSortFieldsAndDirections() {
        assertThatThrownBy(() -> service.list(null, null, null, 0, 10, "password", "asc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.list(null, null, null, 0, 10, "id", "sideways")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void csvEscapesCommasQuotesNewlinesAndSpreadsheetFormulas() {
        employee.setFullName("=SUM(1,2)");
        employee.setDesignation("Engineer, \"Platform\"\nTeam");
        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(employee));
        String csv = service.exportCsv(null, null, null, "fullName", "asc");
        assertThat(csv).contains("\"'=SUM(1,2)\"");
        assertThat(csv).contains("\"Engineer, \"\"Platform\"\"\nTeam\"");
        assertThat(csv).contains("\"'+91 98765 43210\"");
        assertThat(csv).contains("\"1250000.25\"");
    }
}
