package com.crediya.autenticacion.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI crediyaAutenticacionOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CrediYa - Microservicio de Autenticacion")
                .description("Registro de usuarios y autenticacion (JWT) de la plataforma CrediYa")
                .version("v1"));
    }
}
