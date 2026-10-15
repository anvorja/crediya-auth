package com.crediya.autenticacion.usecase.registrarusuario;

import com.crediya.autenticacion.model.common.gateways.TransaccionGateway;
import com.crediya.autenticacion.model.usuario.Rol;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.commands.RegistrarUsuarioCommand;
import com.crediya.autenticacion.model.usuario.exceptions.DatosUsuarioInvalidosException;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioDuplicadoException;
import com.crediya.autenticacion.model.usuario.gateways.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrarUsuarioUseCase (HU1)")
class RegistrarUsuarioUseCaseTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2025-06-15T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TransaccionGateway transaccionGateway;

    private RegistrarUsuarioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegistrarUsuarioUseCase(usuarioRepository, transaccionGateway, RELOJ);
    }

    private void transaccionDejaPasar() {
        when(transaccionGateway.ejecutar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private static RegistrarUsuarioCommand comando() {
        return new RegistrarUsuarioCommand("Maria", "Gomez", "1020304050", LocalDate.of(1995, 3, 20),
                "Calle 10", "+573001234567", "Maria.Gomez@Example.com", new BigDecimal("4500000"), null);
    }

    @Test
    @DisplayName("cuando los datos son validos y no existen duplicados, debe guardar y devolver el usuario")
    void cuandoDatosValidos_debeGuardarYDevolverUsuario() {
        transaccionDejaPasar();
        var id = UUID.randomUUID();
        when(usuarioRepository.existePorEmail("maria.gomez@example.com")).thenReturn(Mono.just(false));
        when(usuarioRepository.existePorDocumentoIdentidad("1020304050")).thenReturn(Mono.just(false));
        when(usuarioRepository.guardar(any(Usuario.class))).thenAnswer(inv -> Mono.just(((Usuario) inv.getArgument(0)).conId(id)));

        StepVerifier.create(useCase.ejecutar(comando()))
                .assertNext(usuario -> {
                    assertThat(usuario.id()).isEqualTo(id);
                    assertThat(usuario.email()).isEqualTo("maria.gomez@example.com");
                    assertThat(usuario.rol()).isEqualTo(Rol.CLIENTE);
                })
                .verifyComplete();

        var capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).guardar(capturado.capture());
        assertThat(capturado.getValue().id()).isNull();
        verify(transaccionGateway).ejecutar(any());
    }

    @Test
    @DisplayName("cuando el correo ya esta registrado, debe fallar con UsuarioDuplicadoException y no guardar")
    void cuandoEmailDuplicado_debeFallarSinGuardar() {
        transaccionDejaPasar();
        when(usuarioRepository.existePorEmail(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.ejecutar(comando()))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(UsuarioDuplicadoException.class);
                    assertThat(((UsuarioDuplicadoException) error).getCampo()).isEqualTo(RegistrarUsuarioUseCase.CAMPO_EMAIL);
                })
                .verify();

        verify(usuarioRepository, never()).existePorDocumentoIdentidad(anyString());
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("cuando el documento ya esta registrado, debe fallar con UsuarioDuplicadoException y no guardar")
    void cuandoDocumentoDuplicado_debeFallarSinGuardar() {
        transaccionDejaPasar();
        when(usuarioRepository.existePorEmail(anyString())).thenReturn(Mono.just(false));
        when(usuarioRepository.existePorDocumentoIdentidad(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.ejecutar(comando()))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(UsuarioDuplicadoException.class);
                    assertThat(((UsuarioDuplicadoException) error).getCampo()).isEqualTo(RegistrarUsuarioUseCase.CAMPO_DOCUMENTO);
                })
                .verify();

        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("cuando los datos son invalidos, debe fallar con DatosUsuarioInvalidosException sin consultar el repositorio")
    void cuandoDatosInvalidos_debeFallarSinTocarRepositorio() {
        transaccionDejaPasar();
        var invalido = new RegistrarUsuarioCommand(" ", "Gomez", "1020304050", null, null, null,
                "maria@example.com", BigDecimal.ONE, null);

        StepVerifier.create(useCase.ejecutar(invalido))
                .expectError(DatosUsuarioInvalidosException.class)
                .verify();

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("cuando el repositorio falla al guardar, debe propagar el error")
    void cuandoGuardarFalla_debePropagarError() {
        transaccionDejaPasar();
        when(usuarioRepository.existePorEmail(anyString())).thenReturn(Mono.just(false));
        when(usuarioRepository.existePorDocumentoIdentidad(anyString())).thenReturn(Mono.just(false));
        when(usuarioRepository.guardar(any())).thenReturn(Mono.error(new IllegalStateException("db caida")));

        StepVerifier.create(useCase.ejecutar(comando()))
                .expectErrorMessage("db caida")
                .verify();
    }
}
