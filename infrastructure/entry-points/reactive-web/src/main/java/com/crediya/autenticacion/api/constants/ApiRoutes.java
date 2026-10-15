package com.crediya.autenticacion.api.constants;

public final class ApiRoutes {

    public static final String API_V1 = "/api/v1";
    public static final String USUARIOS = API_V1 + "/usuarios";
    public static final String USUARIO_POR_DOCUMENTO = USUARIOS + "/{documento}";

    private ApiRoutes() {
    }
}
