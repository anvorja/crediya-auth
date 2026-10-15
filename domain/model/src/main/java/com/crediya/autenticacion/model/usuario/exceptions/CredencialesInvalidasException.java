package com.crediya.autenticacion.model.usuario.exceptions;

import com.crediya.autenticacion.model.common.exceptions.DomainException;

/**
 * Mismo mensaje para "usuario inexistente" y "clave incorrecta": no revela que cuentas existen.
 */
public class CredencialesInvalidasException extends DomainException {

    public static final String CODE = "CREDENCIALES_INVALIDAS";

    public CredencialesInvalidasException() {
        super(CODE, "Correo o clave incorrectos");
    }
}
