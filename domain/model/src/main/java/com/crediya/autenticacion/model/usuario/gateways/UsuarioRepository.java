package com.crediya.autenticacion.model.usuario.gateways;

import com.crediya.autenticacion.model.usuario.Rol;
import com.crediya.autenticacion.model.usuario.Usuario;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface UsuarioRepository {

    Mono<Usuario> guardar(Usuario usuario);

    Mono<Boolean> existePorEmail(String email);

    Mono<Boolean> existePorDocumentoIdentidad(String documentoIdentidad);

    Mono<Usuario> buscarPorDocumentoIdentidad(String documentoIdentidad);

    Mono<Usuario> buscarPorEmail(String email);

    /**
     * Busca varios usuarios por correo (en minusculas) en una sola consulta.
     */
    Flux<Usuario> buscarPorEmails(Collection<String> emails);

    Mono<Boolean> existePorRol(Rol rol);
}
