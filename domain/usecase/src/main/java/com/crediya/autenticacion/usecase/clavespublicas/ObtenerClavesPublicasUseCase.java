package com.crediya.autenticacion.usecase.clavespublicas;

import com.crediya.autenticacion.model.seguridad.gateways.ClavesPublicasGateway;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * HU3: publica las claves con las que otros servicios verifican los tokens (JWKS), sin compartir secretos.
 */
public class ObtenerClavesPublicasUseCase {

    private final ClavesPublicasGateway clavesPublicasGateway;

    public ObtenerClavesPublicasUseCase(ClavesPublicasGateway clavesPublicasGateway) {
        this.clavesPublicasGateway = clavesPublicasGateway;
    }

    public Mono<Map<String, Object>> ejecutar() {
        return clavesPublicasGateway.conjuntoJwk();
    }
}
