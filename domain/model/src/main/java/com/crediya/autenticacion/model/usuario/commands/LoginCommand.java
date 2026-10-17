package com.crediya.autenticacion.model.usuario.commands;

public record LoginCommand(String email, String password) {

    /**
     * Nunca imprime la clave.
     */
    @Override
    public String toString() {
        return "LoginCommand[email=" + email + ", password=****]";
    }
}
