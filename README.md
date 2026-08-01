# Bitácora del curso — Backend

Aplicación **Spring Boot 4.1** (Java 17) que demuestra control de acceso por roles. Incluye una app web **Thymeleaf** (bitácora + login por sesión) y, en construcción, una **API REST con autenticación JWT** respaldada por **Postgres**.

> Los usuarios ya **no** son in-memory: se persisten en Postgres vía JPA. El contenedor de Postgres lo monta el usuario (ver abajo).

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

- JDK 17+ (probado con 21)
- Docker (para Postgres)
- No hace falta Maven instalado: usa el wrapper `./mvnw`

## Puesta en marcha

### 1. Levantar Postgres

El backend espera una base `bitacora` en `localhost:5432`. Levántala con Docker:

```bash
docker run --name bitacora-pg \
  -e POSTGRES_DB=bitacora \
  -e POSTGRES_USER=bitacora \
  -e POSTGRES_PASSWORD=bitacora \
  -p 5432:5432 -d postgres:16
```

> ¿Usas otras credenciales? Sobreescribe con variables de entorno (ver [Configuración](#configuración)).

### 2. Arrancar el backend

```bash
./mvnw spring-boot:run
```

App en **http://localhost:8080**. Al primer arranque:

- Hibernate crea las tablas `users` y `user_roles` (`ddl-auto=update`).
- `DataSeeder` siembra los usuarios de demo **si la tabla está vacía** (idempotente).

### 3. Verificar

- Abre **http://localhost:8080/public** — la bitácora (pública).
- Entra en **http://localhost:8080/login** con un usuario de demo.

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

Ejemplo con credenciales propias:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/midb \
SPRING_DATASOURCE_USERNAME=miuser \
SPRING_DATASOURCE_PASSWORD=mipass \
./mvnw spring-boot:run
```

## Estructura y hoja de ruta

- Código y arquitectura: ver [`CLAUDE.md`](./CLAUDE.md).
- Plan de trabajo por dominios: ver [`.claude/spec/`](./.claude/spec/).
  - **Backend:** `01` Postgres+usuarios ✅ · `02` JWT+API REST ⏳ · `03` Docker ⏳
  - **Frontend:** migración a Next.js (solo documentado; el front Thymeleaf sigue vivo).
