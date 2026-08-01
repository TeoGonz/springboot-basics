# Bitácora del curso — Backend

**API REST** en **Spring Boot 4.1** (Java 17) que demuestra control de acceso por roles, respaldada por **Postgres**.

> Esta aplicación **no sirve páginas**: solo responde JSON y se prueba con Postman o `curl`. El frontend es un proyecto Next.js aparte (`front-react-project/`, repositorio hermano).

> Los usuarios se persisten en Postgres vía JPA. Todo el ecosistema (backend + base de datos) se levanta con **Docker Compose**: un solo comando compila la app y arranca Postgres.

## Stack

| Pieza | Detalle |
|---|---|
| Lenguaje / build | Java 17, Maven (wrapper `./mvnw`) |
| Framework | Spring Boot 4.1.0 (Web MVC, Security 7, Data JPA, Validation, Mail) |
| Base de datos | PostgreSQL 16 |
| Auth | JWT stateless (HS256, `Authorization: Bearer …`) |
| Correo | SMTP; en desarrollo **Mailpit** (bandeja web en `:8025`) |

## Requisitos

- **Docker** con Compose v2 — es lo único imprescindible.
- JDK 17+ solo si quieres correr el backend fuera del contenedor (modo desarrollo).

## Puesta en marcha

### Opción A — Todo en Docker (recomendada)

Compila el backend dentro de la imagen y levanta Postgres y Mailpit:

```bash
docker compose -f docker/docker-compose.yml up --build
```

App en **http://localhost:8080**; bandeja de correo en **http://localhost:8025**. No necesitas Java ni Maven en tu máquina.

En el primer arranque:

- El backend espera a que Postgres pase su *healthcheck* antes de conectarse.
- Hibernate crea las tablas `users`, `user_roles` y `password_reset_token` (`ddl-auto=update`).
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
docker compose -f docker/docker-compose.yml up -d postgres mailpit   # BD + correo
./mvnw spring-boot:run                                                # backend en el host
```

Funciona sin configurar nada: los valores por defecto apuntan a `localhost:5432` y a `localhost:1025` (SMTP).

### Verificar

```bash
# 401: sin token
curl -i localhost:8080/api/me

# login -> token, y llamada autenticada
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"user123"}' | jq -r .accessToken)
curl -s localhost:8080/api/me -H "Authorization: Bearer $TOKEN"
# {"username":"user","enabled":true,"roles":["ROLE_USER"]}

# alta de usuario
curl -i -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"nuevo","email":"nuevo@x.com","password":"secreta123"}'

# recuperación: pide el enlace y léelo en http://localhost:8025
curl -i -X POST localhost:8080/api/auth/forgot-password -H 'Content-Type: application/json' \
  -d '{"email":"user@bitacora.local","locale":"es"}'
```

Inspeccionar la BD: `docker exec -it bitacora-postgres psql -U bitacora -d bitacora`

## Usuarios de demo (seed)

| Usuario | Contraseña | Roles |
|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN`, `ROLE_USER` |
| `user`  | `user123`  | `ROLE_USER` |

> `DataSeeder` escribe directo en la base y no pasa por la validación de los DTO, así que `user123` existe aunque tenga menos de los 8 caracteres que exige el registro. Son credenciales de desarrollo.

## API REST

| Método | Ruta | Acceso | Cuerpo | Respuesta |
|---|---|---|---|---|
| POST | `/api/auth/register` | público | `{username, email, password}` | 201 + `{id, username, email, roles}` |
| POST | `/api/auth/login` | público | `{username, password}` | 200 + `{accessToken, tokenType, expiresInMs, username, roles}` |
| POST | `/api/auth/forgot-password` | público | `{email, locale?}` | 202, sin cuerpo |
| POST | `/api/auth/reset-password` | público | `{token, password}` | 204 |
| GET | `/api/me` | autenticado | — | `{username, enabled, roles}` |
| GET | `/api/posts` | público | — | ⏳ pendiente |

