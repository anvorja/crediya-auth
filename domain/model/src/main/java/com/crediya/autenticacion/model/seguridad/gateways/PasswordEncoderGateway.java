package com.crediya.autenticacion.model.seguridad.gateways;

import reactor.core.publisher.Mono;

public interface PasswordEncoderGateway {

    Mono<String> codificar(String passwordPlano);

    Mono<Boolean> coincide(String passwordPlano, String hash);
}
