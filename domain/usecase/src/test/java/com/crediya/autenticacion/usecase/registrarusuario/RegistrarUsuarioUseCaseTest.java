package com.crediya.autenticacion.usecase.registrarusuario;

import com.crediya.autenticacion.model.common.exceptions.AccesoDenegadoException;
import com.crediya.autenticacion.model.common.gateways.TransaccionGateway;
import com.crediya.autenticacion.model.seguridad.UsuarioAutenticado;
import com.crediya.autenticacion.model.seguridad.gateways.ContextoSeguridadGateway;
import com.crediya.autenticacion.model.seguridad.gateways.PasswordEncoderGateway;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
@DisplayName("RegistrarUsuarioUseCase (HU1 + HU3)")
class RegistrarUsuarioUseCaseTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2025-06-15T10:00:00Z"), ZoneOffset.UTC);
    private static final String HASH = "$2a$10$hash-de-prueba";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TransaccionGateway transaccionGateway;
    @Mock
    private PasswordEncoderGateway passwordEncoder;
    @Mock
    private ContextoSeguridadGateway contextoSeguridad;

    private RegistrarUsuarioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegistrarUsuarioUseCase(usuarioRepository, transaccionGateway, passwordEncoder, contextoSeguridad, RELOJ);
    }

    private void actuaComo(Rol rol) {
        when(contextoSeguridad.usuarioActual()).thenReturn(Mono.just(
                new UsuarioAutenticado(UUID.randomUUID(), "9999999", "actor@crediya.co", rol)));
    }

    private void transaccionDejaPasar() {
        when(transaccionGateway.ejecutar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private static RegistrarUsuarioCommand comando(String rol) {
        return new RegistrarUsuarioCommand("Maria", "Gomez", "1020304050", LocalDate.of(1995, 3, 20),
                "Calle 10", "+573001234567", "Maria.Gomez@Example.com", new BigDecimal("4500000"), rol, "Clave-Segura-123");
    }

    private void repositorioSinDuplicados() {
        var id = UUID.randomUUID();
        when(usuarioRepository.existePorEmail("maria.gomez@example.com")).thenReturn(Mono.just(false));
        when(usuarioRepository.existePorDocumentoIdentidad("1020304050")).thenReturn(Mono.just(false));
        when(passwordEncoder.codificar("Clave-Segura-123")).thenReturn(Mono.just(HASH));
        when(usuarioRepository.guardar(any(Usuario.class))).thenAnswer(inv -> Mono.just(((Usuario) inv.getArgument(0)).conId(id)));
    }

    @ParameterizedTest(name = "actor {0}")
    @EnumSource(value = Rol.class, names = {"ADMIN", "ASESOR"})
    @DisplayName("cuando un ADMIN o ASESOR registra un CLIENTE, debe guardar con la clave codificada")
    void cuandoPersonalInternoRegistraCliente_debeGuardarConHash(Rol actor) {
        actuaComo(actor);
        transaccionDejaPasar();
        repositorioSinDuplicados();

        StepVerifier.create(useCase.ejecutar(comando(null)))
                .assertNext(usuario -> {
                    assertThat(usuario.id()).isNotNull();
                    assertThat(usuario.email()).isEqualTo("maria.gomez@example.com");
                    assertThat(usuario.rol()).isEqualTo(Rol.CLIENTE);
                    assertThat(usuario.passwordHash()).isEqualTo(HASH);
                })
                .verifyComplete();

        var capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).guardar(capturado.capture());
        assertThat(capturado.getValue().passwordHash()).isEqualTo(HASH).isNotEqualTo("Clave-Segura-123");
        verify(transaccionGateway).ejecutar(any());
    }

    @Test
    @DisplayName("cuando un ADMIN registra personal interno, debe permitirlo")
    void cuandoAdminRegistraPersonalInterno_debePermitirlo() {
        actuaComo(Rol.ADMIN);
        transaccionDejaPasar();
        repositorioSinDuplicados();

        StepVerifier.create(useCase.ejecutar(comando("asesor")))
                .assertNext(usuario -> assertThat(usuario.rol()).isEqualTo(Rol.ASESOR))
                .verifyComplete();
    }

    @ParameterizedTest(name = "rol solicitado {0}")
    @EnumSource(value = Rol.class, names = {"ADMIN", "ASESOR"})
    @DisplayName("cuando un ASESOR intenta crear personal interno, debe denegar para evitar escalada de privilegios")
    void cuandoAsesorCreaPersonalInterno_debeDenegar(Rol solicitado) {
        actuaComo(Rol.ASESOR);

        StepVerifier.create(useCase.ejecutar(comando(solicitado.name())))
                .expectError(AccesoDenegadoException.class)
                .verify();

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("cuando quien llama es un CLIENTE, debe denegar sin tocar el repositorio")
    void cuandoActorEsCliente_debeDenegar() {
        actuaComo(Rol.CLIENTE);

        StepVerifier.create(useCase.ejecutar(comando(null)))
                .expectError(AccesoDenegadoException.class)
                .verify();

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("cuando no hay usuario autenticado, debe denegar")
    void cuandoSinUsuarioAutenticado_debeDenegar() {
        when(contextoSeguridad.usuarioActual()).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(comando(null)))
                .expectError(AccesoDenegadoException.class)
                .verify();

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("cuando el rol solicitado no existe y quien llama es ASESOR, debe rechazar por datos invalidos")
    void cuandoRolInexistente_debeRechazar() {
        actuaComo(Rol.ASESOR);

        StepVerifier.create(useCase.ejecutar(comando("SUPERUSUARIO")))
                .expectError(DatosUsuarioInvalidosException.class)
                .verify();
    }

    @Test
    @DisplayName("cuando el correo ya esta registrado, debe fallar con UsuarioDuplicadoException y no guardar")
    void cuandoEmailDuplicado_debeFallarSinGuardar() {
        actuaComo(Rol.ADMIN);
        transaccionDejaPasar();
        when(usuarioRepository.existePorEmail(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.ejecutar(comando(null)))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(UsuarioDuplicadoException.class);
                    assertThat(((UsuarioDuplicadoException) error).getCampo()).isEqualTo(RegistrarUsuarioUseCase.CAMPO_EMAIL);
                })
                .verify();

        verify(usuarioRepository, never()).existePorDocumentoIdentidad(anyString());
        verify(usuarioRepository, never()).guardar(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("cuando el documento ya esta registrado, debe fallar con UsuarioDuplicadoException y no guardar")
    void cuandoDocumentoDuplicado_debeFallarSinGuardar() {
        actuaComo(Rol.ADMIN);
        transaccionDejaPasar();
        when(usuarioRepository.existePorEmail(anyString())).thenReturn(Mono.just(false));
        when(usuarioRepository.existePorDocumentoIdentidad(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.ejecutar(comando(null)))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(UsuarioDuplicadoException.class);
                    assertThat(((UsuarioDuplicadoException) error).getCampo()).isEqualTo(RegistrarUsuarioUseCase.CAMPO_DOCUMENTO);
                })
                .verify();

        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("cuando los datos son invalidos, debe fallar sin consultar el repositorio ni codificar la clave")
    void cuandoDatosInvalidos_debeFallarSinTocarRepositorio() {
        actuaComo(Rol.ADMIN);
        transaccionDejaPasar();
        var invalido = new RegistrarUsuarioCommand(" ", "Gomez", "1020304050", null, null, null,
                "maria@example.com", BigDecimal.ONE, null, "Clave-Segura-123");

        StepVerifier.create(useCase.ejecutar(invalido))
                .expectError(DatosUsuarioInvalidosException.class)
                .verify();

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("cuando la clave es demasiado corta, debe rechazar antes de consultar el repositorio")
    void cuandoPasswordCorta_debeRechazar() {
        actuaComo(Rol.ADMIN);
        transaccionDejaPasar();
        var corta = new RegistrarUsuarioCommand("Maria", "Gomez", "1020304050", null, null, null,
                "maria@example.com", BigDecimal.ONE, null, "corta");

        StepVerifier.create(useCase.ejecutar(corta))
                .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(DatosUsuarioInvalidosException.class)
                        .hasMessageContaining("password"))
                .verify();

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("cuando el repositorio falla al guardar, debe propagar el error")
    void cuandoGuardarFalla_debePropagarError() {
        actuaComo(Rol.ADMIN);
        transaccionDejaPasar();
        when(usuarioRepository.existePorEmail(anyString())).thenReturn(Mono.just(false));
        when(usuarioRepository.existePorDocumentoIdentidad(anyString())).thenReturn(Mono.just(false));
        when(passwordEncoder.codificar(anyString())).thenReturn(Mono.just(HASH));
        when(usuarioRepository.guardar(any())).thenReturn(Mono.error(new IllegalStateException("db caida")));

        StepVerifier.create(useCase.ejecutar(comando(null)))
                .expectErrorMessage("db caida")
                .verify();
    }
}
