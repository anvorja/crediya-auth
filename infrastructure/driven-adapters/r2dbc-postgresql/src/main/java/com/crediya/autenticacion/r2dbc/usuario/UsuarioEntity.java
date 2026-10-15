package com.crediya.autenticacion.r2dbc.usuario;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Table("usuario")
public record UsuarioEntity(
        @Id @Column("id_usuario") UUID id,
        @Column("nombre") String nombres,
        @Column("apellido") String apellidos,
        @Column("documento_identidad") String documentoIdentidad,
        @Column("fecha_nacimiento") LocalDate fechaNacimiento,
        @Column("direccion") String direccion,
        @Column("telefono") String telefono,
        @Column("email") String email,
        @Column("salario_base") BigDecimal salarioBase,
        @Column("id_rol") Short idRol
) {
}
