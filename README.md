# Bitácora del curso — Backend

Aplicación **Spring Boot 4.1** (Java 17) que demuestra control de acceso por roles. Incluye una app web **Thymeleaf** (bitácora + login por sesión) y, en construcción, una **API REST con autenticación JWT** respaldada por **Postgres**.

> Los usuarios ya **no** son in-memory: se persisten en Postgres vía JPA. Todo el ecosistema (backend + base de datos) se levanta con **Docker Compose**: un solo comando compila la app y arranca Postgres.

## Stack

| Pieza | Detalle |
|---|---|
| Lenguaje / build | Java 17, Maven (wrapper `./mvnw`) |
| Framework | Spring Boot 4.1.0 (Web MVC, Security 6, Data JPA, Validation) |
| Vistas | Thymeleaf + Bootstrap 5.3.3 (CDN) |
| i18n | `messages*.properties` (es/en/pt), switch con `?lang=` |
| Base de datos | PostgreSQL 16 |
| Auth | Form login por sesión (web) · JWT stateless para `/api/**` *(en progreso)* |

## Requisitos

- **Docker** con Compose v2 — es lo único imprescindible.
- JDK 17+ solo si quieres correr el backend fuera del contenedor (modo desarrollo).

## Puesta en marcha

### Opción A — Todo en Docker (recomendada)

Compila el backend dentro de la imagen y levanta Postgres:

```bash
docker compose -f docker/docker-compose.yml up --build
```

App en **http://localhost:8080**. No necesitas Java ni Maven en tu máquina.

En el primer arranque:

- El backend espera a que Postgres pase su *healthcheck* antes de conectarse.
- Hibernate crea las tablas `users` y `user_roles` (`ddl-auto=update`).
- `DataSeeder` siembra los usuarios de demo **si la tabla está vacía** (idempotente).

Todos los comandos se ejecutan **desde la raíz del proyecto** (donde está `pom.xml`).

Comandos habituales:

```bash
docker compose -f docker/docker-compose.yml up --build        # compilar y arrancar
docker compose -f docker/docker-compose.yml up --build -d     # igual, en background
docker compose -f docker/docker-compose.yml build             # solo compilar la imagen
docker compose -f docker/docker-compose.yml build --no-cache  # recompilar desde cero
docker compose -f docker/docker-compose.yml logs -f backend   # ver logs
docker compose -f docker/docker-compose.yml down              # parar (conserva datos)
docker compose -f docker/docker-compose.yml down -v           # parar y BORRAR la BD
```

`--build` recompila la imagen antes de arrancar: úsalo siempre que cambies código Java o el `pom.xml`. Si el stack ya está arriba, Compose recrea los contenedores solo, sin necesidad de `down` previo.

Los datos viven en el volumen `bitacora_pgdata`: sobreviven a `down` y a reconstruir la imagen.

> **¿Puerto 5432 ocupado** por otro Postgres local? Cambia solo el puerto del host:
> `POSTGRES_HOST_PORT=5433 docker compose -f docker/docker-compose.yml up --build`

### Opción B — Solo Postgres en Docker (desarrollo)

Para iterar rápido sin reconstruir la imagen en cada cambio:

```bash
docker compose -f docker/docker-compose.yml up -d postgres   # solo la BD
./mvnw spring-boot:run                                        # backend en el host
```

Funciona sin configurar nada: los valores por defecto apuntan a `localhost:5432`.

### Verificar

- **http://localhost:8080/public** — la bitácora (pública).
- **http://localhost:8080/login** — entra con un usuario de demo.
- Inspeccionar la BD: `docker exec -it bitacora-postgres psql -U bitacora -d bitacora`

## Usuarios de demo (seed)

| Usuario | Contraseña | Roles |
|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN`, `ROLE_USER` |
| `user`  | `user123`  | `ROLE_USER` |

## Rutas web (Thymeleaf)

| Ruta | Acceso | Descripción |
|---|---|---|
| `/public` | público | Bitácora (hero + entradas + acerca de) |
| `/login` | público | Formulario de login |
| `/user` | `USER` o `ADMIN` | Zona de usuario |
| `/admin` | `ADMIN` | Zona de administración |
| `/403` | — | Acceso denegado |

Cambia idioma con `?lang=es|en|pt`.

## API REST *(en progreso — spec backend 02)*

| Método | Ruta | Acceso | Estado |
|---|---|---|---|
| POST | `/api/auth/register` | público | ⏳ pendiente |
| POST | `/api/auth/login` | público | ⏳ pendiente |
| GET | `/api/me` | autenticado (JWT) | ⏳ pendiente |
| GET | `/api/posts` | público | ⏳ pendiente |

## Comandos

```bash
# Docker (todo el ecosistema)
docker compose -f docker/docker-compose.yml up --build   # compilar + levantar backend y BD
docker compose -f docker/docker-compose.yml up -d postgres  # solo la BD
docker compose -f docker/docker-compose.yml down         # parar (conserva el volumen)

# Maven (backend en el host)
./mvnw spring-boot:run           # arrancar en :8080
./mvnw clean package -DskipTests # construir jar (target/)
./mvnw package                   # construir + tests (requiere Postgres, ver nota)
./mvnw clean                     # limpiar target/
```

> **Nota tests:** `DemoApplicationTests.contextLoads` levanta el contexto completo, por lo que **requiere Postgres vivo**. Sin base de datos usa `-DskipTests`. Pendiente: perfil de test con H2 o Testcontainers.

## Configuración

Valores por defecto en `src/main/resources/application.properties`, sobreescribibles por variables de entorno (útil en Docker):

| Variable | Default | Descripción |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/bitacora` | URL JDBC de Postgres |
| `SPRING_DATASOURCE_USERNAME` | `bitacora` | Usuario de la BD |
| `SPRING_DATASOURCE_PASSWORD` | `bitacora` | Contraseña de la BD |
| `POSTGRES_HOST_PORT` | `5432` | Puerto del **host** donde se publica Postgres (solo Compose) |

Dentro de Compose el host de la BD es `postgres` (nombre del servicio), no `localhost`; el `docker-compose.yml` ya inyecta esas variables.

Ejemplo con credenciales propias:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/midb \
SPRING_DATASOURCE_USERNAME=miuser \
SPRING_DATASOURCE_PASSWORD=mipass \
./mvnw spring-boot:run
```

## Estructura y hoja de ruta

- Código y arquitectura: ver [`CLAUDE.md`](./CLAUDE.md).
- **Estado actual:** usuarios en Postgres vía JPA y ecosistema dockerizado; el front sigue siendo Thymeleaf con form login.
- **Siguientes pasos:**
  1. Autenticación JWT + API REST bajo `/api/**`, conviviendo con el form login actual.
  2. Migración del front a Next.js consumiendo esa API (los templates Thymeleaf se mantienen hasta entonces).
  3. Perfil de test (H2 o Testcontainers) para que `./mvnw test` no dependa de un Postgres externo.
