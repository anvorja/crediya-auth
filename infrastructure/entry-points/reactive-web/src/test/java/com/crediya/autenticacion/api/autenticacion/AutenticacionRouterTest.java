package com.crediya.autenticacion.api.autenticacion;

import com.crediya.autenticacion.api.ApiTestSupport;
import com.crediya.autenticacion.api.mapper.AutenticacionRestMapperImpl;
import com.crediya.autenticacion.api.mapper.UsuarioRestMapperImpl;
import com.crediya.autenticacion.api.usuario.UsuarioHandler;
import com.crediya.autenticacion.api.validation.RequestValidator;
import com.crediya.autenticacion.model.seguridad.Token;
import com.crediya.autenticacion.model.usuario.commands.LoginCommand;
import com.crediya.autenticacion.model.usuario.exceptions.CredencialesInvalidasException;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("POST /api/v1/login y GET /.well-known/jwks.json (HU3)")
class AutenticacionRouterTest {

    private static final String LOGIN = "/api/v1/login";
    private static final String CUERPO_LOGIN = """
            {"correo_electronico":"maria@example.com","password":"Clave-Segura-123"}""";

    @Mock
    private LoginUseCase loginUseCase;
    @Mock
    private ObtenerClavesPublicasUseCase clavesUseCase;
    @Mock
    private RegistrarUsuarioUseCase registrarUseCase;
    @Mock
    private ConsultarUsuarioUseCase consultarUseCase;

    @Mock
    private ConsultarUsuariosPorCorreoUseCase consultarPorCorreosUseCase;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        var validador = new RequestValidator(Validation.buildDefaultValidatorFactory().getValidator());
        client = ApiTestSupport.cliente(
                new UsuarioHandler(registrarUseCase, consultarUseCase, consultarPorCorreosUseCase, new UsuarioRestMapperImpl(), validador),
                new AutenticacionHandler(loginUseCase, clavesUseCase, new AutenticacionRestMapperImpl(), validador));
    }

    private WebTestClient.ResponseSpec login(String cuerpo) {
        return client.post().uri(LOGIN).contentType(MediaType.APPLICATION_JSON).bodyValue(cuerpo).exchange();
    }

    @Test
    @DisplayName("cuando las credenciales son correctas, debe responder 200 con el token en formato OAuth2 y sin cache")
    void cuandoCredencialesCorrectas_debeResponder200() {
        when(loginUseCase.ejecutar(any())).thenReturn(Mono.just(Token.bearer("jwt-firmado", 3600)));

        login(CUERPO_LOGIN)
                .expectStatus().isOk()
                .expectHeader().cacheControl(org.springframework.http.CacheControl.noStore())
                .expectBody()
                .jsonPath("$.access_token").isEqualTo("jwt-firmado")
                .jsonPath("$.token_type").isEqualTo("Bearer")
                .jsonPath("$.expires_in").isEqualTo(3600);

        var comando = ArgumentCaptor.forClass(LoginCommand.class);
        verify(loginUseCase).ejecutar(comando.capture());
        assertThat(comando.getValue().email()).isEqualTo("maria@example.com");
        assertThat(comando.getValue().password()).isEqualTo("Clave-Segura-123");
    }

    @Test
    @DisplayName("cuando las credenciales son invalidas, debe responder 401 con un mensaje que no revela si el usuario existe")
    void cuandoCredencialesInvalidas_debeResponder401() {
        when(loginUseCase.ejecutar(any())).thenReturn(Mono.error(new CredencialesInvalidasException()));

        login(CUERPO_LOGIN)
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("CREDENCIALES_INVALIDAS")
                .jsonPath("$.mensaje").isEqualTo("Correo o clave incorrectos");
    }

    @Test
    @DisplayName("cuando faltan correo o clave, debe responder 400 con el detalle y sin llamar al caso de uso")
    void cuandoFaltanCampos_debeResponder400() {
        login("{}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("VALIDACION_FALLIDA")
                .jsonPath("$.detalles.correo_electronico").isNotEmpty()
                .jsonPath("$.detalles.password").isNotEmpty();

        verify(loginUseCase, never()).ejecutar(any());
    }

    @Test
    @DisplayName("cuando no hay cuerpo, debe responder 400")
    void cuandoSinCuerpo_debeResponder400() {
        client.post().uri(LOGIN).contentType(MediaType.APPLICATION_JSON).exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detalles.cuerpo").isNotEmpty();
    }

    @Test
    @DisplayName("cuando ocurre un error inesperado en el login, debe responder 500 generico")
    void cuandoErrorInesperado_debeResponder500() {
        when(loginUseCase.ejecutar(any())).thenReturn(Mono.error(new IllegalStateException("clave-privada=xyz")));

        login(CUERPO_LOGIN)
                .expectStatus().is5xxServerError()
                .expectBody(String.class)
                .value(cuerpo -> assertThat(cuerpo).contains("ERROR_INTERNO").doesNotContain("clave-privada"));
    }

    @Test
    @DisplayName("el DTO de login no debe imprimir la clave y el de token no debe imprimir el token")
    void dtos_noDebenImprimirSecretos() {
        assertThat(new com.crediya.autenticacion.api.dto.request.LoginRequest("a@b.co", "Clave-Secreta-1").toString())
                .doesNotContain("Clave-Secreta-1");
        assertThat(new com.crediya.autenticacion.api.dto.response.TokenResponse("jwt-secreto", "Bearer", 60).toString())
                .doesNotContain("jwt-secreto");
    }

    @Test
    @DisplayName("el endpoint JWKS debe publicar el conjunto de claves como JSON")
    void jwks_debePublicarClaves() {
        Map<String, Object> jwks = Map.of("keys", List.of(Map.of("kty", "RSA", "kid", "k1", "n", "abc", "e", "AQAB")));
        when(clavesUseCase.ejecutar()).thenReturn(Mono.just(jwks));

        client.get().uri("/.well-known/jwks.json").exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectHeader().exists("Cache-Control")
                .expectBody()
                .jsonPath("$.keys[0].kty").isEqualTo("RSA")
                .jsonPath("$.keys[0].kid").isEqualTo("k1");
    }
}
