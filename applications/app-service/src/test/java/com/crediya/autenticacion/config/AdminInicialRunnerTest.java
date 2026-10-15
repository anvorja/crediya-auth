package com.crediya.autenticacion.config;

import com.crediya.autenticacion.model.usuario.Rol;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.commands.RegistrarUsuarioCommand;
import com.crediya.autenticacion.model.usuario.gateways.UsuarioRepository;
import com.crediya.autenticacion.usecase.registrarusuario.RegistrarUsuarioUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminInicialRunner")
class AdminInicialRunnerTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RegistrarUsuarioUseCase registrarUsuarioUseCase;

    private AdminInicialRunner runner;

    @BeforeEach
    void setUp() {
        var propiedades = new AdminInicialProperties("admin@crediya.local", "Clave-Admin-123", "0000000001", "Administrador", "CrediYa");
        runner = new AdminInicialRunner(usuarioRepository, registrarUsuarioUseCase, propiedades);
    }

    @Test
    @DisplayName("cuando no existe ningun ADMIN, debe registrar el administrador inicial con los datos configurados")
    void cuandoNoHayAdmin_debeCrearlo() {
        when(usuarioRepository.existePorRol(Rol.ADMIN)).thenReturn(Mono.just(false));
        when(registrarUsuarioUseCase.ejecutar(any())).thenReturn(Mono.just(new Usuario(UUID.randomUUID(), "Administrador",
                "CrediYa", "0000000001", null, null, null, "admin@crediya.local", BigDecimal.ZERO, Rol.ADMIN)));

        runner.run(new DefaultApplicationArguments());

        var comando = ArgumentCaptor.forClass(RegistrarUsuarioCommand.class);
        verify(registrarUsuarioUseCase).ejecutar(comando.capture());
        assertThat(comando.getValue().email()).isEqualTo("admin@crediya.local");
        assertThat(comando.getValue().documentoIdentidad()).isEqualTo("0000000001");
        assertThat(comando.getValue().rol()).isEqualTo("ADMIN");
        assertThat(comando.getValue().password()).isEqualTo("Clave-Admin-123");
    }

    @Test
    @DisplayName("cuando ya existe un ADMIN, no debe crear otro")
    void cuandoYaHayAdmin_noDebeCrearOtro() {
        when(usuarioRepository.existePorRol(Rol.ADMIN)).thenReturn(Mono.just(true));

        runner.run(new DefaultApplicationArguments());

        verify(registrarUsuarioUseCase, never()).ejecutar(any());
    }

    @Test
    @DisplayName("las propiedades no deben imprimir la clave del administrador")
    void propiedades_noDebenImprimirLaClave() {
        var propiedades = new AdminInicialProperties("a@b.co", "Clave-Admin-123", "1", "N", "A");

        assertThat(propiedades.toString()).doesNotContain("Clave-Admin-123").contains("password=****");
    }
}
