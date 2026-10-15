package com.crediya.autenticacion.jwt;

import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RsaKeyFactory y JwtProperties")
class RsaKeyFactoryTest {

    @Test
    @DisplayName("cuando se configura una clave PEM, debe cargarla y derivar la misma clave publica")
    void cuandoClavePem_debeCargarla() throws Exception {
        var par = JwtTestSupport.generarParDeClaves();

        RSAKey clave = RsaKeyFactory.desde(new JwtProperties(null, null, null, JwtTestSupport.pem(par)));

        assertThat(clave.isPrivate()).isTrue();
        assertThat(((RSAPublicKey) clave.toPublicKey()).getModulus()).isEqualTo(((RSAPublicKey) par.getPublic()).getModulus());
        assertThat(clave.getKeyID()).isNotBlank();
    }

    @Test
    @DisplayName("cuando se configura la clave como base64 sin cabeceras, tambien debe cargarla con el mismo kid")
    void cuandoBase64SinCabeceras_debeCargarlaConElMismoKid() throws Exception {
        var par = JwtTestSupport.generarParDeClaves();
        var base64 = Base64.getEncoder().encodeToString(par.getPrivate().getEncoded());

        var desdePem = RsaKeyFactory.desde(new JwtProperties(null, null, null, JwtTestSupport.pem(par)));
        var desdeBase64 = RsaKeyFactory.desde(new JwtProperties(null, null, null, base64));

        assertThat(desdeBase64.getKeyID()).isEqualTo(desdePem.getKeyID());
    }

    @Test
    @DisplayName("cuando la clave es el base64 de un PEM (salida de openssl codificada), tambien debe cargarla con el mismo kid")
    void cuandoBase64DeUnPem_tambienDebeCargarla() throws Exception {
        var par = JwtTestSupport.generarParDeClaves();
        var base64DelPem = Base64.getEncoder().encodeToString(JwtTestSupport.pem(par).getBytes(java.nio.charset.StandardCharsets.US_ASCII));

        var desdePem = RsaKeyFactory.desde(new JwtProperties(null, null, null, JwtTestSupport.pem(par)));
        var desdeBase64DelPem = RsaKeyFactory.desde(new JwtProperties(null, null, null, base64DelPem));

        assertThat(desdeBase64DelPem.getKeyID()).isEqualTo(desdePem.getKeyID());
        assertThat(desdeBase64DelPem.isPrivate()).isTrue();
    }

    @ParameterizedTest(name = "clave = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("cuando no hay clave configurada, debe generar una efimera distinta en cada arranque")
    void cuandoSinClave_debeGenerarEfimera(String vacia) {
        var primera = RsaKeyFactory.desde(new JwtProperties(null, null, null, vacia));
        var segunda = RsaKeyFactory.desde(new JwtProperties(null, null, null, vacia));

        assertThat(primera.isPrivate()).isTrue();
        assertThat(primera.getKeyID()).isNotEqualTo(segunda.getKeyID());
    }

    @Test
    @DisplayName("cuando la clave configurada es invalida, debe fallar rapido sin filtrar su contenido")
    void cuandoClaveInvalida_debeFallarSinFiltrarContenido() {
        assertThatThrownBy(() -> RsaKeyFactory.desde(new JwtProperties(null, null, null, "esto-no-es-una-clave-super-secreta")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PKCS8")
                .hasMessageNotContaining("super-secreta")
                .hasNoCause();
    }

    @Test
    @DisplayName("cuando la clave esta en PKCS#1 (formato tradicional de openssl), debe rechazar indicando como convertirla")
    void cuandoClavePkcs1_debeRechazarIndicandoLaConversion() throws Exception {
        // Estructura minima de una clave PKCS#1: SEQUENCE { INTEGER 0, INTEGER n ... } no es un PKCS#8 (le falta AlgorithmIdentifier)
        var pkcs1 = Base64.getEncoder().encodeToString(new byte[] {0x30, 0x0b, 0x02, 0x01, 0x00, 0x02, 0x06, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06});

        assertThatThrownBy(() -> RsaKeyFactory.desde(new JwtProperties(null, null, null, pkcs1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PKCS8")
                .hasMessageContaining("pkcs8 -topk8");
    }

    @Test
    @DisplayName("las propiedades deben tener valores por defecto sensatos y no imprimir la clave privada")
    void propiedades_debenTenerDefaultsYNoImprimirClave() {
        var porDefecto = new JwtProperties(null, " ", 0L, null);
        var conClave = new JwtProperties("emisor", "aud", 30L, "clave-privada-secreta");

        assertThat(porDefecto.issuer()).isEqualTo("crediya-auth");
        assertThat(porDefecto.audience()).isEqualTo("crediya");
        assertThat(porDefecto.ttlMinutes()).isEqualTo(60L);
        assertThat(conClave.toString()).doesNotContain("clave-privada-secreta").contains("****");
        assertThat(porDefecto.toString()).contains("efimera");
    }
}
