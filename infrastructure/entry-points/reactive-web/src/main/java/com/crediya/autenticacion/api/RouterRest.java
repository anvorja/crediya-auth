package com.crediya.autenticacion.api;

import com.crediya.autenticacion.api.autenticacion.AutenticacionHandler;
import com.crediya.autenticacion.api.constants.ApiRoutes;
import com.crediya.autenticacion.api.documentation.AutenticacionOpenApi;
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
    public RouterFunction<ServerResponse> routerFunction(UsuarioHandler usuarioHandler,
                                                         AutenticacionHandler autenticacionHandler) {
        return route()
                .POST(ApiRoutes.USUARIOS, accept(MediaType.APPLICATION_JSON),
                        usuarioHandler::registrarUsuario, UsuarioOpenApi::registrarUsuario)
                .POST(ApiRoutes.LOGIN, accept(MediaType.APPLICATION_JSON),
                        autenticacionHandler::login, AutenticacionOpenApi::login)
                .GET(ApiRoutes.JWKS, autenticacionHandler::clavesPublicas, AutenticacionOpenApi::clavesPublicas)
                .GET(ApiRoutes.USUARIO_POR_DOCUMENTO, accept(MediaType.APPLICATION_JSON),
                        usuarioHandler::consultarPorDocumento, UsuarioOpenApi::consultarPorDocumento)
                .build();
    }
}
