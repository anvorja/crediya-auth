package com.crediya.autenticacion.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * Construye la clave de firma: la configurada o, si no hay, una efimera.
 */
@Slf4j
final class RsaKeyFactory {

    private static final int BITS_CLAVE_EFIMERA = 2048;

    private RsaKeyFactory() {
    }

    static RSAKey desde(JwtProperties propiedades) {
        if (propiedades.privateKey() == null || propiedades.privateKey().isBlank()) {
            return generarEfimera();
        }
        return cargar(propiedades.privateKey());
    }

    private static RSAKey generarEfimera() {
        log.warn("security.jwt.private-key no esta configurada: se genera una clave RSA efimera. "
                + "Los tokens se invalidan al reiniciar y no es valido con varias replicas.");
        try {
            return new RSAKeyGenerator(BITS_CLAVE_EFIMERA)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyIDFromThumbprint(true)
                    .generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo generar la clave RSA efimera", e);
        }
    }

    private static RSAKey cargar(String configurada) {
        try {
            String base64 = configurada.replaceAll("-----(BEGIN|END)[A-Z ]*-----", "").replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            KeyFactory fabrica = KeyFactory.getInstance("RSA");
            var privada = (RSAPrivateCrtKey) fabrica.generatePrivate(new PKCS8EncodedKeySpec(der));
            var publica = (RSAPublicKey) fabrica.generatePublic(
                    new RSAPublicKeySpec(privada.getModulus(), privada.getPublicExponent()));
            return new RSAKey.Builder(publica)
                    .privateKey(privada)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyIDFromThumbprint()
                    .build();
        } catch (Exception e) {
            // No se incluye la causa para no filtrar fragmentos de la clave en los logs
            throw new IllegalStateException("security.jwt.private-key no es una clave RSA PKCS8 valida (PEM o base64)");
        }
    }
}
