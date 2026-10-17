package com.crediya.autenticacion.usecase.consultarusuarios;

import com.crediya.autenticacion.model.common.exceptions.AccesoDenegadoException;
import com.crediya.autenticacion.model.seguridad.gateways.ContextoSeguridadGateway;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.exceptions.DatosUsuarioInvalidosException;
import com.crediya.autenticacion.model.usuario.gateways.UsuarioRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * HU4: permite a SOLICITUDES obtener nombre y salario de varios solicitantes en una sola llamada para armar el listado
 * de revision. Solo personal interno (ADMIN o ASESOR): expone datos personales de terceros.
 */
public class ConsultarUsuariosPorCorreoUseCase {

    public static final int MAXIMO_CORREOS = 50;

    private final UsuarioRepository usuarioRepository;
    private final ContextoSeguridadGateway contextoSeguridad;

    public ConsultarUsuariosPorCorreoUseCase(UsuarioRepository usuarioRepository, ContextoSeguridadGateway contextoSeguridad) {
        this.usuarioRepository = usuarioRepository;
        this.contextoSeguridad = contextoSeguridad;
    }

    public Flux<Usuario> ejecutar(Collection<String> correos) {
        return contextoSeguridad.usuarioActual()
                .filter(actor -> actor.rol().esPersonalInterno())
                .switchIfEmpty(Mono.error(AccesoDenegadoException::new))
                .then(Mono.fromCallable(() -> normalizar(correos)))
                .flatMapMany(lista -> lista.isEmpty() ? Flux.empty() : usuarioRepository.buscarPorEmails(lista));
    }

    private static List<String> normalizar(Collection<String> correos) {
        if (correos == null) {
            return List.of();
        }
        List<String> limpios = correos.stream()
                .filter(Objects::nonNull)
                .map(correo -> correo.trim().toLowerCase(Locale.ROOT))
                .filter(correo -> !correo.isEmpty())
                .distinct()
                .toList();
        if (limpios.size() > MAXIMO_CORREOS) {
            throw new DatosUsuarioInvalidosException("correos", "no puede superar " + MAXIMO_CORREOS + " correos por consulta");
        }
        return limpios;
    }
}
