package com.crediya.autenticacion.api.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Respuesta de login con la forma habitual de OAuth2: access_token, token_type y expires_in (segundos).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    /**
     * Nunca imprime el token.
     */
    @Override
    public String toString() {
        return "TokenResponse[tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}
