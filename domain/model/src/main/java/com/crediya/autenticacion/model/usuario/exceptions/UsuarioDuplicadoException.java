package com.crediya.autenticacion.model.usuario.exceptions;

import com.crediya.autenticacion.model.common.exceptions.DomainException;

public class UsuarioDuplicadoException extends DomainException {

    public static final String CODE = "USUARIO_DUPLICADO";

    private final String campo;

    public UsuarioDuplicadoException(String campo) {
        super(CODE, "Ya existe un usuario registrado con el mismo " + campo);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
