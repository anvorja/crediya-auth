package com.crediya.autenticacion.model.usuario.exceptions;

import com.crediya.autenticacion.model.common.exceptions.DomainException;

public class UsuarioNoEncontradoException extends DomainException {

    public static final String CODE = "USUARIO_NO_ENCONTRADO";

    public UsuarioNoEncontradoException() {
        super(CODE, "No existe un usuario con el documento de identidad indicado");
    }
}
