package com.crediya.autenticacion.api.documentation;

import com.crediya.autenticacion.api.dto.request.RegistrarUsuarioRequest;
import com.crediya.autenticacion.api.dto.response.ErrorResponse;
import com.crediya.autenticacion.api.dto.response.UsuarioResponse;
import com.crediya.autenticacion.api.config.OpenApiConfig;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.http.MediaType;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.securityrequirement.Builder.securityRequirementBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;

/**
 * Documentacion OpenAPI separada de la logica: se pasa como parametro al router.
 */
public final class UsuarioOpenApi {

    private static final String TAG = "Usuarios";
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String CREATED = String.valueOf(HttpStatus.CREATED.value());
    private static final String BAD_REQUEST = String.valueOf(HttpStatus.BAD_REQUEST.value());
    private static final String OK = String.valueOf(HttpStatus.OK.value());
    private static final String UNAUTHORIZED = String.valueOf(HttpStatus.UNAUTHORIZED.value());
    private static final String FORBIDDEN = String.valueOf(HttpStatus.FORBIDDEN.value());
    private static final String NOT_FOUND = String.valueOf(HttpStatus.NOT_FOUND.value());
    private static final String CONFLICT = String.valueOf(HttpStatus.CONFLICT.value());
    private static final String SERVER_ERROR = String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());

    private UsuarioOpenApi() {
    }

    public static Builder registrarUsuario(Builder builder) {
        return builder
                .operationId("registrarUsuario")
                .summary("Registrar un usuario")
                .description("HU1 + HU3: registra un nuevo usuario validando formato de datos, salario entre 0 y 15000000 "
                        + "y unicidad de correo electronico y documento. Requiere token de ADMIN o ASESOR; "
                        + "solo un ADMIN puede crear ADMIN o ASESOR.")
                .tag(TAG)
                .security(securityRequirementBuilder().name(OpenApiConfig.ESQUEMA_BEARER))
                .response(responseBuilder().responseCode(UNAUTHORIZED).description("Falta el token o no es valido")
                        .content(contentBuilder().mediaType(JSON).schema(schemaBuilder().implementation(ErrorResponse.class))))
                .response(responseBuilder().responseCode(FORBIDDEN).description("El rol no permite esta accion")
                        .content(contentBuilder().mediaType(JSON).schema(schemaBuilder().implementation(ErrorResponse.class))))
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

    public static Builder consultarPorDocumento(Builder builder) {
        return builder
                .operationId("consultarUsuarioPorDocumento")
                .summary("Consultar un usuario por documento de identidad")
                .description("HU2 + HU3: lo usa SOLICITUDES para validar al cliente. Un CLIENTE solo puede consultar "
                        + "sus propios datos; ADMIN y ASESOR pueden consultar cualquiera.")
                .tag(TAG)
                .security(securityRequirementBuilder().name(OpenApiConfig.ESQUEMA_BEARER))
                .response(responseBuilder().responseCode(UNAUTHORIZED).description("Falta el token o no es valido")
                        .content(contentBuilder().mediaType(JSON).schema(schemaBuilder().implementation(ErrorResponse.class))))
                .response(responseBuilder().responseCode(FORBIDDEN).description("Un cliente no puede consultar a otra persona")
                        .content(contentBuilder().mediaType(JSON).schema(schemaBuilder().implementation(ErrorResponse.class))))
                .parameter(parameterBuilder()
                        .name("documento")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .description("Documento de identidad del usuario")
                        .example("1020304050")
                        .schema(schemaBuilder().implementation(String.class)))
                .response(responseBuilder().responseCode(OK).description("Usuario encontrado")
                        .content(contentBuilder().mediaType(JSON)
                                .schema(schemaBuilder().implementation(UsuarioResponse.class))))
                .response(responseBuilder().responseCode(NOT_FOUND).description("Usuario inexistente")
                        .content(contentBuilder().mediaType(JSON)
                                .schema(schemaBuilder().implementation(ErrorResponse.class))))
                .response(responseBuilder().responseCode(SERVER_ERROR).description("Error interno")
                        .content(contentBuilder().mediaType(JSON)
                                .schema(schemaBuilder().implementation(ErrorResponse.class))));
    }
}
