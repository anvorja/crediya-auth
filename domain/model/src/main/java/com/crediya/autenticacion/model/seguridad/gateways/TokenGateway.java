package com.crediya.autenticacion.model.seguridad.gateways;

import com.crediya.autenticacion.model.seguridad.Token;
import com.crediya.autenticacion.model.usuario.Usuario;
import reactor.core.publisher.Mono;

public interface TokenGateway {

    Mono<Token> emitir(Usuario usuario);
}
