package com.kloudspot.employees.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    UserDetailsService users(@Value("${app.admin.username:admin}") String username,
                             @Value("${app.admin.password:}") String password) {
        if (username.isBlank() || password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Set APP_ADMIN_PASSWORD to 12–72 UTF-8 bytes and a nonblank APP_ADMIN_USERNAME.");
        }
        return new InMemoryUserDetailsManager(User.withUsername(username)
            .password("{bcrypt}" + new BCryptPasswordEncoder().encode(password)).roles("ADMIN").build());
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, ObjectMapper mapper) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/styles.css", "/app.js", "/directory-state.js", "/favicon.svg", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .anyRequest().hasRole("ADMIN"))
            .csrf(Customizer.withDefaults())
            .requestCache(cache -> cache.disable())
            .formLogin(login -> login.loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, auth) -> res.setStatus(204))
                .failureHandler((req, res, error) -> problem(res, mapper, 401, "Invalid username or password."))
                .permitAll())
            .httpBasic(basic -> basic.authenticationEntryPoint((req, res, error) -> {
                res.setHeader("WWW-Authenticate", "Basic realm=\"People API\"");
                problem(res, mapper, 401, "Sign in to continue.");
            }))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req, res, error) -> problem(res, mapper, 401, "Sign in to continue."))
                .accessDeniedHandler((req, res, error) -> problem(res, mapper, 403, "Access denied or security token expired. Refresh and try again.")))
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204))
                .invalidateHttpSession(true).deleteCookies("JSESSIONID"))
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")));
        return http.build();
    }

    private static void problem(HttpServletResponse response, ObjectMapper mapper, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        mapper.writeValue(response.getOutputStream(), Map.of("status", status,
            "title", status == 401 ? "Unauthorized" : "Forbidden", "detail", detail));
    }
}
