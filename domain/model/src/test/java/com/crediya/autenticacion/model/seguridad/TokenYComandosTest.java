package com.crediya.autenticacion.model.seguridad;

import com.crediya.autenticacion.model.usuario.commands.LoginCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Token y comandos de seguridad")
class TokenYComandosTest {

    @Test
    @DisplayName("Token.bearer debe marcar el tipo Bearer y conservar la expiracion")
    void tokenBearer_debeMarcarTipo() {
        var token = Token.bearer("abc.def.ghi", 3600);

        assertThat(token.tipo()).isEqualTo("Bearer");
        assertThat(token.valor()).isEqualTo("abc.def.ghi");
        assertThat(token.expiraEnSegundos()).isEqualTo(3600);
    }

    @Test
    @DisplayName("el toString del token y del login no debe filtrar secretos")
    void toString_noDebeFiltrarSecretos() {
        assertThat(Token.bearer("abc.def.ghi", 60).toString()).doesNotContain("abc.def.ghi");
        assertThat(new LoginCommand("a@b.co", "mi-clave").toString()).doesNotContain("mi-clave").contains("password=****");
    }
}
