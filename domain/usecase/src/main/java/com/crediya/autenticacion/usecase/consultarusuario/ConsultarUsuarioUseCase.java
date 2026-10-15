package com.crediya.autenticacion.usecase.consultarusuario;

import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioNoEncontradoException;
import com.crediya.autenticacion.model.usuario.gateways.UsuarioRepository;
import reactor.core.publisher.Mono;

/**
 * HU2: permite a otros servicios (SOLICITUDES) verificar que un cliente existe y obtener sus datos.
 */
public class ConsultarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;

    public ConsultarUsuarioUseCase(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Mono<Usuario> porDocumentoIdentidad(String documentoIdentidad) {
        return usuarioRepository.buscarPorDocumentoIdentidad(documentoIdentidad)
                .switchIfEmpty(Mono.error(UsuarioNoEncontradoException::new));
    }
}
