package com.crediya.autenticacion.model.usuario;

import com.crediya.autenticacion.model.common.exceptions.DomainException;
import com.crediya.autenticacion.model.usuario.exceptions.DatosUsuarioInvalidosException;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioDuplicadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Rol y excepciones de usuario")
class RolTest {

    @ParameterizedTest(name = "rol = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("cuando no se informa el rol, debe asumir CLIENTE")
    void cuandoSinRol_debeAsumirCliente(String texto) {
        assertThat(Rol.desde(texto)).isEqualTo(Rol.CLIENTE);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"admin,ADMIN", " Asesor ,ASESOR", "CLIENTE,CLIENTE"})
    @DisplayName("cuando el texto es un rol valido, debe ignorar mayusculas y espacios")
    void cuandoRolValido_debeConvertir(String texto, Rol esperado) {
        assertThat(Rol.desde(texto)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("cuando el rol no existe, debe rechazar")
    void cuandoRolInexistente_debeRechazar() {
        assertThatThrownBy(() -> Rol.desde("SUPERUSUARIO"))
                .isInstanceOf(DatosUsuarioInvalidosException.class)
                .hasMessageContaining("rol");
    }

    @Test
    @DisplayName("cada rol debe tener una descripcion")
    void cadaRol_debeTenerDescripcion() {
        for (Rol rol : Rol.values()) {
            assertThat(rol.getDescripcion()).isNotBlank();
        }
    }

    @Test
    @DisplayName("las excepciones de dominio deben exponer codigo estable y campo")
    void excepciones_debenExponerCodigoYCampo() {
        DomainException invalidos = new DatosUsuarioInvalidosException("email", "es obligatorio");
        DomainException duplicado = new UsuarioDuplicadoException("correo electronico");

        assertThat(invalidos.getCode()).isEqualTo(DatosUsuarioInvalidosException.CODE);
        assertThat(((DatosUsuarioInvalidosException) invalidos).getCampo()).isEqualTo("email");
        assertThat(invalidos.getMessage()).isEqualTo("El campo 'email' es obligatorio");
        assertThat(duplicado.getCode()).isEqualTo(UsuarioDuplicadoException.CODE);
        assertThat(((UsuarioDuplicadoException) duplicado).getCampo()).isEqualTo("correo electronico");
    }
}
