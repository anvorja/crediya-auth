package com.crediya.autenticacion.usecase.consultarusuarios;

import com.crediya.autenticacion.model.common.exceptions.AccesoDenegadoException;
import com.crediya.autenticacion.model.seguridad.UsuarioAutenticado;
import com.crediya.autenticacion.model.seguridad.gateways.ContextoSeguridadGateway;
import com.crediya.autenticacion.model.usuario.Rol;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.exceptions.DatosUsuarioInvalidosException;
import com.crediya.autenticacion.model.usuario.gateways.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarUsuariosPorCorreoUseCase (HU4)")
class ConsultarUsuariosPorCorreoUseCaseTest {

    private static final Usuario MARIA = new Usuario(UUID.randomUUID(), "Maria", "Gomez", "1020304050", null, null, null,
            "maria@example.com", new BigDecimal("4500000"), Rol.CLIENTE);

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ContextoSeguridadGateway contextoSeguridad;

    @InjectMocks
    private ConsultarUsuariosPorCorreoUseCase useCase;

    private void actuaComo(Rol rol) {
        when(contextoSeguridad.usuarioActual()).thenReturn(Mono.just(
                new UsuarioAutenticado(UUID.randomUUID(), "9999999", "actor@crediya.co", rol)));
    }

    @ParameterizedTest(name = "actor {0}")
    @EnumSource(value = Rol.class, names = {"ADMIN", "ASESOR"})
    @DisplayName("cuando el personal interno consulta, debe normalizar los correos (minusculas, sin duplicados ni vacios)")
    void cuandoPersonalInterno_debeNormalizarYConsultar(Rol actor) {
        actuaComo(actor);
        when(usuarioRepository.buscarPorEmails(anyCollection())).thenReturn(Flux.just(MARIA));

        StepVerifier.create(useCase.ejecutar(Arrays.asList(" Maria@Example.com ", "maria@example.com", "", "  ", null)))
                .expectNext(MARIA)
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> capturado = ArgumentCaptor.forClass(Collection.class);
        verify(usuarioRepository).buscarPorEmails(capturado.capture());
        assertThat(capturado.getValue()).containsExactly("maria@example.com");
    }

    @Test
    @DisplayName("cuando un CLIENTE consulta, debe denegar sin consultar el repositorio (expone datos de terceros)")
    void cuandoCliente_debeDenegar() {
        actuaComo(Rol.CLIENTE);

        StepVerifier.create(useCase.ejecutar(List.of("maria@example.com")))
                .expectError(AccesoDenegadoException.class)
                .verify();

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("cuando no hay usuario autenticado, debe denegar")
    void cuandoSinUsuario_debeDenegar() {
        when(contextoSeguridad.usuarioActual()).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(List.of("maria@example.com")))
                .expectError(AccesoDenegadoException.class)
                .verify();
    }

    @Test
    @DisplayName("cuando la lista esta vacia o es nula, debe devolver vacio sin consultar el repositorio")
    void cuandoListaVacia_debeDevolverVacio() {
        actuaComo(Rol.ASESOR);

        StepVerifier.create(useCase.ejecutar(null)).verifyComplete();
        StepVerifier.create(useCase.ejecutar(List.of(" ", ""))).verifyComplete();

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("cuando la lista supera el maximo de correos, debe rechazar para acotar la consulta")
    void cuandoDemasiadosCorreos_debeRechazar() {
        actuaComo(Rol.ASESOR);
        List<String> muchos = new ArrayList<>();
        for (int i = 0; i <= ConsultarUsuariosPorCorreoUseCase.MAXIMO_CORREOS; i++) {
            muchos.add("usuario" + i + "@example.com");
        }

        StepVerifier.create(useCase.ejecutar(muchos))
                .expectError(DatosUsuarioInvalidosException.class)
                .verify();

        verifyNoInteractions(usuarioRepository);
    }
}
