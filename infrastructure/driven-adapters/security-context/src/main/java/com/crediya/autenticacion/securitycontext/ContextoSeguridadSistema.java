package com.crediya.autenticacion.securitycontext;

import com.crediya.autenticacion.model.seguridad.UsuarioAutenticado;
import com.crediya.autenticacion.model.usuario.Rol;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Permite ejecutar una operacion interna (p. ej. crear el primer administrador al arrancar) con identidad de sistema,
 * sin pasar por HTTP. No se expone a ningun endpoint.
 */
public final class ContextoSeguridadSistema {

    private static final UsuarioAutenticado SISTEMA =
            new UsuarioAutenticado(null, "sistema", "sistema@crediya.local", Rol.ADMIN);

    private ContextoSeguridadSistema() {
    }

    public static <T> Mono<T> comoAdministradorDelSistema(Mono<T> operacion) {
        var autenticacion = new UsernamePasswordAuthenticationToken(SISTEMA, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        return operacion.contextWrite(ReactiveSecurityContextHolder.withAuthentication(autenticacion));
    }
}
