package com.crediya.autenticacion.usecase.consultarusuario;

import com.crediya.autenticacion.model.common.exceptions.AccesoDenegadoException;
import com.crediya.autenticacion.model.seguridad.UsuarioAutenticado;
import com.crediya.autenticacion.model.seguridad.gateways.ContextoSeguridadGateway;
import com.crediya.autenticacion.model.usuario.Rol;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioNoEncontradoException;
import com.crediya.autenticacion.model.usuario.gateways.UsuarioRepository;
import reactor.core.publisher.Mono;

/**
 * HU2 + HU3: permite a otros servicios (SOLICITUDES) obtener los datos de un usuario. Un CLIENTE solo puede
 * consultar sus propios datos; el personal interno puede consultar cualquiera.
 */
public class ConsultarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final ContextoSeguridadGateway contextoSeguridad;

    public ConsultarUsuarioUseCase(UsuarioRepository usuarioRepository, ContextoSeguridadGateway contextoSeguridad) {
        this.usuarioRepository = usuarioRepository;
        this.contextoSeguridad = contextoSeguridad;
    }

    public Mono<Usuario> porDocumentoIdentidad(String documentoIdentidad) {
        return contextoSeguridad.usuarioActual()
                .filter(actor -> puedeConsultar(actor, documentoIdentidad))
                .switchIfEmpty(Mono.error(AccesoDenegadoException::new))
                .then(Mono.defer(() -> usuarioRepository.buscarPorDocumentoIdentidad(documentoIdentidad)))
                .switchIfEmpty(Mono.error(UsuarioNoEncontradoException::new));
    }

    private static boolean puedeConsultar(UsuarioAutenticado actor, String documentoIdentidad) {
        return actor.rol() != Rol.CLIENTE || actor.documentoIdentidad().equals(documentoIdentidad);
    }
}
