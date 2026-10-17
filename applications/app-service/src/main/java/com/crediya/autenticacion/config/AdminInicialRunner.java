package com.crediya.autenticacion.config;

import com.crediya.autenticacion.model.usuario.Rol;
import com.crediya.autenticacion.model.usuario.commands.RegistrarUsuarioCommand;
import com.crediya.autenticacion.model.usuario.gateways.UsuarioRepository;
import com.crediya.autenticacion.securitycontext.ContextoSeguridadSistema;
import com.crediya.autenticacion.usecase.registrarusuario.RegistrarUsuarioUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Crea el primer administrador al arrancar, solo si BOOTSTRAP_ADMIN_PASSWORD esta definida y no existe ningun ADMIN.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${bootstrap.admin.password:}'.isBlank()")
public class AdminInicialRunner implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final AdminInicialProperties propiedades;

    @Override
    public void run(ApplicationArguments args) {
        usuarioRepository.existePorRol(Rol.ADMIN)
                .flatMap(existe -> Boolean.TRUE.equals(existe) ? Mono.fromRunnable(this::avisarQueYaExiste) : crearAdministrador())
                .block();
    }

    private Mono<Void> crearAdministrador() {
        var comando = new RegistrarUsuarioCommand(propiedades.nombres(), propiedades.apellidos(), propiedades.documento(),
                null, null, null, propiedades.email(), BigDecimal.ZERO, Rol.ADMIN.name(), propiedades.password());
        return ContextoSeguridadSistema.comoAdministradorDelSistema(registrarUsuarioUseCase.ejecutar(comando))
                .doOnNext(admin -> log.info("Administrador inicial creado"))
                .then();
    }

    private void avisarQueYaExiste() {
        log.info("Ya existe un administrador: no se crea el administrador inicial");
    }
}
