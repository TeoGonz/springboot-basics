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
├── docker/                     # containerization (backend image + Postgres)
│   ├── Dockerfile                  # multi-stage: maven build -> temurin JRE runtime, non-root
│   └── docker-compose.yml          # services: postgres (healthcheck+volume) + backend (built from ..)
└── src/
    ├── main/
    │   ├── java/com/example/demo/
    │   │   ├── DemoApplication.java          # @SpringBootApplication entry point
    │   │   ├── config/SecurityConfig.java    # stateless filter chain + HTTP Basic (temporary, JWT next)
    │   │   ├── config/DataSeeder.java        # seeds admin/user into Postgres if empty (idempotent)
    │   │   ├── entity/User.java, entity/Role.java  # JPA user + role enum (users table + user_roles)
    │   │   ├── repository/UserRepository.java      # Spring Data JPA repo (findByUsername, existsBy…)
    │   │   ├── service/JpaUserDetailsService.java   # UserDetailsService backed by Postgres
    │   │   └── controller/MeController.java  # GET /api/me -> username, enabled, roles
    │   └── resources/
    │       └── application.properties        # datasource + JPA config (env-overridable)
    └── test/java/com/example/demo/
        └── DemoApplicationTests.java         # contextLoads smoke test
```

The frontend is **not** in this repository: it lives in the sibling Next.js project `front-react-project/`, with its own git. This app serves JSON only — no templates, no static assets, no i18n.

## Commands

```bash
# Docker — whole stack (backend compiled inside the image + Postgres)
docker compose -f docker/docker-compose.yml up --build     # build + run everything
docker compose -f docker/docker-compose.yml up -d postgres # DB only (dev: app on host)
docker compose -f docker/docker-compose.yml logs -f backend
docker compose -f docker/docker-compose.yml down           # stop, keep pgdata volume
docker compose -f docker/docker-compose.yml down -v        # stop and DROP the DB
docker exec -it bitacora-postgres psql -U bitacora -d bitacora

# Maven — app on the host (needs a reachable Postgres, e.g. `up -d postgres`)
./mvnw spring-boot:run          # run app at http://localhost:8080
./mvnw test                     # run all tests
./mvnw test -Dtest=DemoApplicationTests#methodName   # run single test
./mvnw package                  # build jar to target/
./mvnw clean                    # wipe target/

# API smoke check (HTTP Basic, seeded users)
curl -i localhost:8080/api/me                     # 401
curl -u user:user123 localhost:8080/api/me        # 200, ROLE_USER
curl -u admin:admin123 localhost:8080/api/me      # 200, ROLE_ADMIN + ROLE_USER
```

No linter configured. Java 17, Spring Boot 4.1.0. Only test is `DemoApplicationTests.contextLoads` (smoke test, no assertions). It boots the full context, so **`./mvnw test` needs a live Postgres**; builds without a DB use `./mvnw package -DskipTests`.

## Architecture

Stateless Spring REST API demonstrating role-based access control. Moving parts:

- `config/SecurityConfig.java` — the core. Defines the `SecurityFilterChain`: CSRF disabled, `SessionCreationPolicy.STATELESS`, URL → role rules, and HTTP Basic. Users come from **Postgres via JPA**: `service/JpaUserDetailsService` (Spring auto-detects it as the `UserDetailsService`) loads `entity/User` rows; `config/DataSeeder` seeds `admin`/`admin123` (ROLE_ADMIN+USER) and `user`/`user123` (ROLE_USER) on first boot if the table is empty. Passwords BCrypt-encoded. Datasource config in `application.properties` (env-overridable, so the same build works locally and inside Docker where the DB host is the compose service name). Postgres itself is not started by the app — see `README.md`.
- **HTTP Basic is temporary.** It exists so the API is callable from Postman/curl; the JWT filter (spec backend 02) replaces it. CSRF stays disabled only while the API is cookie-less — bringing back cookie auth means bringing back CSRF.
- `controller/MeController.java` — `GET /api/me`, the authenticated principal's `username`, `enabled` and `roles`. Never exposes the password hash.
- `docker/` — containerization. `Dockerfile` is multi-stage (Maven build → `eclipse-temurin:17-jre` runtime, non-root `spring` user); it **must** build with `-DskipTests` since `contextLoads` needs a live DB. `docker-compose.yml` runs `postgres:16` (named volume `pgdata`, `pg_isready` healthcheck) plus `backend`, whose build `context` is the **repo root** (`..`) — that's why `.dockerignore` sits at the root. `depends_on: service_healthy` keeps the app from starting before the DB accepts connections; datasource env vars point at host `postgres` (the service name).

Access rules (SecurityConfig): `/api/auth/**` open (reserved for the JWT endpoints), `/api/admin/**` needs ADMIN, everything else authenticated. There are no view routes: `/public`, `/login`, `/user`, `/admin` and `/403` are gone — an authenticated request to any of them returns 404, an anonymous one 401.

To add an endpoint: `@RestController` under `controller/`, path prefixed `/api/`, plus an authorization rule in `SecurityConfig` if the default (authenticated) is not what you want.

## API conventions

- JSON in, JSON out. No HTML, no redirects, no cookies — every response must make sense to a Postman/curl caller.
- Paths live under `/api/`. Role-restricted areas get their own prefix (`/api/admin/**`) so the rule stays in the filter chain, not scattered in annotations.
- Never serialize an `entity/` object straight out: expose a `record` DTO (see `MeController.MeResponse`) so password hashes and internal fields cannot leak.
- The API has no i18n. User-facing text is the frontend's job; messages in responses are for developers.
- CORS is not configured yet: Next renders statically and makes no cross-origin call. It gets added (`http://localhost:3000`) when the frontend starts consuming the API.
