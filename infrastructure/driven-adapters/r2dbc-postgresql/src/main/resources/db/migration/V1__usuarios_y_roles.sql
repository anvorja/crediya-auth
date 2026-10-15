-- HU1: modelo relacional del microservicio de autenticacion (ver enunciado: tablas Usuario y Rol)

CREATE TABLE rol (
    id_rol      SMALLINT     PRIMARY KEY,
    nombre      VARCHAR(30)  NOT NULL UNIQUE,
    descripcion VARCHAR(200)
);

INSERT INTO rol (id_rol, nombre, descripcion) VALUES
    (1, 'ADMIN',   'Administrador del sistema'),
    (2, 'ASESOR',  'Asesor que revisa las solicitudes de credito'),
    (3, 'CLIENTE', 'Solicitante de credito');

CREATE TABLE usuario (
    id_usuario          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre              VARCHAR(100)  NOT NULL,
    apellido            VARCHAR(100)  NOT NULL,
    documento_identidad VARCHAR(20)   NOT NULL,
    fecha_nacimiento    DATE,
    direccion           VARCHAR(255),
    telefono            VARCHAR(20),
    email               VARCHAR(255)  NOT NULL,
    salario_base        NUMERIC(15,2) NOT NULL,
    id_rol              SMALLINT      NOT NULL REFERENCES rol (id_rol),
    creado_en           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_salario_base CHECK (salario_base >= 0 AND salario_base <= 15000000)
);

CREATE UNIQUE INDEX uq_usuario_email ON usuario (lower(email));
CREATE UNIQUE INDEX uq_usuario_documento ON usuario (documento_identidad);
CREATE INDEX ix_usuario_id_rol ON usuario (id_rol);
