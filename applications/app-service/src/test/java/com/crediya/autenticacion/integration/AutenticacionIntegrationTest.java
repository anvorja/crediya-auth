package com.crediya.autenticacion.integration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU1 + HU2 + HU3 de extremo a extremo: aplicacion completa + PostgreSQL real, con tokens firmados de verdad.
 */
@Tag("integration")
@Testcontainers
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Autenticacion y registro de usuarios (aplicacion completa)")
class AutenticacionIntegrationTest {

    private static final String USUARIOS = "/api/v1/usuarios";
    private static final String LOGIN = "/api/v1/login";
    private static final String ADMIN_EMAIL = "admin@crediya.test";
    private static final String ADMIN_PASSWORD = "Clave-Admin-123";
    private static final String ADMIN_DOCUMENTO = "0000000001";
    private static final String CLAVE = "Clave-Segura-123";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("DB_HOST", POSTGRES::getHost);
        registro.add("DB_PORT", () -> POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
        registro.add("DB_NAME", POSTGRES::getDatabaseName);
        registro.add("DB_USERNAME", POSTGRES::getUsername);
        registro.add("DB_PASSWORD", POSTGRES::getPassword);
        registro.add("BOOTSTRAP_ADMIN_EMAIL", () -> ADMIN_EMAIL);
        registro.add("BOOTSTRAP_ADMIN_PASSWORD", () -> ADMIN_PASSWORD);
        registro.add("BOOTSTRAP_ADMIN_DOCUMENTO", () -> ADMIN_DOCUMENTO);
        // Costo minimo de bcrypt: las pruebas no necesitan el costo de produccion
        registro.add("BCRYPT_STRENGTH", () -> "4");
    }

    @Autowired
    private WebTestClient client;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void dejarSoloElAdministradorInicial() {
        databaseClient.sql("DELETE FROM usuario WHERE documento_identidad <> :doc").bind("doc", ADMIN_DOCUMENTO).then().block();
    }

    // ---------- utilidades ----------

    private static String cuerpoUsuario(String documento, String correo, String salario, String rol, String password) {
        return """
                {"nombres":"Maria","apellidos":"Gomez","documento_identidad":"%s","fecha_nacimiento":"1995-03-20",
                 "direccion":"Calle 10","telefono":"+573001234567","correo_electronico":"%s","salario_base":%s%s%s}"""
                .formatted(documento, correo, salario,
                        rol == null ? "" : ",\"rol\":\"" + rol + "\"",
                        password == null ? "" : ",\"password\":\"" + password + "\"");
    }

    private WebTestClient.ResponseSpec loginRaw(String correo, String password) {
        return client.post().uri(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"correo_electronico\":\"%s\",\"password\":\"%s\"}".formatted(correo, password)).exchange();
    }

    @SuppressWarnings("unchecked")
    private String token(String correo, String password) {
        Map<String, Object> cuerpo = loginRaw(correo, password).expectStatus().isOk()
                .expectBody(Map.class).returnResult().getResponseBody();
        return (String) cuerpo.get("access_token");
    }

    private WebTestClient.ResponseSpec registrar(String token, String cuerpo) {
        var peticion = client.post().uri(USUARIOS).contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return peticion.bodyValue(cuerpo).exchange();
    }

    private WebTestClient.ResponseSpec consultar(String token, String documento) {
        return client.get().uri(USUARIOS + "/" + documento).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange();
    }

