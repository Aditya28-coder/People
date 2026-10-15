package com.kloudspot.employees.department;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kloudspot.employees.employee.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional @WithMockUser(roles = "ADMIN")
class DepartmentApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired DepartmentService departments;
    @Autowired EmployeeService employees;

    @Test void emptyDepartmentCrudAndCounts() throws Exception {
        var response = mvc.perform(post("/api/departments").with(csrf()).contentType("application/json").content("{\"name\":\"  Research  \"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Research"))
            .andExpect(jsonPath("$.employeeCount").value(0)).andReturn();
        long id = mapper.readTree(response.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/employees/summary")).andExpect(jsonPath("$.departments").value(1));
        mvc.perform(put("/api/departments/{id}",id).with(csrf()).contentType("application/json").content("{\"name\":\"Labs\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Labs"));
        mvc.perform(delete("/api/departments/{id}",id).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/departments")).andExpect(jsonPath("$").isEmpty());
    }
    @Test void renameUpdatesEmployeeAndInUseDeleteIsBlocked() throws Exception {
        var employee = employees.create(new EmployeeRequest("Researcher","researcher@example.com","1234567890","Research","Engineer",
            LocalDate.of(2025,1,1),new BigDecimal("100000"),EmploymentStatus.ACTIVE));
        var department = departments.list().get(0);
        mvc.perform(delete("/api/departments/{id}",department.id()).with(csrf())).andExpect(status().isConflict());
        mvc.perform(put("/api/departments/{id}",department.id()).with(csrf()).contentType("application/json").content("{\"name\":\"Innovation\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.employeeCount").value(1));
        assertThat(employees.get(employee.id()).department()).isEqualTo("Innovation");
        employees.delete(employee.id()); departments.delete(department.id());
        assertThat(departments.list()).isEmpty();
    }
    @Test void duplicatesInvalidNamesAndMissingDepartmentsReturnClearErrors() throws Exception {
        departments.create("Design");
        mvc.perform(post("/api/departments").with(csrf()).contentType("application/json").content("{\"name\":\"design\"}"))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/departments").with(csrf()).contentType("application/json").content("{\"name\":\" \"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").exists());
        var other = departments.create("Research");
        mvc.perform(put("/api/departments/{id}",other.id()).with(csrf()).contentType("application/json").content("{\"name\":\"DESIGN\"}"))
            .andExpect(status().isConflict());
        mvc.perform(delete("/api/departments/999999").with(csrf())).andExpect(status().isNotFound());
    }
    @Test void assignmentsReuseCanonicalDepartmentName() {
        departments.create("Engineering");
        var employee = employees.create(new EmployeeRequest("Engineer","engineer@example.com","1234567890","engineering","Engineer",
            LocalDate.of(2025,1,1),new BigDecimal("100000"),EmploymentStatus.ACTIVE));
        assertThat(employee.department()).isEqualTo("Engineering");
        assertThat(departments.list()).hasSize(1);
    }
}
