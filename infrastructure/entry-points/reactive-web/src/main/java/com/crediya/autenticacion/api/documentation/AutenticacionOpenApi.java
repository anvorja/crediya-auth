package com.crediya.autenticacion.api.documentation;

import com.crediya.autenticacion.api.dto.request.LoginRequest;
import com.crediya.autenticacion.api.dto.response.ErrorResponse;
import com.crediya.autenticacion.api.dto.response.TokenResponse;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;

public final class AutenticacionOpenApi {

    private static final String TAG = "Autenticacion";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String OK = String.valueOf(HttpStatus.OK.value());
    private static final String BAD_REQUEST = String.valueOf(HttpStatus.BAD_REQUEST.value());
    private static final String UNAUTHORIZED = String.valueOf(HttpStatus.UNAUTHORIZED.value());

    private AutenticacionOpenApi() {
    }

    public static Builder login(Builder builder) {
        return builder
                .operationId("login")
                .summary("Iniciar sesion")
                .description("HU3: autentica con correo y clave y devuelve un JWT (Bearer). Los intentos son ilimitados. "
                        + "Usuario inexistente y clave incorrecta producen la misma respuesta 401.")
                .tag(TAG)
                .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder().mediaType(JSON)
                                .schema(schemaBuilder().implementation(LoginRequest.class))))
                .response(responseBuilder().responseCode(OK).description("Sesion iniciada")
                        .content(contentBuilder().mediaType(JSON)
                                .schema(schemaBuilder().implementation(TokenResponse.class))))
                .response(error(BAD_REQUEST, "Faltan correo o clave"))
                .response(error(UNAUTHORIZED, "Correo o clave incorrectos"));
    }

    public static Builder clavesPublicas(Builder builder) {
        return builder
                .operationId("clavesPublicas")
                .summary("Claves publicas (JWKS)")
                .description("Conjunto JWK (RFC 7517) para que otros microservicios verifiquen la firma RS256 de los tokens.")
                .tag(TAG)
                .response(responseBuilder().responseCode(OK).description("Conjunto de claves publicas"));
    }

    /**
     * Un unico lugar arma las respuestas de error, siempre con el cuerpo real {@link ErrorResponse}.
     */
    private static org.springdoc.core.fn.builders.apiresponse.Builder error(String codigo, String descripcion) {
        return responseBuilder().responseCode(codigo).description(descripcion)
                .content(contentBuilder().mediaType(JSON).schema(schemaBuilder().implementation(ErrorResponse.class)));
    }
}
