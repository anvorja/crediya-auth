package com.crediya.autenticacion.model.usuario.exceptions;

import com.crediya.autenticacion.model.common.exceptions.DomainException;

public class DatosUsuarioInvalidosException extends DomainException {

    public static final String CODE = "DATOS_USUARIO_INVALIDOS";

    private final String campo;

    public DatosUsuarioInvalidosException(String campo, String motivo) {
        super(CODE, "El campo '" + campo + "' " + motivo);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
