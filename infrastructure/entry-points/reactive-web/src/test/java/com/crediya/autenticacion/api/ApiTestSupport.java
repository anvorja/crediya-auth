package com.crediya.autenticacion.api;

import com.crediya.autenticacion.api.autenticacion.AutenticacionHandler;
import com.crediya.autenticacion.api.exception.GlobalErrorHandler;
import com.crediya.autenticacion.api.usuario.UsuarioHandler;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;

/**
 * Arma un cliente de pruebas sobre el router real con el manejador global de errores.
 */
public final class ApiTestSupport {

    private ApiTestSupport() {
    }

    public static WebTestClient cliente(UsuarioHandler usuarioHandler, AutenticacionHandler autenticacionHandler) {
        var errorHandler = new GlobalErrorHandler(new DefaultErrorAttributes(), new WebProperties(),
                new StaticApplicationContext(), ServerCodecConfigurer.create());
        // HandlerStrategies.builder() ya registra un manejador de ResponseStatusException que responderia antes que
        // el nuestro; en la aplicacion real el manejador global se ejecuta primero (@Order(-2)).
        var estrategias = HandlerStrategies.empty()
                .codecs(configurer -> configurer.registerDefaults(true))
                .exceptionHandler(errorHandler)
                .build();
        return WebTestClient.bindToRouterFunction(new RouterRest().routerFunction(usuarioHandler, autenticacionHandler))
                .handlerStrategies(estrategias)
                .build();
    }
}
