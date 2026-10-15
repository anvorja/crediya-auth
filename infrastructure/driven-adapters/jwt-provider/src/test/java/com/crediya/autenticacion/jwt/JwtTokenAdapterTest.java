package com.crediya.autenticacion.jwt;

import com.crediya.autenticacion.model.usuario.Rol;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.test.StepVerifier;

import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenAdapter + JwtConfig")
class JwtTokenAdapterTest {

    private final JwtConfig config = new JwtConfig();
    private RSAKey clave;
    private JwtProperties propiedades;
    private JwtEncoder encoder;
    private JwtTokenAdapter adaptador;

    @BeforeEach
    void setUp() {
        propiedades = new JwtProperties("crediya-auth", "crediya", 60L, null);
        clave = config.rsaKey(propiedades);
        encoder = config.jwtEncoder(clave);
        adaptador = new JwtTokenAdapter(encoder, clave, propiedades, Clock.systemUTC());
    }

    private ReactiveJwtDecoder decoder() throws Exception {
        return config.reactiveJwtDecoder(clave, propiedades);
    }

    @Test
    @DisplayName("debe emitir un Bearer de 3600 s con los claims de identidad, rol, emisor, audiencia y jti")
    void debeEmitirTokenConLosClaimsEsperados() throws Exception {
        var token = adaptador.emitir(JwtTestSupport.usuario(Rol.ASESOR)).block();

        assertThat(token.tipo()).isEqualTo("Bearer");
        assertThat(token.expiraEnSegundos()).isEqualTo(3600);

        var jwt = NimbusJwtDecoder.withPublicKey((RSAPublicKey) clave.toPublicKey()).build().decode(token.valor());
        assertThat(jwt.getSubject()).isEqualTo("6f1c1c52-1d5e-4a45-9b52-8b1f3f1a0a11");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("crediya-auth");
        assertThat(jwt.getAudience()).containsExactly("crediya");
        assertThat(jwt.<String>getClaim("email")).isEqualTo("maria@example.com");
        assertThat(jwt.<String>getClaim("documento_identidad")).isEqualTo("1020304050");
        assertThat(jwt.<String>getClaim("rol")).isEqualTo("ASESOR");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getHeaders()).containsEntry("alg", "RS256").containsEntry("kid", clave.getKeyID());
        assertThat(jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond()).isEqualTo(3600);
    }

    @Test
    @DisplayName("no debe incluir la clave ni datos sensibles en el token (salario, hash)")
    void noDebeIncluirDatosSensibles() throws Exception {
        var token = adaptador.emitir(JwtTestSupport.usuario(Rol.CLIENTE).conPasswordHash("$2a$10$secreto")).block();

        var jwt = NimbusJwtDecoder.withPublicKey((RSAPublicKey) clave.toPublicKey()).build().decode(token.valor());
        assertThat(jwt.getClaims().keySet()).doesNotContain("salario_base", "password", "password_hash");
        assertThat(token.toString()).doesNotContain(token.valor());
    }

    @Test
    @DisplayName("el decodificador reactivo debe aceptar un token valido y exponer sus claims")
    void decodificador_debeAceptarTokenValido() throws Exception {
        var token = adaptador.emitir(JwtTestSupport.usuario(Rol.CLIENTE)).block();

        StepVerifier.create(decoder().decode(token.valor()))
                .assertNext(jwt -> assertThat(jwt.<String>getClaim("rol")).isEqualTo("CLIENTE"))
                .verifyComplete();
    }

    @Test
    @DisplayName("el decodificador debe rechazar un token manipulado")
    void decodificador_debeRechazarTokenManipulado() throws Exception {
        var token = adaptador.emitir(JwtTestSupport.usuario(Rol.CLIENTE)).block().valor();
        String[] partes = token.split("\\.");
        String payloadAlterado = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(java.util.Base64.getUrlDecoder().decode(partes[1])).replace("CLIENTE", "ADMIN").getBytes());

        StepVerifier.create(decoder().decode(partes[0] + "." + payloadAlterado + "." + partes[2]))
                .expectError(BadJwtException.class)
                .verify();
    }

    @Test
    @DisplayName("el decodificador debe rechazar un token firmado con otra clave")
    void decodificador_debeRechazarOtraClave() throws Exception {
        var claveAjena = JwtTestSupport.claveEfimera();
        var otro = new JwtTokenAdapter(config.jwtEncoder(claveAjena), claveAjena, propiedades, Clock.systemUTC());
        var tokenAjeno = otro.emitir(JwtTestSupport.usuario(Rol.ADMIN)).block().valor();

        StepVerifier.create(decoder().decode(tokenAjeno))
                .expectError(BadJwtException.class)
                .verify();
    }

    @Test
    @DisplayName("el decodificador debe rechazar un token vencido")
    void decodificador_debeRechazarTokenVencido() throws Exception {
        var relojViejo = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);
        var viejo = new JwtTokenAdapter(encoder, clave, propiedades, relojViejo);
        var token = viejo.emitir(JwtTestSupport.usuario(Rol.CLIENTE)).block().valor();

        StepVerifier.create(decoder().decode(token))
                .expectError(BadJwtException.class)
                .verify();
    }

    @Test
    @DisplayName("el decodificador debe rechazar un token de otro emisor o dirigido a otra audiencia")
    void decodificador_debeRechazarEmisorOAudienciaAjenos() throws Exception {
        var otroEmisor = new JwtTokenAdapter(encoder, clave, new JwtProperties("otro-emisor", "crediya", 60L, null), Clock.systemUTC());
        var otraAudiencia = new JwtTokenAdapter(encoder, clave, new JwtProperties("crediya-auth", "otro-servicio", 60L, null), Clock.systemUTC());

        StepVerifier.create(decoder().decode(otroEmisor.emitir(JwtTestSupport.usuario(Rol.CLIENTE)).block().valor()))
                .expectError(BadJwtException.class).verify();
        StepVerifier.create(decoder().decode(otraAudiencia.emitir(JwtTestSupport.usuario(Rol.CLIENTE)).block().valor()))
                .expectError(BadJwtException.class).verify();
    }

    @Test
    @DisplayName("debe respetar el TTL configurado")
    void debeRespetarTtl() {
        var corto = new JwtTokenAdapter(encoder, clave, new JwtProperties(null, null, 5L, null), Clock.systemUTC());

        assertThat(corto.emitir(JwtTestSupport.usuario(Rol.CLIENTE)).block().expiraEnSegundos()).isEqualTo(300);
    }

    @Test
    @DisplayName("el JWKS debe publicar solo la clave publica (sin parametros privados) con kid y algoritmo de firma")
    void jwks_debePublicarSoloClavePublica() {
        Map<String, Object> jwks = adaptador.conjuntoJwk().block();

        @SuppressWarnings("unchecked")
        var claves = (List<Map<String, Object>>) jwks.get("keys");
        assertThat(claves).hasSize(1);
        var jwk = claves.get(0);
        assertThat(jwk).containsEntry("kty", "RSA").containsEntry("kid", clave.getKeyID()).containsEntry("use", "sig");
        assertThat(jwk).containsKeys("n", "e");
        assertThat(jwk).doesNotContainKeys("d", "p", "q", "dp", "dq", "qi");
    }

    @Test
    @DisplayName("el emisor no debe poder firmar si la clave no tiene parte privada")
    void sinParteprivada_noDebePoderFirmar() {
        var soloPublica = clave.toPublicJWK();
        var adaptadorSinPrivada = new JwtTokenAdapter(config.jwtEncoder(soloPublica), soloPublica, propiedades, Clock.systemUTC());

        assertThatThrownBy(() -> adaptadorSinPrivada.emitir(JwtTestSupport.usuario(Rol.CLIENTE)).block())
                .isInstanceOf(RuntimeException.class);
    }
}
