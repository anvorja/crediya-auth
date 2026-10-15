package com.crediya.autenticacion.api.usuario;

import com.crediya.autenticacion.api.RouterRest;
import com.crediya.autenticacion.api.exception.GlobalErrorHandler;
import com.crediya.autenticacion.api.mapper.UsuarioRestMapperImpl;
import com.crediya.autenticacion.api.validation.RequestValidator;
import com.crediya.autenticacion.model.usuario.Rol;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.commands.RegistrarUsuarioCommand;
import com.crediya.autenticacion.model.usuario.exceptions.DatosUsuarioInvalidosException;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioDuplicadoException;
import com.crediya.autenticacion.usecase.registrarusuario.RegistrarUsuarioUseCase;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
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
             "correo_electronico":"Maria@Example.com","salario_base":4500000,"rol":"ASESOR"}""";

    @Mock
    private RegistrarUsuarioUseCase useCase;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        var handler = new UsuarioHandler(useCase, new UsuarioRestMapperImpl(),
                new RequestValidator(Validation.buildDefaultValidatorFactory().getValidator()));
        var errorHandler = new GlobalErrorHandler(new DefaultErrorAttributes(), new WebProperties(),
                new StaticApplicationContext(), ServerCodecConfigurer.create());
        client = WebTestClient.bindToRouterFunction(new RouterRest().routerFunction(handler))
                .handlerStrategies(estrategiasConManejadorGlobal(errorHandler))
                .build();
    }

    /**
     * HandlerStrategies.builder() ya registra un manejador de ResponseStatusException que respondería antes
     * que el nuestro; en la aplicacion real el manejador global se ejecuta primero (@Order(-2)).
     */
    private static HandlerStrategies estrategiasConManejadorGlobal(GlobalErrorHandler errorHandler) {
        return HandlerStrategies.empty()
                .codecs(configurer -> configurer.registerDefaults(true))
                .exceptionHandler(errorHandler)
                .build();
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
                .jsonPath("$.rol").isEqualTo("ASESOR");

        var comando = ArgumentCaptor.forClass(RegistrarUsuarioCommand.class);
        verify(useCase).ejecutar(comando.capture());
        assertThat(comando.getValue().email()).isEqualTo("Maria@Example.com");
        assertThat(comando.getValue().documentoIdentidad()).isEqualTo("1020304050");
        assertThat(comando.getValue().rol()).isEqualTo("ASESOR");
    }

    @Test
    @DisplayName("cuando faltan campos obligatorios o el correo es invalido, debe responder 400 con el detalle por campo")
    void cuandoDatosInvalidos_debeResponder400ConDetalles() {
        post("""
                {"apellidos":"Gomez","documento_identidad":"1020304050","correo_electronico":"no-es-correo","salario_base":-5}""")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("VALIDACION_FALLIDA")
                .jsonPath("$.estado").isEqualTo(400)
                .jsonPath("$.ruta").isEqualTo(RUTA)
                .jsonPath("$.detalles.nombres").isNotEmpty()
                .jsonPath("$.detalles.correo_electronico").isNotEmpty()
                .jsonPath("$.detalles.salario_base").isNotEmpty();

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
}
