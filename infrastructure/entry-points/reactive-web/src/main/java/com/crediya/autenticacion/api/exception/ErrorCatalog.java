package com.crediya.autenticacion.api.exception;

import com.crediya.autenticacion.model.usuario.exceptions.DatosUsuarioInvalidosException;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioDuplicadoException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Traduce los codigos de error del dominio a estados HTTP. Es el unico lugar que conoce ambos mundos.
 */
public final class ErrorCatalog {

    public static final String CODIGO_VALIDACION = "VALIDACION_FALLIDA";
    public static final String CODIGO_PETICION_INVALIDA = "PETICION_INVALIDA";
    public static final String CODIGO_ERROR_INTERNO = "ERROR_INTERNO";
    public static final String MENSAJE_ERROR_INTERNO = "Ocurrio un error interno. Intenta de nuevo mas tarde.";

    private static final Map<String, HttpStatus> ESTADOS_DOMINIO = Map.of(
            DatosUsuarioInvalidosException.CODE, HttpStatus.BAD_REQUEST,
            UsuarioDuplicadoException.CODE, HttpStatus.CONFLICT
    );

    private ErrorCatalog() {
    }

    public static HttpStatus estadoDe(String codigoDominio) {
        return ESTADOS_DOMINIO.getOrDefault(codigoDominio, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
