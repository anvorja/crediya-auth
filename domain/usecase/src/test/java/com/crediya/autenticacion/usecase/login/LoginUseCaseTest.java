package com.crediya.autenticacion.usecase.login;

import com.crediya.autenticacion.model.seguridad.Token;
import com.crediya.autenticacion.model.seguridad.gateways.PasswordEncoderGateway;
import com.crediya.autenticacion.model.seguridad.gateways.TokenGateway;
import com.crediya.autenticacion.model.usuario.Rol;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.commands.LoginCommand;
import com.crediya.autenticacion.model.usuario.exceptions.CredencialesInvalidasException;
import com.crediya.autenticacion.model.usuario.gateways.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginUseCase (HU3)")
class LoginUseCaseTest {

    private static final String HASH = "$2a$10$hash-real-del-usuario";
    private static final Usuario USUARIO = new Usuario(UUID.randomUUID(), "Maria", "Gomez", "1020304050", null, null, null,
            "maria@example.com", new BigDecimal("4500000"), Rol.CLIENTE, HASH);

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoderGateway passwordEncoder;
    @Mock
    private TokenGateway tokenGateway;

    @InjectMocks
    private LoginUseCase useCase;

    @Test
    @DisplayName("cuando el correo y la clave son correctos, debe emitir el token (el correo se normaliza)")
    void cuandoCredencialesCorrectas_debeEmitirToken() {
        var token = Token.bearer("jwt", 3600);
        when(usuarioRepository.buscarPorEmail("maria@example.com")).thenReturn(Mono.just(USUARIO));
        when(passwordEncoder.coincide("Clave-Segura-123", HASH)).thenReturn(Mono.just(true));
        when(tokenGateway.emitir(USUARIO)).thenReturn(Mono.just(token));

        StepVerifier.create(useCase.ejecutar(new LoginCommand("  Maria@Example.COM ", "Clave-Segura-123")))
                .expectNext(token)
                .verifyComplete();
    }

    @Test
    @DisplayName("cuando la clave es incorrecta, debe fallar con CredencialesInvalidasException sin emitir token")
    void cuandoClaveIncorrecta_debeFallar() {
        when(usuarioRepository.buscarPorEmail("maria@example.com")).thenReturn(Mono.just(USUARIO));
        when(passwordEncoder.coincide("mala-clave", HASH)).thenReturn(Mono.just(false));
        when(passwordEncoder.coincide("mala-clave", LoginUseCase.HASH_FICTICIO)).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.ejecutar(new LoginCommand("maria@example.com", "mala-clave")))
                .expectError(CredencialesInvalidasException.class)
                .verify();

        verify(tokenGateway, never()).emitir(any());
    }

    @Test
    @DisplayName("cuando el usuario no existe, debe fallar igual y comparar contra un hash ficticio para no delatar la cuenta por tiempo")
    void cuandoUsuarioNoExiste_debeFallarIgualYEcualizarTiempo() {
        when(usuarioRepository.buscarPorEmail("nadie@example.com")).thenReturn(Mono.empty());
        when(passwordEncoder.coincide("cualquiera-123", LoginUseCase.HASH_FICTICIO)).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.ejecutar(new LoginCommand("nadie@example.com", "cualquiera-123")))
                .expectError(CredencialesInvalidasException.class)
                .verify();

        verify(passwordEncoder).coincide("cualquiera-123", LoginUseCase.HASH_FICTICIO);
        verify(tokenGateway, never()).emitir(any());
    }

    @Test
    @DisplayName("cuando el usuario no tiene clave registrada, debe fallar como credenciales invalidas")
    void cuandoUsuarioSinHash_debeFallar() {
        var sinHash = USUARIO.conPasswordHash(null);
        when(usuarioRepository.buscarPorEmail("maria@example.com")).thenReturn(Mono.just(sinHash));
        when(passwordEncoder.coincide("Clave-Segura-123", LoginUseCase.HASH_FICTICIO)).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.ejecutar(new LoginCommand("maria@example.com", "Clave-Segura-123")))
                .expectError(CredencialesInvalidasException.class)
                .verify();
    }

    @ParameterizedTest(name = "email = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("cuando el correo viene vacio, debe fallar como credenciales invalidas sin consultar nada")
    void cuandoEmailVacio_debeFallarSinConsultar(String email) {
        StepVerifier.create(useCase.ejecutar(new LoginCommand(email, "Clave-Segura-123")))
                .expectError(CredencialesInvalidasException.class)
                .verify();

        verifyNoInteractions(usuarioRepository, passwordEncoder, tokenGateway);
    }

    @ParameterizedTest(name = "password = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("cuando la clave viene vacia, debe fallar como credenciales invalidas sin consultar nada")
    void cuandoPasswordVacia_debeFallarSinConsultar(String password) {
        StepVerifier.create(useCase.ejecutar(new LoginCommand("maria@example.com", password)))
                .expectError(CredencialesInvalidasException.class)
                .verify();

        verifyNoInteractions(usuarioRepository, passwordEncoder, tokenGateway);
    }

    @Test
    @DisplayName("cuando no llega el comando, debe fallar como credenciales invalidas")
    void cuandoComandoNulo_debeFallar() {
        StepVerifier.create(useCase.ejecutar(null))
                .expectError(CredencialesInvalidasException.class)
                .verify();
    }
}
