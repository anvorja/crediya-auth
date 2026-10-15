package com.crediya.autenticacion.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de los tokens. {@code privateKey} es una clave RSA PKCS8 (PEM o su base64) y es opcional:
 * si no se informa se genera una efimera al arrancar (los tokens dejan de ser validos al reiniciar y no sirve con
 * varias replicas). En produccion debe venir de un gestor de secretos.
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String issuer, String audience, Long ttlMinutes, String privateKey) {

    public static final String ISSUER_POR_DEFECTO = "crediya-auth";
    public static final String AUDIENCE_POR_DEFECTO = "crediya";
    public static final long TTL_MINUTOS_POR_DEFECTO = 60;

    public JwtProperties {
        issuer = issuer == null || issuer.isBlank() ? ISSUER_POR_DEFECTO : issuer;
        audience = audience == null || audience.isBlank() ? AUDIENCE_POR_DEFECTO : audience;
        ttlMinutes = ttlMinutes == null || ttlMinutes <= 0 ? TTL_MINUTOS_POR_DEFECTO : ttlMinutes;
    }

    /**
     * Nunca imprime la clave privada.
     */
    @Override
    public String toString() {
        return "JwtProperties[issuer=" + issuer + ", audience=" + audience + ", ttlMinutes=" + ttlMinutes
                + ", privateKey=" + (privateKey == null || privateKey.isBlank() ? "(efimera)" : "****") + "]";
    }
}
