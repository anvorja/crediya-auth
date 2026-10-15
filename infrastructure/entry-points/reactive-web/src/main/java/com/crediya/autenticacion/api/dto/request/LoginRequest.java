package com.crediya.autenticacion.api.dto.request;

import com.crediya.autenticacion.api.constants.ValidationMessages;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LoginRequest(
        @NotBlank(message = ValidationMessages.EMAIL_REQUERIDO) String correoElectronico,
        @NotBlank(message = ValidationMessages.PASSWORD_REQUERIDA) String password
) {

    /**
     * Nunca imprime la clave.
     */
    @Override
    public String toString() {
        return "LoginRequest[correoElectronico=" + correoElectronico + ", password=****]";
    }
}
