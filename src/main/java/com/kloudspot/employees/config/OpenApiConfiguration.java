package com.kloudspot.employees.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI employeeOpenApi() {
        return new OpenAPI().info(new Info().title("Employee Management API").version("1.0.0")
            .description("Employee and department management. Annual salary in INR. Sign in through the app or use HTTP Basic. For POST/PUT/DELETE, GET /api/auth/csrf first, retain its session cookie, and send the returned token in X-CSRF-TOKEN. Refresh the token after login/logout."))
            .components(new Components().addSecuritySchemes("basicAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic"))
                .addSecuritySchemes("csrf", new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-CSRF-TOKEN")))
            .addSecurityItem(new SecurityRequirement().addList("basicAuth").addList("csrf"));
    }
}
