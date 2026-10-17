package com.crediya.autenticacion.model.seguridad.gateways;

import com.crediya.autenticacion.model.seguridad.UsuarioAutenticado;
import reactor.core.publisher.Mono;

public interface ContextoSeguridadGateway {

    /**
     * Usuario autenticado de la peticion actual; Mono vacio si no hay ninguno.
     */
    Mono<UsuarioAutenticado> usuarioActual();
}
