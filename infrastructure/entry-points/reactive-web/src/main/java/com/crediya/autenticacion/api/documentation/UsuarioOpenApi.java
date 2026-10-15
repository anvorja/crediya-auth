package com.crediya.autenticacion.api.documentation;

import com.crediya.autenticacion.api.dto.request.RegistrarUsuarioRequest;
import com.crediya.autenticacion.api.dto.response.ErrorResponse;
import com.crediya.autenticacion.api.dto.response.UsuarioResponse;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;

/**
 * Documentacion OpenAPI separada de la logica: se pasa como parametro al router.
 */
public final class UsuarioOpenApi {

    private static final String TAG = "Usuarios";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String CREATED = String.valueOf(HttpStatus.CREATED.value());
    private static final String BAD_REQUEST = String.valueOf(HttpStatus.BAD_REQUEST.value());
    private static final String CONFLICT = String.valueOf(HttpStatus.CONFLICT.value());
    private static final String SERVER_ERROR = String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());

    private UsuarioOpenApi() {
    }

    public static Builder registrarUsuario(Builder builder) {
        return builder
                .operationId("registrarUsuario")
                .summary("Registrar un usuario")
                .description("HU1: registra un nuevo usuario validando formato de datos, salario entre 0 y 15000000 "
                        + "y unicidad de correo electronico y documento de identidad.")
                .tag(TAG)
                .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder().mediaType(JSON)
                                .schema(schemaBuilder().implementation(RegistrarUsuarioRequest.class))))
                .response(responseBuilder().responseCode(CREATED).description("Usuario registrado")
                        .content(contentBuilder().mediaType(JSON)
                                .schema(schemaBuilder().implementation(UsuarioResponse.class))))
                .response(responseBuilder().responseCode(BAD_REQUEST).description("Datos invalidos")
                        .content(contentBuilder().mediaType(JSON)
                                .schema(schemaBuilder().implementation(ErrorResponse.class))))
                .response(responseBuilder().responseCode(CONFLICT).description("Correo o documento ya registrado")
                        .content(contentBuilder().mediaType(JSON)
                                .schema(schemaBuilder().implementation(ErrorResponse.class))))
                .response(responseBuilder().responseCode(SERVER_ERROR).description("Error interno")
                        .content(contentBuilder().mediaType(JSON)
                                .schema(schemaBuilder().implementation(ErrorResponse.class))));
    }
}
