package com.crediya.autenticacion.api.usuario;

import com.crediya.autenticacion.api.constants.ValidationMessages;
import com.crediya.autenticacion.api.dto.request.RegistrarUsuarioRequest;
import com.crediya.autenticacion.api.mapper.UsuarioRestMapper;
import com.crediya.autenticacion.api.validation.RequestValidationException;
import com.crediya.autenticacion.api.validation.RequestValidator;
import com.crediya.autenticacion.usecase.consultarusuario.ConsultarUsuarioUseCase;
import com.crediya.autenticacion.usecase.consultarusuarios.ConsultarUsuariosPorCorreoUseCase;
import com.crediya.autenticacion.usecase.registrarusuario.RegistrarUsuarioUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioHandler {

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final ConsultarUsuarioUseCase consultarUsuarioUseCase;
    private final ConsultarUsuariosPorCorreoUseCase consultarUsuariosPorCorreoUseCase;
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

    /**
     * GET /api/v1/usuarios/{documento} (HU2): usado por SOLICITUDES para validar al cliente.
     */
    public Mono<ServerResponse> consultarPorDocumento(ServerRequest request) {
        return Mono.fromSupplier(() -> request.pathVariable("documento"))
                .doOnNext(documento -> log.info("Consulta de usuario por documento de identidad"))
                .flatMap(consultarUsuarioUseCase::porDocumentoIdentidad)
                .map(mapper::toResponse)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    /**
     * GET /api/v1/usuarios?correos=a@x.co,b@y.co (HU4): lo usa SOLICITUDES para armar el listado de revision.
     */
    public Mono<ServerResponse> consultarPorCorreos(ServerRequest request) {
        return Mono.fromSupplier(() -> correosDeLaConsulta(request))
                .doOnNext(correos -> log.info("Consulta de usuarios por correo ({} correos)", correos.size()))
                .flatMapMany(consultarUsuariosPorCorreoUseCase::ejecutar)
                .map(mapper::toResponse)
                .collectList()
                .flatMap(usuarios -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(usuarios));
    }

    private static List<String> correosDeLaConsulta(ServerRequest request) {
        List<String> correos = request.queryParams().getOrDefault(ValidationMessages.CAMPO_CORREOS, List.of()).stream()
                .flatMap(valor -> Arrays.stream(valor.split(",")))
                .map(String::trim)
                .filter(correo -> !correo.isEmpty())
                .toList();
        if (correos.isEmpty()) {
            throw new RequestValidationException(
                    Map.of(ValidationMessages.CAMPO_CORREOS, ValidationMessages.CORREOS_REQUERIDOS));
        }
        return correos;
    }
}
