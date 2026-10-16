# CrediYa - Microservicio de Autenticacion

Registro de usuarios y autenticacion de la plataforma CrediYa. Arquitectura hexagonal / Clean Architecture
con [Scaffold de Bancolombia](https://bancolombia.github.io/scaffold-clean-architecture/) 3.25.0, Spring Boot 3.5 y WebFlux
(flujo reactivo de punta a punta: WebFlux -> casos de uso -> R2DBC).

## Estado de las historias de usuario

| HU | Descripcion | Rama | Estado |
|----|-------------|------|--------|
| HU1 | Registrar usuarios (`POST /api/v1/usuarios`) | `feature/HU-01-registro-usuarios` | Hecha |
| HU2 | Consulta de usuario por documento (`GET /api/v1/usuarios/{documento}`), usada por SOLICITUDES | `feature/HU-02-consulta-usuario` | Hecha |
| HU5 | Contenerizacion: Dockerfile y docker-compose | `feature/HU-05-contenerizacion` | Hecha |
| HU4 | Consulta de varios usuarios por correo (`GET /api/v1/usuarios?correos=`), usada por SOLICITUDES para el listado | `feature/HU-04-consulta-por-correos` | Hecha |
| HU3 | Login y JWT (`POST /api/v1/login`) y autorizacion por rol | `feature/HU-03-autenticacion` | Hecha |

## Estructura

```
domain/model      entidades, comandos, excepciones de dominio y puertos (gateways). Sin Spring ni slf4j
domain/usecase    casos de uso (flujo reactivo). Solo depende de model
infrastructure/
  driven-adapters/r2dbc-postgresql   persistencia R2DBC + migraciones Flyway
  entry-points/reactive-web          router funcional, handlers, DTO (records), MapStruct, OpenAPI, errores
applications/app-service             ensamblado, configuracion y main
```

Reglas del enunciado que se cumplen: el dominio no tiene `spring-boot-dependencies` ni `slf4j-api`; los logs viven en
infraestructura; los errores de dominio llevan un codigo y `GlobalErrorHandler` (extiende `AbstractErrorWebExceptionHandler`)
los traduce a HTTP; la documentacion OpenAPI esta en una clase utilitaria (`UsuarioOpenApi`), no en anotaciones.

## Requisitos

- Java 21
- Docker (para PostgreSQL local y para los tests de integracion con Testcontainers)

## Ejecutar localmente

```bash
# 1. PostgreSQL (o usa docker compose, ver abajo)
docker run -d --name crediya-auth-db -e POSTGRES_DB=crediya_auth -e POSTGRES_USER=crediya \
  -e POSTGRES_PASSWORD=cambia-esta-clave -p 5432:5432 postgres:16-alpine

# 2. Variables de entorno (ver .env.example) y arranque
export DB_USERNAME=crediya DB_PASSWORD=cambia-esta-clave BOOTSTRAP_ADMIN_PASSWORD=cambia-esta-clave-admin
./gradlew bootRun
```

Swagger UI: http://localhost:8080/swagger-ui.html - Health: http://localhost:8080/actuator/health

## Autenticacion y roles (HU3)

`POST /api/v1/login` con `correo_electronico` y `password` devuelve un JWT firmado con **RS256**
(`access_token`, `token_type`, `expires_in`). La clave publica se publica en `GET /.well-known/jwks.json` para que otros
servicios (SOLICITUDES) verifiquen los tokens **sin compartir ningun secreto**.

| Endpoint | Quien puede |
|----------|-------------|
| `POST /api/v1/login`, `GET /.well-known/jwks.json`, `/actuator/health`, `/v3/api-docs`, Swagger UI | Publico |
| `POST /api/v1/usuarios` | `ADMIN` y `ASESOR`; solo un `ADMIN` puede crear `ADMIN` o `ASESOR` |
| `GET /api/v1/usuarios/{documento}` | Cualquier autenticado; un `CLIENTE` solo puede consultar su propio documento |
| `GET /api/v1/usuarios?correos=a@x.co,b@y.co` | Solo `ADMIN` y `ASESOR` (expone datos de terceros); maximo 50 correos por consulta |

Decisiones de seguridad:
- Las claves se guardan solo como hash **bcrypt** (calculado fuera de los hilos reactivos). La clave tiene 8..72 caracteres
  (bcrypt solo usa 72 bytes: se rechaza en vez de truncar en silencio).
- Usuario inexistente y clave incorrecta devuelven la **misma respuesta 401** y un costo de calculo similar (no se puede
  averiguar que cuentas existen). Los intentos son ilimitados, como pide la HU3 (no hay bloqueo de cuenta).
- Los errores 401/403 usan el mismo formato JSON que el resto de la API y no revelan el motivo del rechazo.
- Ningun `toString` imprime claves, hashes ni tokens.

### Claves y primer administrador

```bash
export JWT_PRIVATE_KEY=$(./scripts/generate-jwt-key.sh)   # en produccion: gestor de secretos
export BOOTSTRAP_ADMIN_PASSWORD=cambia-esta-clave-admin   # crea el primer ADMIN si no existe ninguno
```

Sin `JWT_PRIVATE_KEY` se genera una clave efimera (los tokens no sobreviven a un reinicio ni sirve con varias replicas).
Los usuarios creados en HU1 antes de HU3 no tienen clave y no pueden iniciar sesion hasta que se vuelvan a registrar.

## Registrar un usuario (HU1)

```bash
# TOKEN=<access_token de un ADMIN o ASESOR>
curl -i -X POST http://localhost:8080/api/v1/usuarios -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{
  "nombres": "Maria", "apellidos": "Gomez", "documento_identidad": "1020304050",
  "fecha_nacimiento": "1995-03-20", "direccion": "Calle 10", "telefono": "+573001234567",
  "correo_electronico": "maria@example.com", "salario_base": 4500000, "password": "Clave-Segura-123"
}'
```

| Codigo | Cuando |
|--------|--------|
| 201 | Usuario registrado |
| 400 | Campos vacios, correo invalido, clave de largo invalido o salario fuera de 0..15000000 |
| 401 | Falta el token o no es valido |
| 403 | El rol no permite la accion (p. ej. un `CLIENTE`, o un `ASESOR` creando personal interno) |
| 409 | Correo o documento ya registrados |
| 500 | Error inesperado (mensaje generico, sin detalles internos) |

## Docker (HU5)

```bash
./scripts/init-env.sh                       # una vez: crea .env con credenciales y clave JWT aleatorias
docker compose --env-file .env -f deployment/docker-compose.yml up --build
```

Levanta autenticacion (http://localhost:8081) y su PostgreSQL. La base vive en el volumen `db-data`: sobrevive a
`docker compose down` (solo `down -v` la borra). El administrador inicial es `BOOTSTRAP_ADMIN_EMAIL` /
`BOOTSTRAP_ADMIN_PASSWORD` de `.env`.

La imagen (`deployment/Dockerfile`) es multi-stage: se compila con el JDK y solo el JRE y el jar llegan a la imagen final, que
corre como usuario sin privilegios (uid 10001) con `java` como PID 1 (recibe SIGTERM y apaga de forma ordenada). El contenedor
va con sistema de archivos de solo lectura, sin capacidades de Linux y con limite de memoria. Las versiones de imagen estan fijadas.

## Pruebas y calidad

```bash
./gradlew check                 # tests + estructura + cobertura minima 90% en model y usecase
./gradlew jacocoMergedReport    # reporte consolidado: build/reports/jacocoMergedReport/
./gradlew jacocoMergedReport -PrunPitest   # ademas, mutation testing (lento, opt-in)
```

Notas de entorno: Testcontainers 1.21.x necesita `api.version=1.44` con Docker Engine 29+ (ya configurado en `main.gradle`;
se puede cambiar con `-PdockerApiVersion=...`).

## CI/CD (GitHub Actions)

`.github/workflows/ci.yml` corre en cada PR y en cada push a `develop` o `main`. Sus tres jobs se llaman exactamente como los exigen las
rulesets de la rama (no los renombres sin actualizar la ruleset):

| Job (check requerido) | Que valida |
|-----------------------|------------|
| **Lint** | `hadolint` sobre el Dockerfile, `shellcheck` sobre los scripts, `docker compose config` y `validateStructure` (el dominio no depende de infraestructura) |
| **Calidad y build** | `./gradlew build jacocoMergedReport`: compila, corre las pruebas unitarias y de integracion (Testcontainers) y exige 90% de cobertura en el dominio; publica los reportes como artefacto y la cobertura en el resumen del job |
| **Imagen Docker** | Construye la imagen, comprueba que no corra como root y, solo al integrar en `develop`/`main`, la publica en Amazon ECR (`crediya/<servicio>`, autenticando con OIDC) con una etiqueta unica `sha-<corto>` |

`develop` solo admite PR con *squash* y `main` solo con *merge*; ambas exigen los tres checks en verde.

## Flujo de trabajo (gitflow)

`main` (estable) <- `develop` <- `feature/HU-XX-nombre` (una rama por HU, se integra a `develop` con `--no-ff`).
