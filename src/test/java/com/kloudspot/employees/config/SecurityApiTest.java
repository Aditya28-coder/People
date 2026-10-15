package com.kloudspot.employees.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class SecurityApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void publicShellAndCsrfAreAccessibleButEmployeeDataIsPrivate() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        for (String path : new String[]{"/api/employees", "/api/departments", "/api/auth/me", "/v3/api-docs"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }
    @Test void httpBasicAcceptsCorrectCredentialsAndRejectsWrongOnes() throws Exception {
        mvc.perform(get("/api/auth/me").with(httpBasic("admin","TestAdmin123!")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("admin"));
        mvc.perform(get("/api/employees").with(httpBasic("admin","wrong")))
            .andExpect(status().isUnauthorized()).andExpect(header().exists("WWW-Authenticate"));
    }
    @Test void browserLoginRotatesSessionAndLogoutInvalidatesIt() throws Exception {
        var tokenResponse = mvc.perform(get("/api/auth/csrf")).andReturn();
        var session = (MockHttpSession) tokenResponse.getRequest().getSession();
        String token = mapper.readTree(tokenResponse.getResponse().getContentAsString()).get("token").asText();
        String oldSessionId = session.getId();
        mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-TOKEN",token)
            .param("username","admin").param("password","TestAdmin123!")).andExpect(status().isNoContent());
        org.assertj.core.api.Assertions.assertThat(session.getId()).isNotEqualTo(oldSessionId);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        var newTokenResponse = mvc.perform(get("/api/auth/csrf").session(session)).andReturn();
        String newToken = mapper.readTree(newTokenResponse.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-TOKEN",newToken)).andExpect(status().isNoContent());
        org.assertj.core.api.Assertions.assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
    @Test void loginAndBasicWritesRequireValidCsrf() throws Exception {
        mvc.perform(post("/api/auth/login").param("username","admin").param("password","TestAdmin123!"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/employees/1").with(httpBasic("admin","TestAdmin123!"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").with(csrf()).param("username","admin").param("password","wrong"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Invalid username or password."));
    }
    @Test void nonAdministratorCannotReadOrChangeRecords() throws Exception {
        mvc.perform(get("/api/employees").with(user("reader").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/employees/1").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
    }
}
