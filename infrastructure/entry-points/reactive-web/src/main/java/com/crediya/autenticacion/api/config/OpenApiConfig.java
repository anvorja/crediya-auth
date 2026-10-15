package com.crediya.autenticacion.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA_BEARER = "bearerAuth";

    @Bean
    public OpenAPI crediyaAutenticacionOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CrediYa - Microservicio de Autenticacion")
                .description("Registro de usuarios y autenticacion (JWT) de la plataforma CrediYa")
                .version("v1"))
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
