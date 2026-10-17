package com.crediya.autenticacion.usecase.login;

import com.crediya.autenticacion.model.seguridad.Token;
import com.crediya.autenticacion.model.seguridad.gateways.PasswordEncoderGateway;
import com.crediya.autenticacion.model.seguridad.gateways.TokenGateway;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.commands.LoginCommand;
import com.crediya.autenticacion.model.usuario.exceptions.CredencialesInvalidasException;
import com.crediya.autenticacion.model.usuario.gateways.UsuarioRepository;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * HU3: inicio de sesion con correo y clave. Los intentos son ilimitados (criterio de aceptacion de la HU).
 * Un usuario inexistente y una clave incorrecta producen exactamente el mismo error y un costo de calculo similar.
 */
public class LoginUseCase {

    /**
     * Hash bcrypt valido de una clave cualquiera: se compara cuando el usuario no existe para no delatarlo por tiempo.
     */
    static final String HASH_FICTICIO = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoderGateway passwordEncoder;
    private final TokenGateway tokenGateway;

    public LoginUseCase(UsuarioRepository usuarioRepository,
                        PasswordEncoderGateway passwordEncoder,
                        TokenGateway tokenGateway) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenGateway = tokenGateway;
    }

    public Mono<Token> ejecutar(LoginCommand comando) {
        return Mono.justOrEmpty(comando)
                .filter(c -> c.email() != null && !c.email().isBlank() && c.password() != null && !c.password().isBlank())
                .flatMap(c -> autenticar(c.email().trim().toLowerCase(Locale.ROOT), c.password()))
                .switchIfEmpty(Mono.error(CredencialesInvalidasException::new))
                .flatMap(tokenGateway::emitir);
    }

    private Mono<Usuario> autenticar(String email, String password) {
        return usuarioRepository.buscarPorEmail(email)
                .filter(usuario -> usuario.passwordHash() != null)
                .flatMap(usuario -> passwordEncoder.coincide(password, usuario.passwordHash())
                        .filter(Boolean.TRUE::equals)
                        .map(coincide -> usuario))
                .switchIfEmpty(Mono.defer(() -> passwordEncoder.coincide(password, HASH_FICTICIO).then(Mono.empty())));
    }
}
