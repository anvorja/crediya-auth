package com.crediya.autenticacion.r2dbc.usuario;

import com.crediya.autenticacion.model.usuario.Rol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RolCatalogo")
class RolCatalogoTest {

    @ParameterizedTest(name = "rol {0}")
    @EnumSource(Rol.class)
    @DisplayName("debe convertir rol -> id -> rol sin perdida")
    void debeIdaYVuelta(Rol rol) {
        assertThat(RolCatalogo.rolDe(RolCatalogo.idDe(rol))).isEqualTo(rol);
    }

    @Test
    @DisplayName("cuando el id no existe, debe fallar de forma explicita")
    void cuandoIdDesconocido_debeFallar() {
        assertThatThrownBy(() -> RolCatalogo.rolDe((short) 99))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("99");
    }
}
