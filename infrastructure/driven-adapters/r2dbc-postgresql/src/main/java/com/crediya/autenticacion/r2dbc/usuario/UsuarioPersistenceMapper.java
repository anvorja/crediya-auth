package com.crediya.autenticacion.r2dbc.usuario;

import com.crediya.autenticacion.model.usuario.Usuario;

final class UsuarioPersistenceMapper {

    private UsuarioPersistenceMapper() {
    }

    static UsuarioEntity toEntity(Usuario usuario) {
        return new UsuarioEntity(
                usuario.id(),
                usuario.nombres(),
                usuario.apellidos(),
                usuario.documentoIdentidad(),
                usuario.fechaNacimiento(),
                usuario.direccion(),
                usuario.telefono(),
                usuario.email(),
                usuario.salarioBase(),
                RolCatalogo.idDe(usuario.rol()),
                usuario.passwordHash()
        );
    }

    static Usuario toDomain(UsuarioEntity entity) {
        return new Usuario(
                entity.id(),
                entity.nombres(),
                entity.apellidos(),
                entity.documentoIdentidad(),
                entity.fechaNacimiento(),
                entity.direccion(),
                entity.telefono(),
                entity.email(),
                entity.salarioBase(),
                RolCatalogo.rolDe(entity.idRol()),
                entity.passwordHash()
        );
    }
}
