package com.crediya.autenticacion.api.dto.request;

import com.crediya.autenticacion.api.constants.ValidationMessages;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RegistrarUsuarioRequest(
        @NotBlank(message = ValidationMessages.NOMBRES_REQUERIDOS) String nombres,
        @NotBlank(message = ValidationMessages.APELLIDOS_REQUERIDOS) String apellidos,
        @NotBlank(message = ValidationMessages.DOCUMENTO_REQUERIDO) String documentoIdentidad,
        LocalDate fechaNacimiento,
        String direccion,
        String telefono,
        @NotBlank(message = ValidationMessages.EMAIL_REQUERIDO)
        @Email(message = ValidationMessages.EMAIL_FORMATO) String correoElectronico,
        @NotNull(message = ValidationMessages.SALARIO_REQUERIDO)
        @DecimalMin(value = "0", message = ValidationMessages.SALARIO_MINIMO)
        @DecimalMax(value = "15000000", message = ValidationMessages.SALARIO_MAXIMO) BigDecimal salarioBase,
        String rol
) {
}
