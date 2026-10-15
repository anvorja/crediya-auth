package com.crediya.autenticacion.model.usuario;

import com.crediya.autenticacion.model.usuario.exceptions.DatosUsuarioInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Usuario - reglas de registro (HU1)")
class UsuarioTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2025-06-15T10:00:00Z"), ZoneOffset.UTC);

    private static void assertInvalido(Runnable accion, String campo) {
        assertThatThrownBy(accion::run)
                .isInstanceOf(DatosUsuarioInvalidosException.class)
                .satisfies(e -> assertThat(((DatosUsuarioInvalidosException) e).getCampo()).isEqualTo(campo));
    }

    @Test
    @DisplayName("cuando los datos son validos, debe normalizar y dejar el usuario sin id con rol CLIENTE")
    void cuandoDatosValidos_debeNormalizarYAsignarRolCliente() {
        var comando = ComandoUsuarioBuilder.valido().build();

        var usuario = Usuario.registrar(comando, RELOJ);

        assertThat(usuario.id()).isNull();
        assertThat(usuario.nombres()).isEqualTo("Maria Fernanda");
        assertThat(usuario.apellidos()).isEqualTo("Gomez Perez");
        assertThat(usuario.email()).isEqualTo("maria.gomez@example.com");
        assertThat(usuario.documentoIdentidad()).isEqualTo("1020304050");
        assertThat(usuario.telefono()).isEqualTo("+573001234567");
        assertThat(usuario.direccion()).isEqualTo("Calle 10 # 20-30");
        assertThat(usuario.fechaNacimiento()).isEqualTo(LocalDate.of(1995, 3, 20));
        assertThat(usuario.salarioBase()).isEqualByComparingTo("4500000");
        assertThat(usuario.rol()).isEqualTo(Rol.CLIENTE);
    }

    @Test
    @DisplayName("cuando se informa un rol, debe respetarlo")
    void cuandoRolInformado_debeRespetarlo() {
        var usuario = Usuario.registrar(ComandoUsuarioBuilder.valido().rol("asesor").build(), RELOJ);

        assertThat(usuario.rol()).isEqualTo(Rol.ASESOR);
    }

    @Test
    @DisplayName("cuando faltan los datos opcionales, debe aceptarlos como nulos")
    void cuandoFaltanOpcionales_debeAceptarNulos() {
        var comando = ComandoUsuarioBuilder.valido().direccion("  ").telefono(null).fechaNacimiento(null).build();

        var usuario = Usuario.registrar(comando, RELOJ);

        assertThat(usuario.direccion()).isNull();
        assertThat(usuario.telefono()).isNull();
        assertThat(usuario.fechaNacimiento()).isNull();
    }

    @ParameterizedTest(name = "nombres = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("cuando nombres es nulo o vacio, debe rechazar")
    void cuandoNombresVacio_debeRechazar(String nombres) {
        assertInvalido(() -> Usuario.registrar(ComandoUsuarioBuilder.valido().nombres(nombres).build(), RELOJ), "nombres");
    }

    @ParameterizedTest(name = "apellidos = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("cuando apellidos es nulo o vacio, debe rechazar")
    void cuandoApellidosVacio_debeRechazar(String apellidos) {
        assertInvalido(() -> Usuario.registrar(ComandoUsuarioBuilder.valido().apellidos(apellidos).build(), RELOJ), "apellidos");
    }

    @Test
    @DisplayName("cuando nombres supera la longitud maxima, debe rechazar")
    void cuandoNombresDemasiadoLargo_debeRechazar() {
        var largo = "a".repeat(Usuario.NOMBRE_LONGITUD_MAXIMA + 1);

        assertInvalido(() -> Usuario.registrar(ComandoUsuarioBuilder.valido().nombres(largo).build(), RELOJ), "nombres");
    }

    @ParameterizedTest(name = "email = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"sin-arroba.com", "dos@@example.com", "sin@dominio", "espacio @example.com", "a@b.c"})
    @DisplayName("cuando el correo es nulo, vacio o no tiene formato valido, debe rechazar")
    void cuandoEmailInvalido_debeRechazar(String email) {
        assertInvalido(() -> Usuario.registrar(ComandoUsuarioBuilder.valido().email(email).build(), RELOJ),
                "correo_electronico");
    }

    @Test
    @DisplayName("cuando el correo supera la longitud maxima, debe rechazar")
    void cuandoEmailDemasiadoLargo_debeRechazar() {
        var largo = "a".repeat(Usuario.EMAIL_LONGITUD_MAXIMA) + "@example.com";

        assertInvalido(() -> Usuario.registrar(ComandoUsuarioBuilder.valido().email(largo).build(), RELOJ),
                "correo_electronico");
    }

    @ParameterizedTest(name = "documento = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"1234", "12345abc", "123456789012345678901"})
    @DisplayName("cuando el documento es nulo, vacio o invalido, debe rechazar")
    void cuandoDocumentoInvalido_debeRechazar(String documento) {
        assertInvalido(() -> Usuario.registrar(ComandoUsuarioBuilder.valido().documentoIdentidad(documento).build(), RELOJ),
                "documento_identidad");
    }

    @ParameterizedTest(name = "telefono = [{0}]")
    @ValueSource(strings = {"123", "abc1234567", "+5730012345678901"})
    @DisplayName("cuando el telefono tiene formato invalido, debe rechazar")
    void cuandoTelefonoInvalido_debeRechazar(String telefono) {
        assertInvalido(() -> Usuario.registrar(ComandoUsuarioBuilder.valido().telefono(telefono).build(), RELOJ), "telefono");
    }

    @Test
    @DisplayName("cuando la direccion supera la longitud maxima, debe rechazar")
    void cuandoDireccionDemasiadoLarga_debeRechazar() {
        var larga = "d".repeat(Usuario.DIRECCION_LONGITUD_MAXIMA + 1);

        assertInvalido(() -> Usuario.registrar(ComandoUsuarioBuilder.valido().direccion(larga).build(), RELOJ), "direccion");
    }

    @ParameterizedTest(name = "salario = {0}")
    @ValueSource(strings = {"0", "15000000", "0.01", "14999999.99"})
    @DisplayName("cuando el salario esta dentro de 0 y 15000000, debe aceptar")
    void cuandoSalarioEnRango_debeAceptar(String salario) {
        var usuario = Usuario.registrar(ComandoUsuarioBuilder.valido().salarioBase(new BigDecimal(salario)).build(), RELOJ);

        assertThat(usuario.salarioBase()).isEqualByComparingTo(salario);
    }

    @ParameterizedTest(name = "salario = {0}")
    @ValueSource(strings = {"-0.01", "-1", "15000000.01", "99999999"})
    @DisplayName("cuando el salario esta fuera de 0 y 15000000, debe rechazar")
    void cuandoSalarioFueraDeRango_debeRechazar(String salario) {
        assertInvalido(() -> Usuario.registrar(
                ComandoUsuarioBuilder.valido().salarioBase(new BigDecimal(salario)).build(), RELOJ), "salario_base");
    }

    @Test
    @DisplayName("cuando el salario es nulo, debe rechazar")
    void cuandoSalarioNulo_debeRechazar() {
        assertInvalido(() -> Usuario.registrar(ComandoUsuarioBuilder.valido().salarioBase(null).build(), RELOJ), "salario_base");
    }

    @ParameterizedTest(name = "fecha = {0}")
    @ValueSource(strings = {"2025-06-15", "2025-06-16", "2030-01-01"})
    @DisplayName("cuando la fecha de nacimiento es hoy o futura, debe rechazar")
    void cuandoFechaNoEsPasada_debeRechazar(String fecha) {
        assertInvalido(() -> Usuario.registrar(
                ComandoUsuarioBuilder.valido().fechaNacimiento(LocalDate.parse(fecha)).build(), RELOJ), "fecha_nacimiento");
    }

    @Test
    @DisplayName("cuando se asigna un id, debe devolver una copia con el id y el resto intacto")
    void cuandoSeAsignaId_debeDevolverCopia() {
        var id = UUID.randomUUID();
        var original = Usuario.registrar(ComandoUsuarioBuilder.valido().build(), RELOJ);

        var conId = original.conId(id);

        assertThat(conId.id()).isEqualTo(id);
        assertThat(conId).usingRecursiveComparison().ignoringFields("id").isEqualTo(original);
        assertThat(original.id()).isNull();
    }
}
