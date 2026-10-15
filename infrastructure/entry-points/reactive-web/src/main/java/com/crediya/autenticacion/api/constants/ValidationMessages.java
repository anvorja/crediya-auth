package com.crediya.autenticacion.api.constants;

public final class ValidationMessages {

    public static final String NOMBRES_REQUERIDOS = "Los nombres son obligatorios";
    public static final String APELLIDOS_REQUERIDOS = "Los apellidos son obligatorios";
    public static final String DOCUMENTO_REQUERIDO = "El documento de identidad es obligatorio";
    public static final String EMAIL_REQUERIDO = "El correo electronico es obligatorio";
    public static final String EMAIL_FORMATO = "El correo electronico no tiene un formato valido";
    public static final String SALARIO_REQUERIDO = "El salario base es obligatorio";
    public static final String SALARIO_MINIMO = "El salario base no puede ser negativo";
    public static final String SALARIO_MAXIMO = "El salario base no puede superar 15000000";
    public static final String CUERPO_REQUERIDO = "El cuerpo de la peticion es obligatorio";
    public static final String CAMPO_CUERPO = "cuerpo";

    private ValidationMessages() {
    }
}
