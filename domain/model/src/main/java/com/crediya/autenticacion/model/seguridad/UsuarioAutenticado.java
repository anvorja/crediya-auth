package com.crediya.autenticacion.model.seguridad;

import com.crediya.autenticacion.model.usuario.Rol;

import java.util.UUID;

/**
 * Quien esta invocando la operacion, segun el token validado en el entry point.
 */
public record UsuarioAutenticado(UUID id, String documentoIdentidad, String email, Rol rol) {
}
