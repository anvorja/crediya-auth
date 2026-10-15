package com.crediya.autenticacion.model.seguridad;

/**
 * Token de acceso emitido tras un login correcto.
 */
public record Token(String valor, String tipo, long expiraEnSegundos) {

    public static final String TIPO_BEARER = "Bearer";

    public static Token bearer(String valor, long expiraEnSegundos) {
        return new Token(valor, TIPO_BEARER, expiraEnSegundos);
    }

    /**
     * Nunca imprime el token.
     */
    @Override
    public String toString() {
        return "Token[tipo=" + tipo + ", expiraEnSegundos=" + expiraEnSegundos + "]";
    }
}
