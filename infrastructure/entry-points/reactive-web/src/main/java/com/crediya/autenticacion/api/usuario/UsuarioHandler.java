package com.crediya.autenticacion.api.usuario;

import com.crediya.autenticacion.api.constants.ValidationMessages;
import com.crediya.autenticacion.api.dto.request.RegistrarUsuarioRequest;
import com.crediya.autenticacion.api.mapper.UsuarioRestMapper;
import com.crediya.autenticacion.api.validation.RequestValidationException;
import com.crediya.autenticacion.api.validation.RequestValidator;
import com.crediya.autenticacion.usecase.registrarusuario.RegistrarUsuarioUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioHandler {

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final UsuarioRestMapper mapper;
    private final RequestValidator validator;

    /**
     * POST /api/v1/usuarios (HU1).
     */
    public Mono<ServerResponse> registrarUsuario(ServerRequest request) {
        return request.bodyToMono(RegistrarUsuarioRequest.class)
                .switchIfEmpty(Mono.error(() -> new RequestValidationException(
                        Map.of(ValidationMessages.CAMPO_CUERPO, ValidationMessages.CUERPO_REQUERIDO))))
                .flatMap(validator::validar)
                .doOnNext(body -> log.info("Peticion de registro de usuario recibida"))
                .map(mapper::toCommand)
                .flatMap(registrarUsuarioUseCase::ejecutar)
                .map(mapper::toResponse)
                .flatMap(response -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .doOnSuccess(respuesta -> log.info("Usuario registrado correctamente"));
    }
}
