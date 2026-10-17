package com.crediya.autenticacion.r2dbc.usuario;

import com.crediya.autenticacion.model.usuario.Rol;

/**
 * Traduce el rol del dominio al identificador sembrado en la tabla {@code rol}.
 */
final class RolCatalogo {

    private RolCatalogo() {
    }

    static short idDe(Rol rol) {
        return switch (rol) {
            case ADMIN -> 1;
            case ASESOR -> 2;
            case CLIENTE -> 3;
        };
    }

    static Rol rolDe(short id) {
        return switch (id) {
            case 1 -> Rol.ADMIN;
            case 2 -> Rol.ASESOR;
            case 3 -> Rol.CLIENTE;
            default -> throw new IllegalStateException("Rol desconocido en base de datos: " + id);
        };
    }
}
