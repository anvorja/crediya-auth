# CrediYa - Microservicio de Autenticacion

Registro de usuarios y autenticacion de la plataforma CrediYa. Arquitectura hexagonal / Clean Architecture
con [Scaffold de Bancolombia](https://bancolombia.github.io/scaffold-clean-architecture/) 3.25.0, Spring Boot 3.5 y WebFlux
(flujo reactivo de punta a punta: WebFlux -> casos de uso -> R2DBC).

## Estado de las historias de usuario

| HU | Descripcion | Rama | Estado |
|----|-------------|------|--------|
| HU1 | Registrar usuarios (`POST /api/v1/usuarios`) | `feature/HU-01-registro-usuarios` | Hecha |
| HU3 | Login y JWT (`POST /api/v1/login`) | `feature/HU-03-autenticacion` | Pendiente |

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
# 1. PostgreSQL
docker run -d --name crediya-auth-db -e POSTGRES_DB=crediya_auth -e POSTGRES_USER=crediya \
  -e POSTGRES_PASSWORD=cambia-esta-clave -p 5432:5432 postgres:16-alpine

# 2. Variables de entorno (ver .env.example) y arranque
export DB_USERNAME=crediya DB_PASSWORD=cambia-esta-clave
./gradlew bootRun
```

Swagger UI: http://localhost:8080/swagger-ui.html - Health: http://localhost:8080/actuator/health

## Registrar un usuario (HU1)

```bash
curl -i -X POST http://localhost:8080/api/v1/usuarios -H 'Content-Type: application/json' -d '{
  "nombres": "Maria", "apellidos": "Gomez", "documento_identidad": "1020304050",
  "fecha_nacimiento": "1995-03-20", "direccion": "Calle 10", "telefono": "+573001234567",
  "correo_electronico": "maria@example.com", "salario_base": 4500000
}'
```

| Codigo | Cuando |
|--------|--------|
| 201 | Usuario registrado |
| 400 | Campos vacios, correo invalido o salario fuera de 0..15000000 |
| 409 | Correo o documento ya registrados |
| 500 | Error inesperado (mensaje generico, sin detalles internos) |

## Pruebas y calidad

```bash
./gradlew check                 # tests + estructura + cobertura minima 90% en model y usecase
./gradlew jacocoMergedReport    # reporte consolidado: build/reports/jacocoMergedReport/
./gradlew jacocoMergedReport -PrunPitest   # ademas, mutation testing (lento, opt-in)
```

Notas de entorno: Testcontainers 1.21.x necesita `api.version=1.44` con Docker Engine 29+ (ya configurado en `main.gradle`;
se puede cambiar con `-PdockerApiVersion=...`).

## Flujo de trabajo (gitflow)

`main` (estable) <- `develop` <- `feature/HU-XX-nombre` (una rama por HU, se integra a `develop` con `--no-ff`).
