package com.crediya.autenticacion.passwordencoder;

import com.crediya.autenticacion.model.seguridad.gateways.PasswordEncoderGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * BCrypt es costoso a proposito (CPU y SecureRandom): se ejecuta en boundedElastic para no frenar los hilos
 * del event loop de WebFlux.
 */
@Component
public class BcryptPasswordEncoderAdapter implements PasswordEncoderGateway {

    private final BCryptPasswordEncoder encoder;

    public BcryptPasswordEncoderAdapter(@Value("${security.bcrypt.strength:10}") int strength) {
        this.encoder = new BCryptPasswordEncoder(strength);
    }

    @Override
    public Mono<String> codificar(String passwordPlano) {
        return Mono.fromCallable(() -> encoder.encode(passwordPlano))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> coincide(String passwordPlano, String hash) {
        return Mono.fromCallable(() -> encoder.matches(passwordPlano, hash))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
