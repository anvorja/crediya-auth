package com.crediya.autenticacion.usecase.consultarusuario;

import com.crediya.autenticacion.model.usuario.Rol;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioNoEncontradoException;
import com.crediya.autenticacion.model.usuario.gateways.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarUsuarioUseCase (HU2)")
class ConsultarUsuarioUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ConsultarUsuarioUseCase useCase;

    @Test
    @DisplayName("cuando el usuario existe, debe devolverlo")
    void cuandoExiste_debeDevolverlo() {
        var usuario = new Usuario(UUID.randomUUID(), "Maria", "Gomez", "1020304050", null, null, null,
                "maria@example.com", new BigDecimal("4500000"), Rol.CLIENTE);
        when(usuarioRepository.buscarPorDocumentoIdentidad("1020304050")).thenReturn(Mono.just(usuario));

        StepVerifier.create(useCase.porDocumentoIdentidad("1020304050"))
                .expectNext(usuario)
                .verifyComplete();
    }

    @Test
    @DisplayName("cuando no existe, debe fallar con UsuarioNoEncontradoException")
    void cuandoNoExiste_debeFallar() {
        when(usuarioRepository.buscarPorDocumentoIdentidad("0000000")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.porDocumentoIdentidad("0000000"))
                .expectErrorSatisfies(error -> {
                    org.assertj.core.api.Assertions.assertThat(error).isInstanceOf(UsuarioNoEncontradoException.class);
                    org.assertj.core.api.Assertions.assertThat(((UsuarioNoEncontradoException) error).getCode())
                            .isEqualTo(UsuarioNoEncontradoException.CODE);
                })
                .verify();
    }
}
