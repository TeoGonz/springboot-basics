# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project tree

> **IMPORTANT: keep this tree up to date.** Whenever you add, remove, move, or rename a file or directory, update this section in the same change so it always mirrors the real project. Excludes build/IDE artifacts (`target/`, `.idea/`, `.DS_Store`) and everything under `.claude/` (gitignored local working notes — never referenced from tracked files).

```
springboot_java_project/
├── pom.xml                     # Maven config, deps, Java 17 / Spring Boot 4.1.0
├── mvnw, mvnw.cmd              # Maven wrapper
├── .mvn/wrapper/               # wrapper properties
├── .gitattributes, .gitignore  # git config
├── .dockerignore               # build context excludes (must live at repo root)
├── HELP.md                     # Spring Initializr help (gitignored)
├── README.md                   # project overview + setup/startup steps (Docker, run, users, endpoints)
├── CLAUDE.md                   # this file
├── docker/                     # containerization (backend image + Postgres + Mailpit)
│   ├── Dockerfile                  # multi-stage: maven build -> temurin JRE runtime, non-root
│   └── docker-compose.yml          # services: postgres (healthcheck+volume), mailpit, backend (built from ..)
└── src/
    ├── main/
    │   ├── java/com/example/demo/
    │   │   ├── DemoApplication.java          # @SpringBootApplication entry point
    │   │   ├── config/SecurityConfig.java    # stateless chain, JWT filter, AuthenticationManager
    │   │   ├── config/DataSeeder.java        # seeds admin/user into Postgres if empty (idempotent)
    │   │   ├── config/AsyncConfig.java       # @EnableAsync (used by the mail sender)
    │   │   ├── entity/User.java, entity/Role.java       # JPA user + role enum (users + user_roles)
    │   │   ├── entity/PasswordResetToken.java           # single-use reset link (SHA-256 hash stored)
    │   │   ├── repository/UserRepository.java           # findByUsername/findByEmail/existsBy…
    │   │   ├── repository/PasswordResetTokenRepository.java  # findByTokenHash, invalidateAllForUser
    │   │   ├── security/JwtService.java      # HS256 issue/parse; fails fast on a short secret
    │   │   ├── security/JwtAuthFilter.java   # Bearer header -> SecurityContext (runs on error dispatch)
    │   │   ├── security/RestAuthEntryPoint.java, RestAccessDeniedHandler.java  # 401/403 as JSON
    │   │   ├── service/JpaUserDetailsService.java   # UserDetailsService backed by Postgres
    │   │   ├── service/AuthService.java             # register + login (token issuing)
    │   │   ├── service/PasswordResetService.java    # reset token: create, hash, validate, consume
    │   │   ├── service/MailService.java             # @Async plain-text mail via JavaMailSender
    │   │   ├── dto/                          # records: Register/Login/Forgot/Reset requests, Auth/User/ApiError
    │   │   ├── web/ApiException.java, web/ApiExceptionHandler.java  # single error shape + codes
    │   │   ├── controller/AuthController.java  # /api/auth: register, login, forgot/reset password
    │   │   └── controller/MeController.java    # GET /api/me -> username, enabled, roles
    │   └── resources/
    │       ├── application.properties        # datasource, JPA, JWT, mail, reset (env-overridable)
    │       └── messages/email*.properties    # es/en/pt text of the reset email (only i18n here)
    └── test/java/com/example/demo/
        └── DemoApplicationTests.java         # contextLoads smoke test
```

The frontend is **not** in this repository: it lives in the sibling Next.js project `front-react-project/`, with its own git. This app serves JSON only — no templates, no static assets. The one exception to "no i18n" is the body of transactional emails: this app writes and sends them, so it owns their wording.

## Commands

```bash
# Docker — whole stack (backend compiled inside the image + Postgres + Mailpit)
docker compose -f docker/docker-compose.yml up --build     # build + run everything
docker compose -f docker/docker-compose.yml up -d postgres mailpit  # deps only (dev: app on host)
docker compose -f docker/docker-compose.yml logs -f backend
docker compose -f docker/docker-compose.yml down           # stop, keep pgdata volume
docker compose -f docker/docker-compose.yml down -v        # stop and DROP the DB
docker exec -it bitacora-postgres psql -U bitacora -d bitacora
# Mailpit inbox: http://localhost:8025 — REST too:
curl -s 'localhost:8025/api/v1/messages?limit=1'           # list; /api/v1/message/<ID> for the body

# Maven — app on the host (needs a reachable Postgres, e.g. `up -d postgres`)
./mvnw spring-boot:run          # run app at http://localhost:8080
./mvnw test                     # run all tests
./mvnw test -Dtest=DemoApplicationTests#methodName   # run single test
./mvnw package                  # build jar to target/
./mvnw clean                    # wipe target/

# API smoke check (JWT, seeded users)
curl -i localhost:8080/api/me                     # 401 UNAUTHENTICATED
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)
curl -s localhost:8080/api/me -H "Authorization: Bearer $TOKEN"   # 200, ROLE_ADMIN + ROLE_USER
curl -s -X POST localhost:8080/api/auth/forgot-password -H 'Content-Type: application/json' \
  -d '{"email":"user@bitacora.local","locale":"es"}'              # 202, mail lands in Mailpit
```

