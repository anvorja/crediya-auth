package com.crediya.autenticacion.api.autenticacion;

import com.crediya.autenticacion.api.constants.ValidationMessages;
import com.crediya.autenticacion.api.dto.request.LoginRequest;
import com.crediya.autenticacion.api.mapper.AutenticacionRestMapper;
import com.crediya.autenticacion.api.validation.RequestValidationException;
import com.crediya.autenticacion.api.validation.RequestValidator;
import com.crediya.autenticacion.usecase.clavespublicas.ObtenerClavesPublicasUseCase;
import com.crediya.autenticacion.usecase.login.LoginUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutenticacionHandler {

    private final LoginUseCase loginUseCase;
    private final ObtenerClavesPublicasUseCase obtenerClavesPublicasUseCase;
    private final AutenticacionRestMapper mapper;
    private final RequestValidator validator;

    /**
     * POST /api/v1/login (HU3).
     */
    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(LoginRequest.class)
                .switchIfEmpty(Mono.error(() -> new RequestValidationException(
                        Map.of(ValidationMessages.CAMPO_CUERPO, ValidationMessages.CUERPO_REQUERIDO))))
                .flatMap(validator::validar)
                .doOnNext(body -> log.info("Intento de inicio de sesion recibido"))
                .map(mapper::toCommand)
                .flatMap(loginUseCase::ejecutar)
                .map(mapper::toResponse)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .cacheControl(CacheControl.noStore())
                        .bodyValue(response))
                .doOnSuccess(respuesta -> log.info("Inicio de sesion exitoso"));
    }

    /**
     * GET /.well-known/jwks.json: claves publicas para que otros servicios verifiquen los tokens.
     */
    public Mono<ServerResponse> clavesPublicas(ServerRequest request) {
        return obtenerClavesPublicasUseCase.ejecutar()
                .flatMap(jwks -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                        .bodyValue(jwks));
    }
}
