package com.crediya.autenticacion.api.usuario;

import com.crediya.autenticacion.api.ApiTestSupport;
import com.crediya.autenticacion.api.autenticacion.AutenticacionHandler;
import com.crediya.autenticacion.api.mapper.AutenticacionRestMapperImpl;
import com.crediya.autenticacion.api.mapper.UsuarioRestMapperImpl;
import com.crediya.autenticacion.api.validation.RequestValidator;
import com.crediya.autenticacion.model.common.exceptions.AccesoDenegadoException;
import com.crediya.autenticacion.model.usuario.Rol;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.commands.RegistrarUsuarioCommand;
import com.crediya.autenticacion.model.usuario.exceptions.DatosUsuarioInvalidosException;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioDuplicadoException;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioNoEncontradoException;
import com.crediya.autenticacion.usecase.clavespublicas.ObtenerClavesPublicasUseCase;
import com.crediya.autenticacion.usecase.consultarusuario.ConsultarUsuarioUseCase;
import com.crediya.autenticacion.usecase.consultarusuarios.ConsultarUsuariosPorCorreoUseCase;
import com.crediya.autenticacion.usecase.login.LoginUseCase;
import com.crediya.autenticacion.usecase.registrarusuario.RegistrarUsuarioUseCase;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("POST /api/v1/usuarios (HU1) - router, handler y manejo de errores")
class UsuarioRouterTest {

    private static final String RUTA = "/api/v1/usuarios";
    private static final String CUERPO_VALIDO = """
            {"nombres":"Maria","apellidos":"Gomez","documento_identidad":"1020304050",
             "fecha_nacimiento":"1995-03-20","direccion":"Calle 10","telefono":"+573001234567",
             "correo_electronico":"Maria@Example.com","salario_base":4500000,"rol":"ASESOR",
             "password":"Clave-Segura-123"}""";

    @Mock
    private RegistrarUsuarioUseCase useCase;

    @Mock
    private ConsultarUsuarioUseCase consultarUseCase;

    @Mock
    private ConsultarUsuariosPorCorreoUseCase consultarPorCorreosUseCase;

    @Mock
    private LoginUseCase loginUseCase;

