package com.crediya.autenticacion.model.usuario.gateways;

import com.crediya.autenticacion.model.usuario.Rol;
import com.crediya.autenticacion.model.usuario.Usuario;
import reactor.core.publisher.Mono;

public interface UsuarioRepository {

    Mono<Usuario> guardar(Usuario usuario);

    Mono<Boolean> existePorEmail(String email);

    Mono<Boolean> existePorDocumentoIdentidad(String documentoIdentidad);

    Mono<Usuario> buscarPorDocumentoIdentidad(String documentoIdentidad);

    Mono<Usuario> buscarPorEmail(String email);

    Mono<Boolean> existePorRol(Rol rol);
}
