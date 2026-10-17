package com.crediya.autenticacion.usecase.clavespublicas;

import com.crediya.autenticacion.model.seguridad.gateways.ClavesPublicasGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ObtenerClavesPublicasUseCase (HU3)")
class ObtenerClavesPublicasUseCaseTest {

    @Mock
    private ClavesPublicasGateway clavesPublicasGateway;

    @InjectMocks
    private ObtenerClavesPublicasUseCase useCase;

    @Test
    @DisplayName("debe devolver el conjunto JWK que publica el puerto")
    void debeDevolverConjuntoJwk() {
        Map<String, Object> jwks = Map.of("keys", List.of(Map.of("kty", "RSA", "kid", "k1")));
        when(clavesPublicasGateway.conjuntoJwk()).thenReturn(Mono.just(jwks));

        StepVerifier.create(useCase.ejecutar()).expectNext(jwks).verifyComplete();
    }
}
