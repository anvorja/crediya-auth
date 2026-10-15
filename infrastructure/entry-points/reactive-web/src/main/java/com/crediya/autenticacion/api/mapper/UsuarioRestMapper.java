package com.crediya.autenticacion.api.mapper;

import com.crediya.autenticacion.api.dto.request.RegistrarUsuarioRequest;
import com.crediya.autenticacion.api.dto.response.UsuarioResponse;
import com.crediya.autenticacion.model.usuario.Usuario;
import com.crediya.autenticacion.model.usuario.commands.RegistrarUsuarioCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioRestMapper {

    @Mapping(source = "correoElectronico", target = "email")
    RegistrarUsuarioCommand toCommand(RegistrarUsuarioRequest request);

    @Mapping(source = "email", target = "correoElectronico")
    UsuarioResponse toResponse(Usuario usuario);
}
