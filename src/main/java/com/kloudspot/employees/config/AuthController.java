package com.kloudspot.employees.config;

import java.security.Principal;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    @GetMapping("/api/auth/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @GetMapping("/api/auth/me")
    Map<String, String> me(Principal principal) {
        return Map.of("username", principal.getName(), "role", "ADMIN");
    }
}
