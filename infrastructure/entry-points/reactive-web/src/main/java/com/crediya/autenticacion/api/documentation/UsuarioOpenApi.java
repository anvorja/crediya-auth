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
import static org.springdoc.core.fn.builders.arrayschema.Builder.arraySchemaBuilder;
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
        return protegida(builder, "El rol no permite esta accion")
                .operationId("registrarUsuario")
                .summary("Registrar un usuario")
                .description("HU1 + HU3: registra un nuevo usuario validando formato de datos, salario entre 0 y 15000000 "
                        + "y unicidad de correo electronico y documento. Requiere token de ADMIN o ASESOR; "
                        + "solo un ADMIN puede crear ADMIN o ASESOR.")
                .tag(TAG)
                .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder().mediaType(JSON)
                                .schema(schemaBuilder().implementation(RegistrarUsuarioRequest.class))))
                .response(responseBuilder().responseCode(CREATED).description("Usuario registrado")
                        .content(contentBuilder().mediaType(JSON)
                                .schema(schemaBuilder().implementation(UsuarioResponse.class))))
                .response(error(BAD_REQUEST, "Datos invalidos"))
                .response(error(CONFLICT, "Correo o documento ya registrado"))
                .response(error(SERVER_ERROR, "Error interno"));
    }

    public static Builder consultarPorDocumento(Builder builder) {
        return protegida(builder, "Un cliente no puede consultar a otra persona")
                .operationId("consultarUsuarioPorDocumento")
                .summary("Consultar un usuario por documento de identidad")
                .description("HU2 + HU3: lo usa SOLICITUDES para validar al cliente. Un CLIENTE solo puede consultar "
                        + "sus propios datos; ADMIN y ASESOR pueden consultar cualquiera.")
                .tag(TAG)
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
                .response(error(NOT_FOUND, "Usuario inexistente"))
                .response(error(SERVER_ERROR, "Error interno"));
    }

    public static Builder consultarPorCorreos(Builder builder) {
        return protegida(builder, "Solo personal interno")
                .operationId("consultarUsuariosPorCorreo")
                .summary("Consultar varios usuarios por correo")
                .description("HU4: lo usa SOLICITUDES para obtener nombre y salario de los solicitantes de un listado. "
                        + "Solo ADMIN y ASESOR. Maximo 50 correos por consulta.")
                .tag(TAG)
                .parameter(parameterBuilder()
                        .name("correos")
                        .in(ParameterIn.QUERY)
                        .required(true)
                        .description("Correos separados por coma")
                        .example("maria@example.com,pedro@example.com")
                        .schema(schemaBuilder().implementation(String.class)))
                .response(responseBuilder().responseCode(OK).description("Usuarios encontrados (puede ser una lista vacia)")
                        .content(contentBuilder().mediaType(JSON)
                                .array(arraySchemaBuilder().schema(schemaBuilder().implementation(UsuarioResponse.class)))))
                .response(error(BAD_REQUEST, "Falta el parametro o hay demasiados correos"));
    }

    /**
     * Esquema Bearer mas los 401 y 403 que puede devolver cualquier endpoint protegido.
     */
    private static Builder protegida(Builder builder, String descripcionForbidden) {
        return builder
                .security(securityRequirementBuilder().name(OpenApiConfig.ESQUEMA_BEARER))
                .response(error(UNAUTHORIZED, "Falta el token o no es valido"))
                .response(error(FORBIDDEN, descripcionForbidden));
    }

    /**
     * Un unico lugar arma las respuestas de error, siempre con el cuerpo real {@link ErrorResponse}.
     */
    private static org.springdoc.core.fn.builders.apiresponse.Builder error(String codigo, String descripcion) {
        return responseBuilder().responseCode(codigo).description(descripcion)
                .content(contentBuilder().mediaType(JSON).schema(schemaBuilder().implementation(ErrorResponse.class)));
    }
}
