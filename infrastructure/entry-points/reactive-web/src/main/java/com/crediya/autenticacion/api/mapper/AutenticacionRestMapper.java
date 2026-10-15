package com.crediya.autenticacion.api.mapper;

import com.crediya.autenticacion.api.dto.request.LoginRequest;
import com.crediya.autenticacion.api.dto.response.TokenResponse;
import com.crediya.autenticacion.model.seguridad.Token;
import com.crediya.autenticacion.model.usuario.commands.LoginCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AutenticacionRestMapper {

    @Mapping(source = "correoElectronico", target = "email")
    LoginCommand toCommand(LoginRequest request);

    @Mapping(source = "valor", target = "accessToken")
    @Mapping(source = "tipo", target = "tokenType")
    @Mapping(source = "expiraEnSegundos", target = "expiresIn")
    TokenResponse toResponse(Token token);
}
