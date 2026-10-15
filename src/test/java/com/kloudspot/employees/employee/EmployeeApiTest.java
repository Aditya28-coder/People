package com.kloudspot.employees.employee;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
class EmployeeApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired EmployeeService service;

    EmployeeRequest request(String name, String email, String department, EmploymentStatus status) {
        return new EmployeeRequest(name, email, "+91 98765 43210", department, "Backend Engineer",
            LocalDate.of(2025, 1, 10), new BigDecimal("1250000.25"), status);
    }

    @Test
    void fullCrudLifecyclePersistsAndDeletesTheRecord() throws Exception {
        var body = mapper.writeValueAsString(request("Ananya Sharma", "ananya@example.com", "Engineering", EmploymentStatus.ACTIVE));
        var created = mvc.perform(post("/api/employees").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.employeeId").isNotEmpty()).andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/employees/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.salary").value(1250000.25));
        var changed = mapper.writeValueAsString(request("Ananya Rao", "ananya@example.com", "Design", EmploymentStatus.INACTIVE));
        mvc.perform(put("/api/employees/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(changed))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Ananya Rao")).andExpect(jsonPath("$.status").value("INACTIVE"));
        mvc.perform(delete("/api/employees/{id}", id).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/employees/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void searchCombinesCaseInsensitiveNameOrEmailWithBothFilters() throws Exception {
        service.create(request("Ananya Sharma", "ananya@example.com", "Engineering", EmploymentStatus.ACTIVE));
        service.create(request("Ananya Rao", "rao@example.com", "Design", EmploymentStatus.ACTIVE));
        service.create(request("Dev Patel", "ananya.dev@example.com", "Engineering", EmploymentStatus.INACTIVE));
        mvc.perform(get("/api/employees").param("search", "ANANYA").param("department", "engineering").param("status", "ACTIVE"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].email").value("ananya@example.com"));
        mvc.perform(get("/api/employees").param("search", "ANANYA.DEV"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].fullName").value("Dev Patel"));
    }

    @Test
    void searchTreatsSqlWildcardsAsLiteralText() throws Exception {
        service.create(request("100% Person", "literal@example.com", "Engineering", EmploymentStatus.ACTIVE));
        service.create(request("Other Person", "other@example.com", "Engineering", EmploymentStatus.ACTIVE));
        mvc.perform(get("/api/employees").param("search", "%"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/employees").param("search", "_"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void paginationSortAndSummaryReportDatabaseValues() throws Exception {
        service.create(request("Zara", "zara@example.com", "Engineering", EmploymentStatus.ACTIVE));
        service.create(request("Aarav", "aarav@example.com", "Design", EmploymentStatus.INACTIVE));
        mvc.perform(get("/api/employees").param("size", "1").param("page", "1").param("sortBy", "fullName"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].fullName").value("Zara"))
            .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.last").value(true));
        mvc.perform(get("/api/employees/summary")).andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.active").value(1))
            .andExpect(jsonPath("$.inactive").value(1)).andExpect(jsonPath("$.departments").value(2));
        mvc.perform(get("/api/employees/departments")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value("Design")).andExpect(jsonPath("$[1]").value("Engineering"));
    }

    @Test
    void emptyDirectoryHasZeroSummaryAndEmptyPage() throws Exception {
        mvc.perform(get("/api/employees/summary")).andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.active").value(0)).andExpect(jsonPath("$.departments").value(0));
        mvc.perform(get("/api/employees")).andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty()).andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void invalidFieldsProduceStructuredFieldErrors() throws Exception {
        String body = mapper.writeValueAsString(Map.of("fullName", " ", "email", "bad-email", "phoneNumber", "abc",
            "department", "", "designation", "", "dateOfJoining", "2025-01-01", "salary", -1, "status", "ACTIVE"));
        mvc.perform(post("/api/employees").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.fullName").exists())
            .andExpect(jsonPath("$.errors.email").exists()).andExpect(jsonPath("$.errors.phoneNumber").exists())
            .andExpect(jsonPath("$.errors.salary").exists()).andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void duplicateEmailIsCaseInsensitiveAndReturnsConflict() throws Exception {
        service.create(request("Ananya", "ananya@example.com", "Engineering", EmploymentStatus.ACTIVE));
        String body = mapper.writeValueAsString(request("Other", "ANANYA@EXAMPLE.COM", "Design", EmploymentStatus.ACTIVE));
        mvc.perform(post("/api/employees").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void updatingToAnotherEmployeesEmailIsRejected() throws Exception {
        service.create(request("Ananya", "ananya@example.com", "Engineering", EmploymentStatus.ACTIVE));
        var other = service.create(request("Other", "other@example.com", "Design", EmploymentStatus.ACTIVE));
        String body = mapper.writeValueAsString(request("Other", "ANANYA@EXAMPLE.COM", "Design", EmploymentStatus.ACTIVE));
        mvc.perform(put("/api/employees/{id}", other.id()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict());
        assertThat(service.get(other.id()).email()).isEqualTo("other@example.com");
    }

    @Test
    void invalidQueryParametersAreRejected() throws Exception {
        for (var param : Map.of("page", "-1", "size", "101", "status", "UNKNOWN", "sortBy", "unknown", "direction", "sideways").entrySet()) {
            mvc.perform(get("/api/employees").param(param.getKey(), param.getValue())).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/employees/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/employees/999999")).andExpect(status().isNotFound());
    }

    @Test
    void malformedJsonAndUnknownFieldsAreRejected() throws Exception {
        mvc.perform(post("/api/employees").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{broken"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/employees").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"unexpected\":true}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void decimalTextFromTheBrowserPreservesCentsAndRejectsExcessPrecision() throws Exception {
        String body = "{\"fullName\":\"Decimal Test\",\"email\":\"decimal@example.com\",\"phoneNumber\":\"9876543210\",\"department\":\"Engineering\",\"designation\":\"Engineer\",\"dateOfJoining\":\"2025-01-10\",\"salary\":\"123456789.25\",\"status\":\"ACTIVE\"}";
        mvc.perform(post("/api/employees").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.salary").value(123456789.25));
        mvc.perform(post("/api/employees").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body.replace("123456789.25", "1.123")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.salary").exists());
    }

    @Test
    void frameworkErrorsKeepTheirHttpStatus() throws Exception {
        mvc.perform(patch("/api/employees/1").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/employees").with(csrf()).contentType(MediaType.TEXT_PLAIN).content("text"))
            .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/api/unknown-resource")).andExpect(status().isNotFound());
    }

    @Test
    void csvRespectsFiltersAndExportsAllMatches() throws Exception {
        service.create(request("Ananya", "ananya@example.com", "Engineering", EmploymentStatus.ACTIVE));
        service.create(request("Other", "other@example.com", "Design", EmploymentStatus.ACTIVE));
        var response = mvc.perform(get("/api/employees/export").param("department", "Engineering"))
            .andExpect(status().isOk()).andExpect(header().string("Content-Disposition", "attachment; filename=employees.csv"))
            .andReturn().getResponse().getContentAsString();
        assertThat(response).contains("ananya@example.com").doesNotContain("other@example.com");
    }

    @Test
    void frontendAndOpenApiAreServedFromTheSameApplication() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/app.js")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.info.title").value("Employee Management API"));
    }

    @Test
    void employeeIdSortUsesNumericOrderInBothDirections() throws Exception {
        var first = service.create(request("Zara", "idsort1@example.com", "Engineering", EmploymentStatus.ACTIVE));
        var second = service.create(request("Aarav", "idsort2@example.com", "Engineering", EmploymentStatus.ACTIVE));
        mvc.perform(get("/api/employees").param("sortBy","id").param("direction","asc"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(first.id()));
        mvc.perform(get("/api/employees").param("sortBy","id").param("direction","desc"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(second.id()));
    }
}