Reglas de acceso: `/api/auth/**` es público (es lo que hay que poder llamar sin token), `/api/admin/**` exige `ROLE_ADMIN`, y todo lo demás requiere autenticación.

No quedan rutas web: `/public`, `/login`, `/user`, `/admin` y `/403` ya no existen. Una petición autenticada a cualquiera de ellas devuelve 404; una anónima, 401.

### Autenticación

**JWT stateless.** `POST /api/auth/login` devuelve un token HS256 que se manda en cada petición:

```
Authorization: Bearer <accessToken>
```

Sin sesión, sin cookies y con CSRF desactivado (no hay formularios ni cookies que falsificar). El token dura una hora y **no se puede revocar** antes: es lo que cuesta no guardar estado. HTTP Basic ya no está habilitado.

En Postman: pestaña *Authorization* → tipo *Bearer Token* → pegar el `accessToken` del login.

Tampoco hay CORS configurado, y no es un olvido: al front lo sirve Next, que llama a esta API desde su propio servidor. El navegador nunca hace la petición, así que no hay origen cruzado que autorizar.

### Recuperación de contraseña

1. `POST /api/auth/forgot-password` con el correo. Responde **202 siempre**, exista la cuenta o no: contestar distinto convertiría el endpoint en un buscador de correos registrados.
2. Si la cuenta existe, llega un correo con un enlace al front (`/{idioma}/reset-password?token=…`) que caduca en 30 minutos. En desarrollo el correo se lee en **http://localhost:8025** (Mailpit); nada sale de la máquina.
3. `POST /api/auth/reset-password` con el token y la contraseña nueva. El token es **de un solo uso**, y pedir un enlace nuevo invalida los anteriores.

En la base de datos solo se guarda el **hash SHA-256** del token: quien lea la tabla no puede reconstruir el enlace que se envió.

#### Dónde llega el correo

Por defecto, **a Mailpit y a ningún sitio más**. Mailpit es un buzón trampa: acepta cualquier destinatario y no entrega nada fuera de la máquina, así que pedir veinte enlaces de prueba no molesta a nadie. Se lee en **http://localhost:8025**, y si prefieres la terminal:

```bash
curl -s 'localhost:8025/api/v1/messages?limit=1'    # lista
curl -s 'localhost:8025/api/v1/message/<ID>'        # cuerpo, con el enlace
```

Un correo enviado a tu dirección real **no aparecerá en tu bandeja**: está en Mailpit. Eso es lo esperado, no un fallo.

#### Enviar a un buzón real

Es solo configuración; el código no cambia.

```bash
cp docker/.env.example docker/.env      # docker/.env está gitignorado
```

Y en él, el bloque de Gmail:

```dotenv
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu.cuenta@gmail.com
MAIL_PASSWORD=xxxxxxxxxxxxxxxx
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
APP_MAIL_FROM=tu.cuenta@gmail.com
```

Después, `docker compose -f docker/docker-compose.yml up -d --build backend`.

Tres detalles que hacen fallar el envío si se pasan por alto:

- `MAIL_PASSWORD` es una **contraseña de aplicación**, no la de tu cuenta. Se genera en [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords) y exige tener activada la verificación en dos pasos. Google la muestra en cuatro bloques de cuatro; los espacios son decorativos, se pega seguida.
- `APP_MAIL_FROM` tiene que ser **la misma dirección** que `MAIL_USERNAME`: Gmail solo permite enviar como la cuenta autenticada.
- La contraseña de aplicación permite enviar correo en tu nombre. Va en `docker/.env`, que git ignora, y **no debe acabar en un archivo versionado**. Si alguna vez se filtra, hay que revocarla en Google: borrarla del archivo no la desactiva.

Si el envío falla, el usuario ya recibió su 202 (por diseño), así que el motivo está solo en el log: `docker compose -f docker/docker-compose.yml logs backend | grep -i mail`. Un `MailAuthenticationException` es contraseña de aplicación incorrecta o dos pasos sin activar.

### Errores

Todos comparten la misma forma, con un código estable pensado para que el front elija el texto traducido:

