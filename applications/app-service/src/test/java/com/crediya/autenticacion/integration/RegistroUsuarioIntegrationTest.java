package com.crediya.autenticacion.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU1 de extremo a extremo: aplicacion completa + PostgreSQL real, sin dobles.
 */
@Tag("integration")
@Testcontainers
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("HU1 - Registro de usuarios (aplicacion completa)")
class RegistroUsuarioIntegrationTest {

    private static final String RUTA = "/api/v1/usuarios";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void propiedadesDeBaseDeDatos(DynamicPropertyRegistry registro) {
        registro.add("DB_HOST", POSTGRES::getHost);
        registro.add("DB_PORT", () -> POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
        registro.add("DB_NAME", POSTGRES::getDatabaseName);
        registro.add("DB_USERNAME", POSTGRES::getUsername);
        registro.add("DB_PASSWORD", POSTGRES::getPassword);
    }

    @Autowired
    private WebTestClient client;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void limpiar() {
        databaseClient.sql("TRUNCATE TABLE usuario").then().block();
    }

    private static String cuerpo(String documento, String correo, String salario) {
        return """
                {"nombres":"Maria","apellidos":"Gomez","documento_identidad":"%s","fecha_nacimiento":"1995-03-20",
                 "direccion":"Calle 10","telefono":"+573001234567","correo_electronico":"%s","salario_base":%s}"""
                .formatted(documento, correo, salario);
    }

    private WebTestClient.ResponseSpec registrar(String cuerpo) {
        return client.post().uri(RUTA).contentType(MediaType.APPLICATION_JSON).bodyValue(cuerpo).exchange();
    }

    @Test
    @DisplayName("cuando los datos son validos, debe responder 201 y dejar el usuario guardado permanentemente")
    void cuandoDatosValidos_debeResponder201YPersistir() {
        registrar(cuerpo("1020304050", "Maria@Example.com", "4500000"))
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.correo_electronico").isEqualTo("maria@example.com")
                .jsonPath("$.rol").isEqualTo("CLIENTE");

        var total = databaseClient.sql("SELECT count(*) AS total FROM usuario WHERE email = 'maria@example.com'")
                .map(fila -> fila.get("total", Long.class)).one().block();
        assertThat(total).isEqualTo(1L);
    }

    @Test
    @DisplayName("cuando el correo ya esta registrado por otro solicitante, debe responder 409")
    void cuandoCorreoYaRegistrado_debeResponder409() {
        registrar(cuerpo("1020304050", "maria@example.com", "4500000")).expectStatus().isCreated();

        registrar(cuerpo("9999999999", "MARIA@example.com", "3000000"))
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.codigo").isEqualTo("USUARIO_DUPLICADO");
    }

    @Test
    @DisplayName("cuando el documento ya esta registrado, debe responder 409")
    void cuandoDocumentoYaRegistrado_debeResponder409() {
        registrar(cuerpo("1020304050", "uno@example.com", "4500000")).expectStatus().isCreated();

        registrar(cuerpo("1020304050", "dos@example.com", "4500000"))
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.codigo").isEqualTo("USUARIO_DUPLICADO");
    }

    @Test
    @DisplayName("cuando el salario esta fuera de 0 y 15000000, debe responder 400")
    void cuandoSalarioFueraDeRango_debeResponder400() {
        registrar(cuerpo("1020304050", "maria@example.com", "15000001"))
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detalles.salario_base").isNotEmpty();
    }

    @Test
    @DisplayName("cuando el correo no tiene formato valido, debe responder 400")
    void cuandoCorreoInvalido_debeResponder400() {
        registrar(cuerpo("1020304050", "no-es-un-correo", "4500000"))
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detalles.correo_electronico").isNotEmpty();
    }

    @Test
    @DisplayName("la documentacion OpenAPI debe exponer el endpoint de registro")
    void documentacionOpenApi_debeExponerEndpoint() {
        client.get().uri("/v3/api-docs").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.paths['/api/v1/usuarios'].post.operationId").isEqualTo("registrarUsuario");
    }

    @Test
    @DisplayName("el health check debe reportar UP")
    void healthCheck_debeReportarUp() {
        client.get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @DisplayName("HU2: debe poder consultar por documento un usuario registrado y recibir 404 si no existe")
    void consultaPorDocumento_debeEncontrarORechazar() {
        registrar(cuerpo("1020304050", "maria@example.com", "4500000")).expectStatus().isCreated();

        client.get().uri(RUTA + "/1020304050").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.correo_electronico").isEqualTo("maria@example.com")
                .jsonPath("$.salario_base").isEqualTo(4500000);

        client.get().uri(RUTA + "/99999999").exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.codigo").isEqualTo("USUARIO_NO_ENCONTRADO");
    }
}
