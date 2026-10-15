package com.crediya.autenticacion.model.usuario;

import com.crediya.autenticacion.model.usuario.commands.RegistrarUsuarioCommand;
import com.crediya.autenticacion.model.usuario.exceptions.DatosUsuarioInvalidosException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;

public record Usuario(
        UUID id,
        String nombres,
        String apellidos,
        String documentoIdentidad,
        LocalDate fechaNacimiento,
        String direccion,
        String telefono,
        String email,
        BigDecimal salarioBase,
        Rol rol
) {

    public static final BigDecimal SALARIO_MINIMO = BigDecimal.ZERO;
    public static final BigDecimal SALARIO_MAXIMO = new BigDecimal("15000000");
    public static final int NOMBRE_LONGITUD_MAXIMA = 100;
    public static final int EMAIL_LONGITUD_MAXIMA = 255;
    public static final int DIRECCION_LONGITUD_MAXIMA = 255;

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern DOCUMENTO = Pattern.compile("^[0-9]{5,20}$");
    private static final Pattern TELEFONO = Pattern.compile("^\\+?[0-9]{7,15}$");

    /**
     * Crea un usuario nuevo (sin id, lo asigna la persistencia) aplicando las reglas de la HU1.
     */
    public static Usuario registrar(RegistrarUsuarioCommand comando, Clock reloj) {
        String nombres = textoObligatorio("nombres", comando.nombres(), NOMBRE_LONGITUD_MAXIMA);
        String apellidos = textoObligatorio("apellidos", comando.apellidos(), NOMBRE_LONGITUD_MAXIMA);
        String email = email(comando.email());
        String documento = documento(comando.documentoIdentidad());
        BigDecimal salario = salario(comando.salarioBase());

        return new Usuario(
                null,
                nombres,
                apellidos,
                documento,
                fechaNacimiento(comando.fechaNacimiento(), reloj),
                textoOpcional("direccion", comando.direccion(), DIRECCION_LONGITUD_MAXIMA),
                telefono(comando.telefono()),
                email,
                salario,
                Rol.desde(comando.rol())
        );
    }

    public Usuario conId(UUID nuevoId) {
        return new Usuario(nuevoId, nombres, apellidos, documentoIdentidad, fechaNacimiento,
                direccion, telefono, email, salarioBase, rol);
    }

    private static String textoObligatorio(String campo, String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new DatosUsuarioInvalidosException(campo, "es obligatorio");
        }
        String limpio = valor.trim();
        if (limpio.length() > maximo) {
            throw new DatosUsuarioInvalidosException(campo, "no puede superar " + maximo + " caracteres");
        }
        return limpio;
    }

    private static String textoOpcional(String campo, String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return textoObligatorio(campo, valor, maximo);
    }

    private static String email(String valor) {
        String limpio = textoObligatorio("correo_electronico", valor, EMAIL_LONGITUD_MAXIMA);
        if (!EMAIL.matcher(limpio).matches()) {
            throw new DatosUsuarioInvalidosException("correo_electronico", "no tiene un formato de email valido");
        }
        return limpio.toLowerCase();
    }

    private static String documento(String valor) {
        String limpio = textoObligatorio("documento_identidad", valor, 20);
        if (!DOCUMENTO.matcher(limpio).matches()) {
            throw new DatosUsuarioInvalidosException("documento_identidad", "debe tener entre 5 y 20 digitos");
        }
        return limpio;
    }

    private static String telefono(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        if (!TELEFONO.matcher(limpio).matches()) {
            throw new DatosUsuarioInvalidosException("telefono", "debe tener entre 7 y 15 digitos");
        }
        return limpio;
    }

    private static BigDecimal salario(BigDecimal valor) {
        if (valor == null) {
            throw new DatosUsuarioInvalidosException("salario_base", "es obligatorio");
        }
        if (valor.compareTo(SALARIO_MINIMO) < 0 || valor.compareTo(SALARIO_MAXIMO) > 0) {
            throw new DatosUsuarioInvalidosException("salario_base",
                    "debe estar entre " + SALARIO_MINIMO + " y " + SALARIO_MAXIMO.toPlainString());
        }
        return valor;
    }

    private static LocalDate fechaNacimiento(LocalDate valor, Clock reloj) {
        if (valor != null && !valor.isBefore(LocalDate.now(reloj))) {
            throw new DatosUsuarioInvalidosException("fecha_nacimiento", "debe ser una fecha pasada");
        }
        return valor;
    }
}
