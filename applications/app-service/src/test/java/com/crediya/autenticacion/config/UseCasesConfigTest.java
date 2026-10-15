package com.crediya.autenticacion.config;

import com.crediya.autenticacion.model.common.gateways.TransaccionGateway;
import com.crediya.autenticacion.model.seguridad.gateways.ClavesPublicasGateway;
import com.crediya.autenticacion.model.seguridad.gateways.ContextoSeguridadGateway;
import com.crediya.autenticacion.model.seguridad.gateways.PasswordEncoderGateway;
import com.crediya.autenticacion.model.seguridad.gateways.TokenGateway;
import com.crediya.autenticacion.model.usuario.gateways.UsuarioRepository;
import com.crediya.autenticacion.usecase.clavespublicas.ObtenerClavesPublicasUseCase;
import com.crediya.autenticacion.usecase.consultarusuario.ConsultarUsuarioUseCase;
import com.crediya.autenticacion.usecase.login.LoginUseCase;
import com.crediya.autenticacion.usecase.registrarusuario.RegistrarUsuarioUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UseCasesConfig: registro automatico de casos de uso")
class UseCasesConfigTest {

    @Test
    @DisplayName("debe registrar como bean cada clase *UseCase con sus puertos inyectados")
    void debeRegistrarCasosDeUso() {
        try (var contexto = new AnnotationConfigApplicationContext(PuertosSimulados.class)) {
            assertThat(contexto.getBeansOfType(RegistrarUsuarioUseCase.class)).hasSize(1);
            assertThat(contexto.getBeansOfType(ConsultarUsuarioUseCase.class)).hasSize(1);
            assertThat(contexto.getBeansOfType(LoginUseCase.class)).hasSize(1);
            assertThat(contexto.getBeansOfType(ObtenerClavesPublicasUseCase.class)).hasSize(1);
        }
    }

    @Configuration
    @Import({UseCasesConfig.class, ClockConfig.class})
    static class PuertosSimulados {

        @Bean
        UsuarioRepository usuarioRepository() {
            return Mockito.mock(UsuarioRepository.class);
        }

        @Bean
        TransaccionGateway transaccionGateway() {
            return Mockito.mock(TransaccionGateway.class);
        }

        @Bean
        PasswordEncoderGateway passwordEncoderGateway() {
            return Mockito.mock(PasswordEncoderGateway.class);
        }

        @Bean
        ContextoSeguridadGateway contextoSeguridadGateway() {
            return Mockito.mock(ContextoSeguridadGateway.class);
        }

        @Bean
        TokenGateway tokenGateway() {
            return Mockito.mock(TokenGateway.class);
        }

        @Bean
        ClavesPublicasGateway clavesPublicasGateway() {
            return Mockito.mock(ClavesPublicasGateway.class);
        }
    }

    @Test
    @DisplayName("el reloj de la aplicacion debe usar UTC")
    void relojDebeSerUtc() {
        assertThat(new ClockConfig().clock()).isEqualTo(Clock.systemUTC());
    }
}
