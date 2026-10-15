package com.crediya.autenticacion.model.seguridad.gateways;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface ClavesPublicasGateway {

    /**
     * Conjunto JWK (RFC 7517) con las claves publicas que verifican los tokens emitidos.
     */
    Mono<Map<String, Object>> conjuntoJwk();
}
