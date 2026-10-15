package com.crediya.autenticacion.model.usuario;

import com.crediya.autenticacion.model.usuario.exceptions.DatosUsuarioInvalidosException;

import java.util.Locale;

public enum Rol {
    ADMIN("Administrador del sistema"),
    ASESOR("Asesor que revisa las solicitudes de credito"),
    CLIENTE("Solicitante de credito");

    private final String descripcion;

    Rol(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Convierte un texto en rol. Si no se informa, el usuario es CLIENTE.
     */
    public static Rol desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return CLIENTE;
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DatosUsuarioInvalidosException("rol", "debe ser ADMIN, ASESOR o CLIENTE");
        }
    }
}
