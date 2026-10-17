package com.crediya.autenticacion.api.exception;

import com.crediya.autenticacion.model.common.exceptions.AccesoDenegadoException;
import com.crediya.autenticacion.model.usuario.exceptions.CredencialesInvalidasException;
import com.crediya.autenticacion.model.usuario.exceptions.DatosUsuarioInvalidosException;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioDuplicadoException;
import com.crediya.autenticacion.model.usuario.exceptions.UsuarioNoEncontradoException;
import com.crediya.autenticacion.model.common.exceptions.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCatalog")
class ErrorCatalogTest {

    @Test
    @DisplayName("debe mapear cada codigo de dominio conocido a su estado HTTP")
    void debeMapearCodigosConocidos() {
        assertThat(ErrorCatalog.estadoDe(DatosUsuarioInvalidosException.CODE)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCatalog.estadoDe(UsuarioDuplicadoException.CODE)).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCatalog.estadoDe(UsuarioNoEncontradoException.CODE)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCatalog.estadoDe(CredencialesInvalidasException.CODE)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCatalog.estadoDe(AccesoDenegadoException.CODE)).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("cuando el codigo de dominio no esta catalogado, debe responder 422")
    void cuandoCodigoDesconocido_debeResponder422() {
        assertThat(ErrorCatalog.estadoDe("CODIGO_NUEVO")).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("cada excepcion de dominio del modelo debe estar catalogada para no caer en el 422 por defecto")
    void cadaExcepcionDeDominio_debeEstarCatalogada() {
        var escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new AssignableTypeFilter(DomainException.class));

        assertThat(escaner.findCandidateComponents("com.crediya.autenticacion.model"))
                .isNotEmpty()
                .allSatisfy(definicion -> assertThat(ErrorCatalog.estaCatalogado(codigoDe(definicion)))
                        .as("codigo de %s sin estado HTTP en ErrorCatalog", definicion.getBeanClassName())
                        .isTrue());
    }

    private static String codigoDe(BeanDefinition definicion) {
        try {
            return (String) Class.forName(definicion.getBeanClassName()).getField("CODE").get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("La excepcion " + definicion.getBeanClassName() + " debe declarar un CODE publico y estatico", e);
        }
    }
}
