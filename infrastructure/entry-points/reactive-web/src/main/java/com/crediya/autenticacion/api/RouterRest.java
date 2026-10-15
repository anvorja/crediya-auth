package com.crediya.autenticacion.api;

import com.crediya.autenticacion.api.constants.ApiRoutes;
import com.crediya.autenticacion.api.documentation.UsuarioOpenApi;
import com.crediya.autenticacion.api.usuario.UsuarioHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration
public class RouterRest {

    @Bean
    public RouterFunction<ServerResponse> routerFunction(UsuarioHandler usuarioHandler) {
        return route()
                .POST(ApiRoutes.USUARIOS, accept(MediaType.APPLICATION_JSON),
                        usuarioHandler::registrarUsuario, UsuarioOpenApi::registrarUsuario)
                .GET(ApiRoutes.USUARIO_POR_DOCUMENTO, accept(MediaType.APPLICATION_JSON),
                        usuarioHandler::consultarPorDocumento, UsuarioOpenApi::consultarPorDocumento)
                .build();
    }
}
