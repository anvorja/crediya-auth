package com.crediya.autenticacion.jwt;

import com.crediya.autenticacion.model.seguridad.Token;
import com.crediya.autenticacion.model.seguridad.gateways.ClavesPublicasGateway;
import com.crediya.autenticacion.model.seguridad.gateways.TokenGateway;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Emite JWT firmados con RS256 y publica la clave publica (JWKS) con la que otros servicios los verifican.
 */
@Slf4j
@Component
public class JwtTokenAdapter implements TokenGateway, ClavesPublicasGateway {

    static final String CLAIM_EMAIL = "email";
    static final String CLAIM_DOCUMENTO = "documento_identidad";
    static final String CLAIM_ROL = "rol";

    private final JwtEncoder encoder;
    private final RSAKey rsaKey;
    private final JwtProperties propiedades;
    private final Clock reloj;

    public JwtTokenAdapter(JwtEncoder encoder, RSAKey rsaKey, JwtProperties propiedades, Clock reloj) {
        this.encoder = encoder;
        this.rsaKey = rsaKey;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Override
    public Mono<Token> emitir(Usuario usuario) {
        // La firma RSA es costosa y jti usa SecureRandom: se hace fuera de los hilos del event loop
        return Mono.fromCallable(() -> firmar(usuario)).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Map<String, Object>> conjuntoJwk() {
        return Mono.fromSupplier(() -> new JWKSet(rsaKey.toPublicJWK()).toJSONObject(true));
    }

    private Token firmar(Usuario usuario) {
        Instant ahora = reloj.instant();
        Duration vigencia = Duration.ofMinutes(propiedades.ttlMinutes());
        var claims = JwtClaimsSet.builder()
                .issuer(propiedades.issuer())
                .audience(List.of(propiedades.audience()))
                .subject(usuario.id().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(vigencia))
                .claim(CLAIM_EMAIL, usuario.email())
                .claim(CLAIM_DOCUMENTO, usuario.documentoIdentidad())
                .claim(CLAIM_ROL, usuario.rol().name())
                .build();
        var cabecera = JwsHeader.with(SignatureAlgorithm.RS256).keyId(rsaKey.getKeyID()).build();
        String valor = encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        log.info("Token emitido para el usuario id={} rol={}", usuario.id(), usuario.rol());
        return Token.bearer(valor, vigencia.toSeconds());
    }
}