No linter configured. Java 17, Spring Boot 4.1.0. Only test is `DemoApplicationTests.contextLoads` (smoke test, no assertions). It boots the full context, so **`./mvnw test` needs a live Postgres**; builds without a DB use `./mvnw package -DskipTests`.

## Architecture

Stateless Spring REST API demonstrating role-based access control. Moving parts:

- `config/SecurityConfig.java` — the core. One `SecurityFilterChain`: CSRF disabled, `SessionCreationPolicy.STATELESS`, URL → role rules, `JwtAuthFilter` before `UsernamePasswordAuthenticationFilter`, and the two JSON error handlers. Also exposes the `AuthenticationManager` (a `ProviderManager` over `DaoAuthenticationProvider`, which in Spring Security 7 takes the `UserDetailsService` in its constructor) that `/api/auth/login` uses. Users come from **Postgres via JPA**: `service/JpaUserDetailsService` (Spring auto-detects it as the `UserDetailsService`) loads `entity/User` rows; `config/DataSeeder` seeds `admin`/`admin123` (ROLE_ADMIN+USER) and `user`/`user123` (ROLE_USER) on first boot if the table is empty. Passwords BCrypt-encoded. Datasource config in `application.properties` (env-overridable, so the same build works locally and inside Docker where the DB host is the compose service name). Postgres itself is not started by the app — see `README.md`.
- **Authentication is JWT only.** `security/JwtService` signs HS256 tokens (`sub`, `roles`, `iat`, `exp`) with `app.jwt.secret` and refuses to start if that secret is under 32 bytes. `security/JwtAuthFilter` turns a `Bearer` header into an `Authentication`; a missing or broken token is *not* an error, it just leaves the request anonymous and lets the rules decide. It overrides `shouldNotFilterErrorDispatch()` — without that, the internal forward to `/error` arrives unauthenticated and a 404 comes back as 401. HTTP Basic is gone. CSRF stays disabled only while the API is cookie-less; bringing back cookie auth means bringing back CSRF.
- `controller/AuthController.java` — `/api/auth`: `register` (409 on duplicate username/email), `login` (JWT), `forgot-password` (always 202 — answering differently would leak which emails exist) and `reset-password` (single-use token). Logic lives in `service/AuthService` and `service/PasswordResetService`; the controller only maps HTTP.
- `service/PasswordResetService` — generates 32 random bytes, mails the base64url value and stores **only its SHA-256 hash**. Plain SHA-256 is enough here: unlike a password, the token is already 256 bits of entropy, so there is nothing to brute-force and BCrypt's deliberate cost buys nothing. Requesting a new link marks previous ones used. `service/MailService` sends plain text `@Async` (so response time does not reveal whether the address exists) and is the only place with translated strings.
- `web/ApiExceptionHandler` — every error leaves as `{status, error, message[, fields]}`. `error` is a stable code (`BAD_CREDENTIALS`, `EMAIL_TAKEN`, `EXPIRED_TOKEN`…) that the front maps to its own translated text. Controllers throw `ApiException` and never shape responses.
- `controller/MeController.java` — `GET /api/me`, the authenticated principal's `username`, `enabled` and `roles`. Never exposes the password hash.
- `docker/` — containerization. `Dockerfile` is multi-stage (Maven build → `eclipse-temurin:17-jre` runtime, non-root `spring` user); it **must** build with `-DskipTests` since `contextLoads` needs a live DB. `docker-compose.yml` runs `postgres:16` (named volume `pgdata`, `pg_isready` healthcheck), `mailpit` (SMTP 1025, inbox 8025) and `backend`, whose build `context` is the **repo root** (`..`) — that's why `.dockerignore` sits at the root. `depends_on: service_healthy` keeps the app from starting before the DB accepts connections; env vars point the datasource at host `postgres` and the mailer at `mailpit`.

Access rules (SecurityConfig): `/api/auth/**` open (register, login and password recovery must work without a token), `/api/admin/**` needs ADMIN, everything else authenticated. There are no view routes: `/public`, `/login`, `/user`, `/admin` and `/403` are gone — an authenticated request to any of them returns 404, an anonymous one 401.

To add an endpoint: `@RestController` under `controller/`, path prefixed `/api/`, plus an authorization rule in `SecurityConfig` if the default (authenticated) is not what you want.

## API conventions

- JSON in, JSON out. No HTML, no redirects, no cookies — every response must make sense to a Postman/curl caller.
- Paths live under `/api/`. Role-restricted areas get their own prefix (`/api/admin/**`) so the rule stays in the filter chain, not scattered in annotations.
- Never serialize an `entity/` object straight out: expose a `record` DTO under `dto/` (or nested, see `MeController.MeResponse`) so password hashes and internal fields cannot leak. Input DTOs carry Bean Validation and are read with `@Valid`.
- Errors are codes, not sentences: throw `ApiException(status, CODE, developerMessage)` and let `ApiExceptionHandler` shape it. Adding a case means adding the code to the table in `README.md` and a message key in the front.
- The API has no i18n for responses. The single exception is the body of transactional emails (`messages/email_*.properties`), because this app is the one writing and sending them; the requester picks the language with the `locale` field.
- **CORS is not configured, and that is the design.** Next calls the API from its own Node process (Server Actions), so the browser never issues a cross-origin request. Add the bean only if a browser ever needs to call the API directly.
- Authentication endpoints must stay callable without a token, hence the `/api/auth/**` prefix; anything that needs the caller's identity reads it from the `Authentication`, never from a request field.
