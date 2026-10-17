package com.crediya.autenticacion.model.usuario;

import com.crediya.autenticacion.model.usuario.commands.RegistrarUsuarioCommand;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fixture de pruebas: un comando valido que se puede alterar campo por campo.
 */
public final class ComandoUsuarioBuilder {

    private String nombres = "  Maria Fernanda ";
    private String apellidos = " Gomez Perez ";
    private String documentoIdentidad = "1020304050";
    private LocalDate fechaNacimiento = LocalDate.of(1995, 3, 20);
    private String direccion = "Calle 10 # 20-30";
    private String telefono = "+573001234567";
    private String email = "  Maria.Gomez@Example.COM ";
    private BigDecimal salarioBase = new BigDecimal("4500000");
    private String rol;
    private String password = "Clave-Segura-123";

    private ComandoUsuarioBuilder() {
    }

    public static ComandoUsuarioBuilder valido() {
        return new ComandoUsuarioBuilder();
    }

    public ComandoUsuarioBuilder nombres(String valor) {
        this.nombres = valor;
        return this;
    }

    public ComandoUsuarioBuilder apellidos(String valor) {
        this.apellidos = valor;
        return this;
    }

    public ComandoUsuarioBuilder documentoIdentidad(String valor) {
        this.documentoIdentidad = valor;
        return this;
    }

    public ComandoUsuarioBuilder fechaNacimiento(LocalDate valor) {
        this.fechaNacimiento = valor;
        return this;
    }

    public ComandoUsuarioBuilder direccion(String valor) {
        this.direccion = valor;
        return this;
    }

    public ComandoUsuarioBuilder telefono(String valor) {
        this.telefono = valor;
        return this;
    }

    public ComandoUsuarioBuilder email(String valor) {
        this.email = valor;
        return this;
    }

    public ComandoUsuarioBuilder salarioBase(BigDecimal valor) {
        this.salarioBase = valor;
        return this;
    }

    public ComandoUsuarioBuilder rol(String valor) {
        this.rol = valor;
        return this;
    }

    public ComandoUsuarioBuilder password(String valor) {
        this.password = valor;
        return this;
    }

    public RegistrarUsuarioCommand build() {
        return new RegistrarUsuarioCommand(nombres, apellidos, documentoIdentidad, fechaNacimiento,
                direccion, telefono, email, salarioBase, rol, password);
    }
}
