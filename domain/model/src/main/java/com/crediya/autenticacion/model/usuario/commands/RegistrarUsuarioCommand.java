package com.crediya.autenticacion.model.usuario.commands;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos crudos para registrar un usuario. Las reglas se validan en {@code Usuario.registrar}.
 */
public record RegistrarUsuarioCommand(
        String nombres,
        String apellidos,
        String documentoIdentidad,
        LocalDate fechaNacimiento,
        String direccion,
        String telefono,
        String email,
        BigDecimal salarioBase,
        String rol,
        String password
) {

    /**
     * Nunca imprime la clave: este comando puede terminar en un log por accidente.
     */
    @Override
    public String toString() {
        return "RegistrarUsuarioCommand[email=" + email + ", documentoIdentidad=" + documentoIdentidad
                + ", rol=" + rol + ", password=****]";
    }
}