    private String tokenAdmin() {
        return token(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private String crearYLoguear(String documento, String correo, String rol) {
        registrar(tokenAdmin(), cuerpoUsuario(documento, correo, "4500000", rol, CLAVE)).expectStatus().isCreated();
        return token(correo, CLAVE);
    }

    // ---------- login ----------

    @Test
    @DisplayName("el administrador inicial debe poder iniciar sesion y recibir un Bearer de 3600 s")
    void administradorInicial_debePoderIniciarSesion() {
        loginRaw(ADMIN_EMAIL, ADMIN_PASSWORD)
                .expectStatus().isOk()
                .expectHeader().cacheControl(org.springframework.http.CacheControl.noStore())
                .expectBody()
                .jsonPath("$.access_token").isNotEmpty()
                .jsonPath("$.token_type").isEqualTo("Bearer")
                .jsonPath("$.expires_in").isEqualTo(3600);
    }

    @Test
    @DisplayName("una clave incorrecta y un usuario inexistente deben dar exactamente la misma respuesta 401")
    void claveIncorrectaYUsuarioInexistente_debenDarLaMismaRespuesta() {
        var claveIncorrecta = loginRaw(ADMIN_EMAIL, "otra-clave-123").expectStatus().isUnauthorized()
                .expectBody(String.class).returnResult().getResponseBody();
        var usuarioInexistente = loginRaw("nadie@crediya.test", "otra-clave-123").expectStatus().isUnauthorized()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(claveIncorrecta).contains("CREDENCIALES_INVALIDAS").contains("Correo o clave incorrectos");
        assertThat(sinVolatiles(claveIncorrecta)).isEqualTo(sinVolatiles(usuarioInexistente));
    }

    private static String sinVolatiles(String json) {
        return json.replaceAll("\"marca_tiempo\":\"[^\"]*\"", "");
    }

    @Test
    @DisplayName("los intentos son ilimitados: tras muchos fallos el usuario correcto aun puede iniciar sesion")
    void intentosIlimitados_noDebenBloquearLaCuenta() {
        for (int i = 0; i < 12; i++) {
            loginRaw(ADMIN_EMAIL, "mala-clave-" + i).expectStatus().isUnauthorized();
        }

        loginRaw(ADMIN_EMAIL, ADMIN_PASSWORD).expectStatus().isOk();
    }

    @Test
    @DisplayName("el login con correo en mayusculas debe funcionar")
    void loginConMayusculas_debeFuncionar() {
        loginRaw(ADMIN_EMAIL.toUpperCase(), ADMIN_PASSWORD).expectStatus().isOk();
    }

    @Test
    @DisplayName("el token debe verificarse con la clave publica publicada en el JWKS y llevar la identidad y el rol")
    void token_debeVerificarseConElJwks() throws Exception {
        String jwks = client.get().uri("/.well-known/jwks.json").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        assertThat(jwks).doesNotContain("\"d\"").doesNotContain("\"p\"").doesNotContain("\"q\"");

        var clavePublica = (RSAKey) JWKSet.parse(jwks).getKeys().get(0);
        var firmado = SignedJWT.parse(tokenAdmin());

        assertThat(firmado.verify(new RSASSAVerifier(clavePublica))).isTrue();
        assertThat(firmado.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(firmado.getHeader().getKeyID()).isEqualTo(clavePublica.getKeyID());
        var claims = firmado.getJWTClaimsSet();
        assertThat(claims.getStringClaim("rol")).isEqualTo("ADMIN");
        assertThat(claims.getStringClaim("documento_identidad")).isEqualTo(ADMIN_DOCUMENTO);
        assertThat(claims.getIssuer()).isEqualTo("crediya-auth");
        assertThat(claims.getAudience()).containsExactly("crediya");
    }

    // ---------- autenticacion ----------

    @Test
    @DisplayName("sin token, un endpoint protegido debe responder 401 en formato JSON")
    void sinToken_debeResponder401() {
        registrar(null, cuerpoUsuario("111111", "a@crediya.test", "1000", null, CLAVE))
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.codigo").isEqualTo("NO_AUTENTICADO");
    }

    @Test
    @DisplayName("un token basura debe responder 401")
    void tokenBasura_debeResponder401() {
        registrar("esto.no.es-un-jwt", cuerpoUsuario("111111", "a@crediya.test", "1000", null, CLAVE))
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("un token firmado con una clave ajena debe responder 401 aunque diga ser ADMIN")
    void tokenFirmadoConOtraClave_debeResponder401() throws Exception {
        RSAKey ajena = new RSAKeyGenerator(2048).keyID("ajena").generate();
        var claims = new JWTClaimsSet.Builder().issuer("crediya-auth").audience("crediya").subject(UUID.randomUUID().toString())
                .claim("rol", "ADMIN").claim("documento_identidad", "1").claim("email", "x@y.co")
                .expirationTime(new Date(System.currentTimeMillis() + 60_000)).build();
        var falso = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("ajena").build(), claims);
        falso.sign(new RSASSASigner(ajena));

        registrar(falso.serialize(), cuerpoUsuario("111111", "a@crediya.test", "1000", null, CLAVE))
                .expectStatus().isUnauthorized();
    }

    // ---------- registro (HU1) con autorizacion (HU3) ----------

    @Test
    @DisplayName("un ADMIN registra un cliente: 201, sin exponer clave ni hash, y en BD solo queda el hash bcrypt")
    void adminRegistraCliente_debeGuardarSoloElHash() {
        registrar(tokenAdmin(), cuerpoUsuario("1020304050", "Maria@Example.com", "4500000", null, CLAVE))
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.correo_electronico").isEqualTo("maria@example.com")
                .jsonPath("$.rol").isEqualTo("CLIENTE")
                .jsonPath("$.password").doesNotExist()
                .jsonPath("$.password_hash").doesNotExist();

        var hash = databaseClient.sql("SELECT password_hash FROM usuario WHERE documento_identidad = '1020304050'")
                .map((fila, metadatos) -> fila.get("password_hash", String.class)).one().block();
        assertThat(hash).startsWith("$2a$04$").doesNotContain(CLAVE);
    }

    @Test
    @DisplayName("el cliente recien registrado puede iniciar sesion con su clave")
    void clienteRegistrado_puedeIniciarSesion() {
        crearYLoguear("1020304050", "maria@crediya.test", null);
    }

    @Test
    @DisplayName("un CLIENTE no puede registrar usuarios: 403")
    void clienteNoPuedeRegistrar_debeResponder403() {
        String tokenCliente = crearYLoguear("1020304050", "maria@crediya.test", null);

        registrar(tokenCliente, cuerpoUsuario("2020202020", "otro@crediya.test", "1000", null, CLAVE))
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.codigo").isEqualTo("ACCESO_DENEGADO");
    }

    @Test
    @DisplayName("un ASESOR puede registrar clientes pero no crear ADMIN ni ASESOR (escalada de privilegios): 403")
    void asesor_soloPuedeRegistrarClientes() {
        String tokenAsesor = crearYLoguear("3030303030", "asesor@crediya.test", "ASESOR");

        registrar(tokenAsesor, cuerpoUsuario("4040404040", "cliente@crediya.test", "1000", null, CLAVE))
                .expectStatus().isCreated();
        registrar(tokenAsesor, cuerpoUsuario("5050505050", "admin2@crediya.test", "1000", "ADMIN", CLAVE))
                .expectStatus().isForbidden();
        registrar(tokenAsesor, cuerpoUsuario("6060606060", "asesor2@crediya.test", "1000", "ASESOR", CLAVE))
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("un ADMIN puede crear personal interno")
    void admin_puedeCrearPersonalInterno() {
        registrar(tokenAdmin(), cuerpoUsuario("7070707070", "asesor@crediya.test", "1000", "ASESOR", CLAVE))
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.rol").isEqualTo("ASESOR");
    }

    @Test
    @DisplayName("el registro valida los datos: salario fuera de rango, clave corta y correo invalido dan 400 con detalle")
    void registro_debeValidarDatos() {
        registrar(tokenAdmin(), cuerpoUsuario("1020304050", "no-es-correo", "15000001", null, "corta"))
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detalles.correo_electronico").isNotEmpty()
                .jsonPath("$.detalles.salario_base").isNotEmpty()
                .jsonPath("$.detalles.password").isNotEmpty();
    }

    @Test
    @DisplayName("un correo o documento ya registrado da 409")
    void registro_debeRechazarDuplicados() {
        registrar(tokenAdmin(), cuerpoUsuario("1020304050", "maria@crediya.test", "4500000", null, CLAVE)).expectStatus().isCreated();

        registrar(tokenAdmin(), cuerpoUsuario("9999999999", "MARIA@crediya.test", "4500000", null, CLAVE))
                .expectStatus().isEqualTo(409).expectBody().jsonPath("$.codigo").isEqualTo("USUARIO_DUPLICADO");
        registrar(tokenAdmin(), cuerpoUsuario("1020304050", "otra@crediya.test", "4500000", null, CLAVE))
                .expectStatus().isEqualTo(409);
    }

    // ---------- consulta (HU2) con autorizacion (HU3) ----------

    @Test
    @DisplayName("un CLIENTE puede consultar sus propios datos pero no los de otra persona")
    void cliente_soloConsultaSusDatos() {
        String tokenMaria = crearYLoguear("1020304050", "maria@crediya.test", null);
        registrar(tokenAdmin(), cuerpoUsuario("2020202020", "pedro@crediya.test", "2000000", null, CLAVE)).expectStatus().isCreated();

        consultar(tokenMaria, "1020304050").expectStatus().isOk()
                .expectBody().jsonPath("$.correo_electronico").isEqualTo("maria@crediya.test");
        consultar(tokenMaria, "2020202020").expectStatus().isForbidden();
    }

    @Test
    @DisplayName("un ADMIN puede consultar cualquier usuario y recibe 404 si no existe")
    void admin_consultaCualquiera() {
        registrar(tokenAdmin(), cuerpoUsuario("1020304050", "maria@crediya.test", "4500000", null, CLAVE)).expectStatus().isCreated();

        consultar(tokenAdmin(), "1020304050").expectStatus().isOk();
        consultar(tokenAdmin(), "99999999").expectStatus().isNotFound()
                .expectBody().jsonPath("$.codigo").isEqualTo("USUARIO_NO_ENCONTRADO");
    }

    @Test
    @DisplayName("consultar un usuario sin token da 401")
    void consultarSinToken_debeResponder401() {
        client.get().uri(USUARIOS + "/1020304050").exchange().expectStatus().isUnauthorized();
    }

    // ---------- operacion ----------

    @Test
    @DisplayName("la documentacion, el health y el JWKS deben ser publicos y la documentacion debe declarar el esquema Bearer")
    void documentacionYHealth_debenSerPublicos() {
        client.get().uri("/v3/api-docs").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['/api/v1/login'].post.operationId").isEqualTo("login")
                .jsonPath("$.paths['/api/v1/usuarios'].post.operationId").isEqualTo("registrarUsuario")
                .jsonPath("$.components.securitySchemes.bearerAuth.scheme").isEqualTo("bearer");
        client.get().uri("/actuator/health").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }
}