```json
{ "status": 409, "error": "USERNAME_TAKEN", "message": "username already registered" }
```

`message` es para quien depura. Los fallos de validación añaden el detalle por campo:

```json
{ "status": 400, "error": "VALIDATION_ERROR", "message": "invalid request body",
  "fields": { "password": "size must be between 8 and 100" } }
```

| Código | HTTP | Cuándo |
|---|---|---|
| `VALIDATION_ERROR` | 400 | el cuerpo no pasa Bean Validation |
| `INVALID_TOKEN` / `EXPIRED_TOKEN` | 400 | enlace de recuperación inválido, gastado o caducado |
| `BAD_CREDENTIALS` | 401 | usuario o contraseña incorrectos |
| `UNAUTHENTICATED` | 401 | falta el token, o no vale |
| `FORBIDDEN` | 403 | autenticado, pero sin el rol necesario |
| `USERNAME_TAKEN` / `EMAIL_TAKEN` | 409 | el usuario o el correo ya existen |

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
| `APP_JWT_SECRET` | clave de desarrollo | Secreto HS256, **mínimo 32 bytes**; con menos la app no arranca |
| `APP_JWT_EXPIRATION_MS` | `3600000` | Vida del token (1 h) |
| `APP_PASSWORD_RESET_EXPIRATION_MS` | `1800000` | Vida del enlace de recuperación (30 min) |
| `APP_FRONTEND_BASE_URL` | `http://localhost:3000` | Base del enlace que se envía por correo |
| `APP_MAIL_FROM` | `bitacora@localhost` | Remitente de los correos; con Gmail, igual a `MAIL_USERNAME` |
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` | Servidor SMTP (en Compose, `mailpit:1025`) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | vacíos | Credenciales SMTP; Mailpit no las pide |
| `MAIL_SMTP_AUTH` | `false` | `true` con un servidor real |
| `MAIL_SMTP_STARTTLS` | `false` | `true` con un servidor real (Gmail: puerto 587) |
| `MAILPIT_UI_PORT` | `8025` | Puerto del host para la bandeja web (solo Compose) |

Dentro de Compose el host de la BD es `postgres` y el del correo `mailpit` (nombres de servicio), no `localhost`; el `docker-compose.yml` ya inyecta esas variables.

Para no repetirlas en cada comando, se copia `docker/.env.example` a `docker/.env` y se rellena: Compose lee ese archivo solo. Está **gitignorado** porque ahí viven las credenciales; la plantilla sí se versiona. Sin `docker/.env` el proyecto funciona igual, con los valores por defecto.

> El secreto JWT por defecto está en el repositorio: sirve para arrancar en local y nada más. En cualquier despliegue real va en `APP_JWT_SECRET`, fuera del código.

Ejemplo con credenciales propias:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/midb \
SPRING_DATASOURCE_USERNAME=miuser \
SPRING_DATASOURCE_PASSWORD=mipass \
./mvnw spring-boot:run
```

## Frontend

Vive en el repositorio hermano **`front-react-project/`** (Next.js 16 + Tailwind). Se levanta aparte con `npm run dev` (http://localhost:3000) y ya consume esta API: entrar, registrarse, recuperar contraseña y una zona de cuenta. Guarda el token en una cookie `httpOnly` y llama a la API desde su propio servidor, así que el navegador nunca ve el JWT.

## Estructura y hoja de ruta

- Código y arquitectura: ver [`CLAUDE.md`](./CLAUDE.md).
- **Estado actual:** API stateless con JWT, alta de usuarios y recuperación de contraseña por correo, sobre Postgres, todo dockerizado.
- **Siguientes pasos:**
  1. `GET /api/posts` para que la bitácora del front deje de ser estática.
  2. Endpoints bajo `/api/admin/**` para la zona de administración.
  3. Perfil de test (H2 o Testcontainers) para que `./mvnw test` no dependa de un Postgres externo.
  4. Revocación de tokens (lista negra o refresh token corto) si hace falta cerrar sesión de verdad.
