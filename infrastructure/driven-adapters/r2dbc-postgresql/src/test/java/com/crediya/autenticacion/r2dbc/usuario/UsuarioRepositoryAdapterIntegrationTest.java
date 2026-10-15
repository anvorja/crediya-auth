package com.crediya.autenticacion.r2dbc.usuario;

import com.crediya.autenticacion.model.usuario.Rol;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioDuplicadoException;
import com.crediya.autenticacion.r2dbc.transaccion.TransaccionAdapter;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@DisplayName("UsuarioRepositoryAdapter + TransaccionAdapter contra PostgreSQL real")
class UsuarioRepositoryAdapterIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static ConnectionPool pool;
    private static R2dbcEntityTemplate template;
    private static UsuarioRepositoryAdapter adapter;
    private static TransaccionAdapter transaccion;

    @BeforeAll
    static void iniciarInfraestructura() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();

        var configuracion = PostgresqlConnectionConfiguration.builder()
                .host(POSTGRES.getHost())
                .port(POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT))
                .database(POSTGRES.getDatabaseName())
                .username(POSTGRES.getUsername())
                .password(POSTGRES.getPassword())
                .build();
        pool = new ConnectionPool(ConnectionPoolConfiguration.builder(new PostgresqlConnectionFactory(configuracion)).build());
        template = new R2dbcEntityTemplate(pool);
        adapter = new UsuarioRepositoryAdapter(template);
        transaccion = new TransaccionAdapter(TransactionalOperator.create(new R2dbcTransactionManager(pool)));
    }

    @AfterAll
    static void cerrarPool() {
        pool.dispose();
    }

    @BeforeEach
    void limpiarTabla() {
        template.getDatabaseClient().sql("TRUNCATE TABLE usuario").then().block();
    }

    private static Usuario usuario(String email, String documento, Rol rol) {
        return new Usuario(null, "Maria", "Gomez", documento, LocalDate.of(1995, 3, 20), "Calle 10",
                "+573001234567", email, new BigDecimal("4500000.00"), rol);
    }

    @Test
    @DisplayName("cuando se guarda un usuario sin id, debe asignar un id y persistir todos los campos")
    void cuandoGuardaSinId_debeAsignarIdYPersistir() {
        var original = usuario("maria@example.com", "1020304050", Rol.CLIENTE);

        StepVerifier.create(adapter.guardar(original))
                .assertNext(guardado -> {
                    assertThat(guardado.id()).isNotNull();
                    assertThat(guardado).usingRecursiveComparison().ignoringFields("id").isEqualTo(original);
                })
                .verifyComplete();

        StepVerifier.create(adapter.existePorEmail("maria@example.com")).expectNext(true).verifyComplete();
    }

    @Test
    @DisplayName("cuando se guarda un usuario con id, debe respetarlo")
    void cuandoGuardaConId_debeRespetarlo() {
        var id = UUID.randomUUID();

        StepVerifier.create(adapter.guardar(usuario("a@example.com", "11111", Rol.CLIENTE).conId(id)))
                .assertNext(guardado -> assertThat(guardado.id()).isEqualTo(id))
                .verifyComplete();
    }

    @ParameterizedTest(name = "rol {0}")
    @EnumSource(Rol.class)
    @DisplayName("cada rol debe persistirse y leerse con el mismo valor")
    void cadaRol_debeHacerRoundTrip(Rol rol) {
        StepVerifier.create(adapter.guardar(usuario("rol@example.com", "22222", rol)))
                .assertNext(guardado -> assertThat(guardado.rol()).isEqualTo(rol))
                .verifyComplete();
    }

    @Test
    @DisplayName("existePorEmail debe ignorar mayusculas y devolver false si no existe")
    void existePorEmail_debeIgnorarMayusculas() {
        adapter.guardar(usuario("maria@example.com", "33333", Rol.CLIENTE)).block();

        StepVerifier.create(adapter.existePorEmail("MARIA@EXAMPLE.COM")).expectNext(true).verifyComplete();
        StepVerifier.create(adapter.existePorEmail("otra@example.com")).expectNext(false).verifyComplete();
    }

    @Test
    @DisplayName("existePorDocumentoIdentidad debe distinguir documentos existentes e inexistentes")
    void existePorDocumento_debeDistinguir() {
        adapter.guardar(usuario("maria@example.com", "44444", Rol.CLIENTE)).block();

        StepVerifier.create(adapter.existePorDocumentoIdentidad("44444")).expectNext(true).verifyComplete();
        StepVerifier.create(adapter.existePorDocumentoIdentidad("99999")).expectNext(false).verifyComplete();
    }

    @Test
    @DisplayName("cuando el correo ya existe, debe traducir el error de unicidad a UsuarioDuplicadoException")
    void cuandoEmailDuplicado_debeTraducirError() {
        adapter.guardar(usuario("maria@example.com", "55555", Rol.CLIENTE)).block();

        StepVerifier.create(adapter.guardar(usuario("maria@example.com", "66666", Rol.CLIENTE)))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(UsuarioDuplicadoException.class);
                    assertThat(((UsuarioDuplicadoException) error).getCampo()).isEqualTo("correo electronico");
                })
                .verify();
    }

    @Test
    @DisplayName("cuando el documento ya existe, debe traducir el error de unicidad a UsuarioDuplicadoException")
    void cuandoDocumentoDuplicado_debeTraducirError() {
        adapter.guardar(usuario("uno@example.com", "77777", Rol.CLIENTE)).block();

        StepVerifier.create(adapter.guardar(usuario("dos@example.com", "77777", Rol.CLIENTE)))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(UsuarioDuplicadoException.class);
                    assertThat(((UsuarioDuplicadoException) error).getCampo()).isEqualTo("documento de identidad");
                })
                .verify();
    }

    @Test
    @DisplayName("cuando otra restriccion de integridad falla, debe propagar el error original")
    void cuandoOtraRestriccionFalla_debePropagarErrorOriginal() {
        var salarioFueraDeRango = new Usuario(null, "Maria", "Gomez", "88888", null, null, null,
                "maria@example.com", new BigDecimal("20000000"), Rol.CLIENTE);

        StepVerifier.create(adapter.guardar(salarioFueraDeRango))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    @DisplayName("la transaccion debe confirmar los cambios cuando la operacion termina bien")
    void transaccion_debeConfirmarCuandoTerminaBien() {
        StepVerifier.create(transaccion.ejecutar(adapter.guardar(usuario("ok@example.com", "12121", Rol.CLIENTE))))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(adapter.existePorEmail("ok@example.com")).expectNext(true).verifyComplete();
    }

    @Test
    @DisplayName("la transaccion debe revertir los cambios cuando la operacion falla")
    void transaccion_debeRevertirCuandoFalla() {
        var operacion = adapter.guardar(usuario("rollback@example.com", "13131", Rol.CLIENTE))
                .then(Mono.<Usuario>error(new IllegalStateException("fallo despues de guardar")));

        StepVerifier.create(transaccion.ejecutar(operacion))
                .expectErrorMessage("fallo despues de guardar")
                .verify();

        StepVerifier.create(adapter.existePorEmail("rollback@example.com")).expectNext(false).verifyComplete();
    }
}
