package com.crediya.autenticacion.r2dbc.usuario;

import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioDuplicadoException;
import com.crediya.autenticacion.model.usuario.gateways.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    static final String CONSTRAINT_EMAIL = "uq_usuario_email";
    static final String CONSTRAINT_DOCUMENTO = "uq_usuario_documento";

    private final R2dbcEntityTemplate template;

    @Override
    public Mono<Usuario> guardar(Usuario usuario) {
        // Si el usuario no trae id lo genera PostgreSQL (gen_random_uuid): evita SecureRandom en hilos reactivos
        return Mono.fromSupplier(() -> UsuarioPersistenceMapper.toEntity(usuario))
                .doOnNext(entity -> log.info("Guardando usuario rol={}", usuario.rol()))
                .flatMap(template::insert)
                .map(UsuarioPersistenceMapper::toDomain)
                .doOnNext(guardado -> log.info("Usuario guardado id={}", guardado.id()))
                .onErrorMap(DataIntegrityViolationException.class, this::traducirIntegridad);
    }

    @Override
    public Mono<Boolean> existePorEmail(String email) {
        return template.exists(Query.query(Criteria.where("email").is(email).ignoreCase(true)), UsuarioEntity.class);
    }

    @Override
    public Mono<Boolean> existePorDocumentoIdentidad(String documentoIdentidad) {
        return template.exists(Query.query(Criteria.where("documento_identidad").is(documentoIdentidad)),
                UsuarioEntity.class);
    }

    private Throwable traducirIntegridad(DataIntegrityViolationException error) {
        String detalle = String.valueOf(error.getMessage());
        if (detalle.contains(CONSTRAINT_EMAIL)) {
            return new UsuarioDuplicadoException("correo electronico");
        }
        if (detalle.contains(CONSTRAINT_DOCUMENTO)) {
            return new UsuarioDuplicadoException("documento de identidad");
        }
        return error;
    }
}
