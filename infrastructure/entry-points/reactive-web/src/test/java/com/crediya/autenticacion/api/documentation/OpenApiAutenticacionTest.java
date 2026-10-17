package com.crediya.autenticacion.api.documentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springdoc.core.fn.builders.operation.Builder.operationBuilder;

@DisplayName("Documentacion OpenAPI de autenticacion")
class OpenApiAutenticacionTest {

    private static List<String> codigos(Operation operacion) {
        return Arrays.stream(operacion.responses()).map(ApiResponse::responseCode).toList();
    }

    private static List<String> esquemasDeSeguridad(Operation operacion) {
        return Arrays.stream(operacion.security()).map(SecurityRequirement::name).toList();
    }

    @Test
    @DisplayName("registrarUsuario debe documentar el esquema Bearer y cada estado que puede devolver")
    void registrarUsuario_debeDocumentarEstados() {
        Operation operacion = UsuarioOpenApi.registrarUsuario(operationBuilder()).build().getOperation();

        assertThat(operacion.operationId()).isEqualTo("registrarUsuario");
        assertThat(esquemasDeSeguridad(operacion)).containsExactly("bearerAuth");
        assertThat(codigos(operacion)).containsExactlyInAnyOrder("201", "400", "401", "403", "409", "500");
    }

    @Test
    @DisplayName("consultarPorDocumento debe documentar el esquema Bearer, su parametro y cada estado que puede devolver")
    void consultarPorDocumento_debeDocumentarEstados() {
        Operation operacion = UsuarioOpenApi.consultarPorDocumento(operationBuilder()).build().getOperation();

        assertThat(operacion.operationId()).isEqualTo("consultarUsuarioPorDocumento");
        assertThat(esquemasDeSeguridad(operacion)).containsExactly("bearerAuth");
        assertThat(codigos(operacion)).containsExactlyInAnyOrder("200", "401", "403", "404", "500");
        assertThat(operacion.parameters()).extracting(parametro -> parametro.name()).containsExactly("documento");
    }

    @Test
    @DisplayName("consultarPorCorreos debe documentar el esquema Bearer, su parametro y cada estado que puede devolver")
    void consultarPorCorreos_debeDocumentarEstados() {
        Operation operacion = UsuarioOpenApi.consultarPorCorreos(operationBuilder()).build().getOperation();

        assertThat(operacion.operationId()).isEqualTo("consultarUsuariosPorCorreo");
        assertThat(esquemasDeSeguridad(operacion)).containsExactly("bearerAuth");
        assertThat(codigos(operacion)).containsExactlyInAnyOrder("200", "400", "401", "403");
        assertThat(operacion.parameters()).extracting(parametro -> parametro.name()).containsExactly("correos");
    }

    @Test
    @DisplayName("login es publico y debe documentar sus estados")
    void login_debeSerPublicoYDocumentarEstados() {
        Operation operacion = AutenticacionOpenApi.login(operationBuilder()).build().getOperation();

        assertThat(operacion.operationId()).isEqualTo("login");
        assertThat(esquemasDeSeguridad(operacion)).isEmpty();
        assertThat(codigos(operacion)).containsExactlyInAnyOrder("200", "400", "401");
    }

    @Test
    @DisplayName("todo error documentado debe usar el esquema ErrorResponse")
    void errores_debenUsarElEsquemaDeError() {
        Operation operacion = UsuarioOpenApi.registrarUsuario(operationBuilder()).build().getOperation();
        List<ApiResponse> errores = Arrays.stream(operacion.responses())
                .filter(respuesta -> Integer.parseInt(respuesta.responseCode()) >= 400)
                .toList();

        assertThat(errores).isNotEmpty()
                .allSatisfy(respuesta -> assertThat(respuesta.content()[0].schema().implementation().getSimpleName())
                        .isEqualTo("ErrorResponse"));
    }
}
