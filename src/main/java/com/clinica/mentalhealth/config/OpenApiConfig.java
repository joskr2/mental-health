package com.clinica.mentalhealth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Mental Health Clinic API",
        version = "1.0",
        description = "API reactiva para gestión de citas en clínica de salud mental. Incluye autenticación JWT con refresh tokens, validación de horarios, y gestión de conflictos.",
        contact = @Contact(
            name = "Mental Health Team",
            email = "support@mentalhealth.com"
        )
    )
)
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
public class OpenApiConfig {
}

