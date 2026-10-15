package com.crediya.autenticacion.jwt;

import com.crediya.autenticacion.model.usuario.Rol;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.nimbusds.jose.jwk.RSAKey;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.UUID;

final class JwtTestSupport {

    private JwtTestSupport() {
    }

    static KeyPair generarParDeClaves() throws Exception {
        var generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        return generador.generateKeyPair();
    }

    /**
     * Clave privada PKCS8 como la entregaria un gestor de secretos: PEM con cabeceras.
     */
    static String pem(KeyPair par) {
        String base64 = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(par.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----";
    }

    static Usuario usuario(Rol rol) {
        return new Usuario(UUID.fromString("6f1c1c52-1d5e-4a45-9b52-8b1f3f1a0a11"), "Maria", "Gomez", "1020304050", null,
                null, null, "maria@example.com", new BigDecimal("4500000"), rol);
    }

    static RSAKey claveEfimera() {
        return RsaKeyFactory.desde(new JwtProperties(null, null, null, null));
    }
}