    @Mock
    private ObtenerClavesPublicasUseCase clavesUseCase;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        var handler = new UsuarioHandler(useCase, consultarUseCase, consultarPorCorreosUseCase, new UsuarioRestMapperImpl(),
                new RequestValidator(Validation.buildDefaultValidatorFactory().getValidator()));
        var autenticacion = new AutenticacionHandler(loginUseCase, clavesUseCase, new AutenticacionRestMapperImpl(),
                new RequestValidator(Validation.buildDefaultValidatorFactory().getValidator()));
        client = ApiTestSupport.cliente(handler, autenticacion);
    }

    private WebTestClient.ResponseSpec post(String cuerpo) {
        return client.post().uri(RUTA).contentType(MediaType.APPLICATION_JSON).bodyValue(cuerpo).exchange();
    }

    @Test
    @DisplayName("cuando el cuerpo es valido, debe responder 201 con el usuario en snake_case y sin exponer datos internos")
    void cuandoCuerpoValido_debeResponder201() {
        var id = UUID.randomUUID();
        when(useCase.ejecutar(any())).thenReturn(Mono.just(new Usuario(id, "Maria", "Gomez", "1020304050",
                LocalDate.of(1995, 3, 20), "Calle 10", "+573001234567", "maria@example.com",
                new BigDecimal("4500000"), Rol.ASESOR)));

        post(CUERPO_VALIDO)
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo(id.toString())
                .jsonPath("$.nombres").isEqualTo("Maria")
                .jsonPath("$.documento_identidad").isEqualTo("1020304050")
                .jsonPath("$.correo_electronico").isEqualTo("maria@example.com")
                .jsonPath("$.salario_base").isEqualTo(4500000)
                .jsonPath("$.rol").isEqualTo("ASESOR")
                .jsonPath("$.password").doesNotExist()
                .jsonPath("$.password_hash").doesNotExist();

        var comando = ArgumentCaptor.forClass(RegistrarUsuarioCommand.class);
        verify(useCase).ejecutar(comando.capture());
        assertThat(comando.getValue().email()).isEqualTo("Maria@Example.com");
        assertThat(comando.getValue().documentoIdentidad()).isEqualTo("1020304050");
        assertThat(comando.getValue().rol()).isEqualTo("ASESOR");
        assertThat(comando.getValue().password()).isEqualTo("Clave-Segura-123");
    }

    @Test
    @DisplayName("cuando faltan campos obligatorios o el correo es invalido, debe responder 400 con el detalle por campo")
    void cuandoDatosInvalidos_debeResponder400ConDetalles() {
        post("""
                {"apellidos":"Gomez","documento_identidad":"1020304050","correo_electronico":"no-es-correo","salario_base":-5,
                 "password":"corta"}""")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("VALIDACION_FALLIDA")
                .jsonPath("$.estado").isEqualTo(400)
                .jsonPath("$.ruta").isEqualTo(RUTA)
                .jsonPath("$.detalles.nombres").isNotEmpty()
                .jsonPath("$.detalles.correo_electronico").isNotEmpty()
                .jsonPath("$.detalles.salario_base").isNotEmpty()
                .jsonPath("$.detalles.password").isNotEmpty();

        verify(useCase, never()).ejecutar(any());
    }

    @Test
    @DisplayName("cuando el salario supera 15000000, debe responder 400")
    void cuandoSalarioSuperaMaximo_debeResponder400() {
        post(CUERPO_VALIDO.replace("4500000", "15000001"))
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detalles.salario_base").isNotEmpty();
    }

    @Test
    @DisplayName("cuando no hay cuerpo, debe responder 400 indicando que es obligatorio")
    void cuandoSinCuerpo_debeResponder400() {
        client.post().uri(RUTA).contentType(MediaType.APPLICATION_JSON).exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("VALIDACION_FALLIDA")
                .jsonPath("$.detalles.cuerpo").isNotEmpty();
    }

    @Test
    @DisplayName("cuando el JSON esta mal formado, debe responder 400 sin detalles internos")
    void cuandoJsonMalFormado_debeResponder400() {
        post("{esto no es json")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("PETICION_INVALIDA")
                .jsonPath("$.mensaje").isEqualTo("La peticion no es valida o no esta soportada");
    }

    @Test
    @DisplayName("cuando el tipo de contenido no es JSON, debe responder 415")
    void cuandoContentTypeNoSoportado_debeResponder415() {
        client.post().uri(RUTA).contentType(MediaType.TEXT_PLAIN).bodyValue("hola").exchange()
                .expectStatus().isEqualTo(415)
                .expectBody().jsonPath("$.codigo").isEqualTo("PETICION_INVALIDA");
    }

    @Test
    @DisplayName("cuando el dominio rechaza los datos, debe responder 400 con el codigo del dominio")
    void cuandoDominioRechazaDatos_debeResponder400() {
        when(useCase.ejecutar(any())).thenReturn(Mono.error(new DatosUsuarioInvalidosException("nombres", "es obligatorio")));

        post(CUERPO_VALIDO)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("DATOS_USUARIO_INVALIDOS")
                .jsonPath("$.mensaje").isEqualTo("El campo 'nombres' es obligatorio");
    }

    @Test
    @DisplayName("cuando el usuario ya existe, debe responder 409")
    void cuandoUsuarioDuplicado_debeResponder409() {
        when(useCase.ejecutar(any())).thenReturn(Mono.error(new UsuarioDuplicadoException("correo electronico")));

        post(CUERPO_VALIDO)
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.codigo").isEqualTo("USUARIO_DUPLICADO");
    }

    @Test
    @DisplayName("cuando ocurre un error inesperado, debe responder 500 con mensaje generico sin filtrar detalles")
    void cuandoErrorInesperado_debeResponder500Generico() {
        when(useCase.ejecutar(any())).thenReturn(Mono.error(new IllegalStateException("password=super-secreto")));

        post(CUERPO_VALIDO)
                .expectStatus().is5xxServerError()
                .expectBody(String.class)
                .value(cuerpo -> assertThat(cuerpo)
                        .contains("ERROR_INTERNO")
                        .contains("Ocurrio un error interno")
                        .doesNotContain("super-secreto")
                        .doesNotContain("IllegalStateException"));
    }

    @Test
    @DisplayName("cuando la ruta no existe, debe responder 404 con el formato de error uniforme")
    void cuandoRutaInexistente_debeResponder404() {
        client.get().uri("/api/v1/no-existe").exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.codigo").isEqualTo("PETICION_INVALIDA");
    }

    @Test
    @DisplayName("GET por documento: cuando el usuario existe, debe responder 200 con sus datos")
    void consultar_cuandoExiste_debeResponder200() {
        var id = UUID.randomUUID();
        when(consultarUseCase.porDocumentoIdentidad("1020304050")).thenReturn(Mono.just(new Usuario(id, "Maria", "Gomez",
                "1020304050", null, null, null, "maria@example.com", new BigDecimal("4500000"), Rol.CLIENTE)));

        client.get().uri(RUTA + "/1020304050").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id.toString())
                .jsonPath("$.correo_electronico").isEqualTo("maria@example.com")
                .jsonPath("$.salario_base").isEqualTo(4500000)
                .jsonPath("$.rol").isEqualTo("CLIENTE");
    }

    @Test
    @DisplayName("GET por documento: cuando no existe, debe responder 404 con el codigo del dominio")
    void consultar_cuandoNoExiste_debeResponder404() {
        when(consultarUseCase.porDocumentoIdentidad("0000000")).thenReturn(Mono.error(new UsuarioNoEncontradoException()));

        client.get().uri(RUTA + "/0000000").exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("USUARIO_NO_ENCONTRADO")
                .jsonPath("$.estado").isEqualTo(404);
    }

    @Test
    @DisplayName("cuando falta la clave, debe responder 400 indicando el campo password")
    void cuandoFaltaPassword_debeResponder400() {
        post(CUERPO_VALIDO.replace("\"password\":\"Clave-Segura-123\"", "\"x\":1"))
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detalles.password").isNotEmpty();
    }

    @Test
    @DisplayName("cuando el dominio deniega el acceso, debe responder 403 con el codigo del dominio")
    void cuandoDominioDeniegaAcceso_debeResponder403() {
        when(useCase.ejecutar(any())).thenReturn(Mono.error(new AccesoDenegadoException()));

        post(CUERPO_VALIDO)
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.codigo").isEqualTo("ACCESO_DENEGADO");
    }

    @Test
    @DisplayName("el DTO de registro no debe imprimir la clave en su toString")
    void dto_noDebeImprimirLaClave() {
        var dto = new com.crediya.autenticacion.api.dto.request.RegistrarUsuarioRequest("M", "G", "123456", null, null, null,
                "m@g.co", BigDecimal.TEN, null, "Clave-Secreta-1");

        assertThat(dto.toString()).doesNotContain("Clave-Secreta-1").contains("password=****");
    }

    @Test
    @DisplayName("GET por correos: debe aceptar correos separados por coma y repetidos, y devolver la lista sin datos sensibles")
    void consultarPorCorreos_debeDevolverLista() {
        when(consultarPorCorreosUseCase.ejecutar(any())).thenReturn(reactor.core.publisher.Flux.just(
                new Usuario(UUID.randomUUID(), "Maria", "Gomez", "1020304050", null, null, null, "maria@example.com",
                        new BigDecimal("4500000"), Rol.CLIENTE, "$2a$10$hash-secreto")));

        client.get().uri(uri -> uri.path(RUTA).queryParam("correos", "maria@example.com,pedro@example.com")
                        .queryParam("correos", "ana@example.com").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(cuerpo -> assertThat(cuerpo).contains("maria@example.com").contains("\"salario_base\":4500000")
                        .doesNotContain("hash-secreto").doesNotContain("password"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<String>> capturado = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(consultarPorCorreosUseCase).ejecutar(capturado.capture());
        assertThat(capturado.getValue()).containsExactly("maria@example.com", "pedro@example.com", "ana@example.com");
    }

    @Test
    @DisplayName("GET por correos: cuando falta el parametro, debe responder 400 con el detalle")
    void consultarPorCorreos_sinParametro_debeResponder400() {
        client.get().uri(RUTA).exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("VALIDACION_FALLIDA")
                .jsonPath("$.detalles.correos").isNotEmpty();

        verify(consultarPorCorreosUseCase, never()).ejecutar(any());
    }

    @Test
    @DisplayName("GET por correos: cuando el dominio deniega el acceso, debe responder 403")
    void consultarPorCorreos_denegado_debeResponder403() {
        when(consultarPorCorreosUseCase.ejecutar(any())).thenReturn(reactor.core.publisher.Flux.error(new AccesoDenegadoException()));

        client.get().uri(RUTA + "?correos=a@b.co").exchange()
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.codigo").isEqualTo("ACCESO_DENEGADO");
    }
}
