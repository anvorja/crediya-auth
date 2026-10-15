package com.crediya.autenticacion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bootstrap.admin")
public record AdminInicialProperties(String email, String password, String documento, String nombres, String apellidos) {

    /**
     * Nunca imprime la clave.
     */
    @Override
    public String toString() {
        return "AdminInicialProperties[email=" + email + ", documento=" + documento + ", password=****]";
    }
}
