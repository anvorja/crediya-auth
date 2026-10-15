package com.crediya.autenticacion.usecase.registrarusuario;

import com.crediya.autenticacion.model.common.gateways.TransaccionGateway;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.commands.RegistrarUsuarioCommand;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioDuplicadoException;
import com.crediya.autenticacion.model.usuario.gateways.UsuarioRepository;
import reactor.core.publisher.Mono;

import java.time.Clock;

/**
 * HU1: registra un usuario validando sus datos y la unicidad de correo y documento.
 */
public class RegistrarUsuarioUseCase {

    static final String CAMPO_EMAIL = "correo electronico";
    static final String CAMPO_DOCUMENTO = "documento de identidad";

    private final UsuarioRepository usuarioRepository;
    private final TransaccionGateway transaccionGateway;
    private final Clock reloj;

    public RegistrarUsuarioUseCase(UsuarioRepository usuarioRepository,
                                   TransaccionGateway transaccionGateway,
                                   Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.transaccionGateway = transaccionGateway;
        this.reloj = reloj;
    }

    public Mono<Usuario> ejecutar(RegistrarUsuarioCommand comando) {
        return Mono.fromCallable(() -> Usuario.registrar(comando, reloj))
                .flatMap(this::asegurarEmailNoRegistrado)
                .flatMap(this::asegurarDocumentoNoRegistrado)
                .flatMap(usuarioRepository::guardar)
                .as(transaccionGateway::ejecutar);
    }

    private Mono<Usuario> asegurarEmailNoRegistrado(Usuario usuario) {
        return usuarioRepository.existePorEmail(usuario.email())
                .filter(Boolean.FALSE::equals)
                .switchIfEmpty(Mono.error(() -> new UsuarioDuplicadoException(CAMPO_EMAIL)))
                .thenReturn(usuario);
    }

    private Mono<Usuario> asegurarDocumentoNoRegistrado(Usuario usuario) {
        return usuarioRepository.existePorDocumentoIdentidad(usuario.documentoIdentidad())
                .filter(Boolean.FALSE::equals)
                .switchIfEmpty(Mono.error(() -> new UsuarioDuplicadoException(CAMPO_DOCUMENTO)))
                .thenReturn(usuario);
    }
}
