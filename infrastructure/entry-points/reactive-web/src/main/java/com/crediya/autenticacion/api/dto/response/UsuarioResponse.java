package com.crediya.autenticacion.api.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UsuarioResponse(
        UUID id,
        String nombres,
        String apellidos,
        String documentoIdentidad,
        LocalDate fechaNacimiento,
        String direccion,
        String telefono,
        String correoElectronico,
        BigDecimal salarioBase,
        String rol
) {
}
