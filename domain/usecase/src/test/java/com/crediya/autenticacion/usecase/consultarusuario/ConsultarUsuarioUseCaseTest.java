package com.crediya.autenticacion.usecase.consultarusuario;

import com.crediya.autenticacion.model.common.exceptions.AccesoDenegadoException;
import com.crediya.autenticacion.model.seguridad.UsuarioAutenticado;
import com.crediya.autenticacion.model.seguridad.gateways.ContextoSeguridadGateway;
import com.crediya.autenticacion.model.usuario.Rol;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioNoEncontradoException;
import com.crediya.autenticacion.model.usuario.gateways.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarUsuarioUseCase (HU2 + HU3)")
class ConsultarUsuarioUseCaseTest {

    private static final Usuario USUARIO = new Usuario(UUID.randomUUID(), "Maria", "Gomez", "1020304050", null, null, null,
            "maria@example.com", new BigDecimal("4500000"), Rol.CLIENTE);

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ContextoSeguridadGateway contextoSeguridad;

    @InjectMocks
    private ConsultarUsuarioUseCase useCase;

    private void actuaComo(Rol rol, String documento) {
        when(contextoSeguridad.usuarioActual()).thenReturn(Mono.just(
                new UsuarioAutenticado(UUID.randomUUID(), documento, "actor@crediya.co", rol)));
    }

    @Test
    @DisplayName("cuando un CLIENTE consulta sus propios datos, debe devolverlos")
    void cuandoClienteConsultaSusDatos_debeDevolverlos() {
        actuaComo(Rol.CLIENTE, "1020304050");
        when(usuarioRepository.buscarPorDocumentoIdentidad("1020304050")).thenReturn(Mono.just(USUARIO));

        StepVerifier.create(useCase.porDocumentoIdentidad("1020304050")).expectNext(USUARIO).verifyComplete();
    }

    @Test
    @DisplayName("cuando un CLIENTE consulta los datos de otra persona, debe denegar sin consultar el repositorio")
    void cuandoClienteConsultaAjeno_debeDenegar() {
        actuaComo(Rol.CLIENTE, "1111111111");

        StepVerifier.create(useCase.porDocumentoIdentidad("1020304050"))
                .expectError(AccesoDenegadoException.class)
                .verify();

        verifyNoInteractions(usuarioRepository);
    }

    @ParameterizedTest(name = "actor {0}")
    @EnumSource(value = Rol.class, names = {"ADMIN", "ASESOR"})
    @DisplayName("cuando el personal interno consulta cualquier documento, debe devolverlo")
    void cuandoPersonalInternoConsulta_debeDevolverlo(Rol actor) {
        actuaComo(actor, "9999999");
        when(usuarioRepository.buscarPorDocumentoIdentidad("1020304050")).thenReturn(Mono.just(USUARIO));

        StepVerifier.create(useCase.porDocumentoIdentidad("1020304050")).expectNext(USUARIO).verifyComplete();
    }

    @Test
    @DisplayName("cuando no hay usuario autenticado, debe denegar")
    void cuandoSinUsuarioAutenticado_debeDenegar() {
        when(contextoSeguridad.usuarioActual()).thenReturn(Mono.empty());

        StepVerifier.create(useCase.porDocumentoIdentidad("1020304050"))
                .expectError(AccesoDenegadoException.class)
                .verify();

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("cuando el usuario no existe, debe fallar con UsuarioNoEncontradoException")
    void cuandoNoExiste_debeFallar() {
        actuaComo(Rol.ADMIN, "9999999");
        when(usuarioRepository.buscarPorDocumentoIdentidad("0000000")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.porDocumentoIdentidad("0000000"))
                .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(UsuarioNoEncontradoException.class))
                .verify();
    }
}
