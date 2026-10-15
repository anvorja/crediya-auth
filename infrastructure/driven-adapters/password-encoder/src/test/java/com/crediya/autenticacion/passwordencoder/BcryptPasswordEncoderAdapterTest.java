package com.crediya.autenticacion.passwordencoder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BcryptPasswordEncoderAdapter")
class BcryptPasswordEncoderAdapterTest {

    // Costo minimo permitido por bcrypt: mantiene las pruebas rapidas sin cambiar el comportamiento
    private final BcryptPasswordEncoderAdapter adapter = new BcryptPasswordEncoderAdapter(4);

    @Test
    @DisplayName("debe generar un hash bcrypt que no contiene la clave y es distinto en cada llamada (salt aleatorio)")
    void debeGenerarHashBcryptConSalt() {
        var primero = adapter.codificar("Clave-Segura-123").block();
        var segundo = adapter.codificar("Clave-Segura-123").block();

        assertThat(primero).startsWith("$2a$04$").hasSize(60).doesNotContain("Clave-Segura-123");
        assertThat(primero).isNotEqualTo(segundo);
    }

    @Test
    @DisplayName("debe validar la clave correcta y rechazar la incorrecta")
    void debeValidarClaveCorrectaEIncorrecta() {
        var hash = adapter.codificar("Clave-Segura-123").block();

        StepVerifier.create(adapter.coincide("Clave-Segura-123", hash)).expectNext(true).verifyComplete();
        StepVerifier.create(adapter.coincide("otra-clave", hash)).expectNext(false).verifyComplete();
    }

    @Test
    @DisplayName("cuando el hash no tiene formato bcrypt, debe responder false sin lanzar error")
    void cuandoHashInvalido_debeResponderFalse() {
        StepVerifier.create(adapter.coincide("Clave-Segura-123", "no-es-un-hash")).expectNext(false).verifyComplete();
    }

    @Test
    @DisplayName("debe funcionar desde un hilo no bloqueante sin que BlockHound detecte llamadas bloqueantes")
    void debeFuncionarDesdeHiloNoBloqueante() {
        Mono<Boolean> desdeParallel = Mono.defer(() -> adapter.codificar("Clave-Segura-123")
                        .flatMap(hash -> adapter.coincide("Clave-Segura-123", hash)))
                .subscribeOn(Schedulers.parallel());

        StepVerifier.create(desdeParallel).expectNext(true).verifyComplete();
    }
}
