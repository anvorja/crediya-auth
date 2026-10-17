package com.crediya.autenticacion.usecase.registrarusuario;

import com.crediya.autenticacion.model.common.exceptions.AccesoDenegadoException;
import com.crediya.autenticacion.model.common.gateways.TransaccionGateway;
import com.crediya.autenticacion.model.seguridad.UsuarioAutenticado;
import com.crediya.autenticacion.model.seguridad.gateways.ContextoSeguridadGateway;
import com.crediya.autenticacion.model.seguridad.gateways.PasswordEncoderGateway;
import com.crediya.autenticacion.model.usuario.Rol;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.commands.RegistrarUsuarioCommand;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioDuplicadoException;
import com.crediya.autenticacion.model.usuario.gateways.UsuarioRepository;
import reactor.core.publisher.Mono;

import java.time.Clock;

/**
 * HU1 + HU3: registra un usuario. Solo ADMIN y ASESOR pueden registrar, y solo un ADMIN puede crear personal
 * interno (ADMIN o ASESOR), para evitar escalada de privilegios.
 */
public class RegistrarUsuarioUseCase {

    static final String CAMPO_EMAIL = "correo electronico";
    static final String CAMPO_DOCUMENTO = "documento de identidad";

    private final UsuarioRepository usuarioRepository;
    private final TransaccionGateway transaccionGateway;
    private final PasswordEncoderGateway passwordEncoder;
    private final ContextoSeguridadGateway contextoSeguridad;
    private final Clock reloj;

    public RegistrarUsuarioUseCase(UsuarioRepository usuarioRepository,
                                   TransaccionGateway transaccionGateway,
                                   PasswordEncoderGateway passwordEncoder,
                                   ContextoSeguridadGateway contextoSeguridad,
                                   Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.transaccionGateway = transaccionGateway;
        this.passwordEncoder = passwordEncoder;
        this.contextoSeguridad = contextoSeguridad;
        this.reloj = reloj;
    }

    public Mono<Usuario> ejecutar(RegistrarUsuarioCommand comando) {
        return contextoSeguridad.usuarioActual()
                .filter(actor -> puedeRegistrar(actor, comando))
                .switchIfEmpty(Mono.error(AccesoDenegadoException::new))
                .then(Mono.defer(() -> registrar(comando)));
    }

    private static boolean puedeRegistrar(UsuarioAutenticado actor, RegistrarUsuarioCommand comando) {
        if (!actor.rol().esPersonalInterno()) {
            return false;
        }
        return actor.rol() == Rol.ADMIN || !Rol.desde(comando.rol()).esPersonalInterno();
    }

    private Mono<Usuario> registrar(RegistrarUsuarioCommand comando) {
        return Mono.fromCallable(() -> Usuario.registrar(comando, reloj))
                .flatMap(this::asegurarEmailNoRegistrado)
                .flatMap(this::asegurarDocumentoNoRegistrado)
                .flatMap(usuario -> passwordEncoder.codificar(comando.password()).map(usuario::conPasswordHash))
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
