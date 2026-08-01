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
    │   │   ├── config/SecurityConfig.java    # filter chain (users now from Postgres via JpaUserDetailsService)
    │   │   ├── config/WebConfig.java         # i18n: LocaleResolver + lang interceptor
    │   │   ├── config/DataSeeder.java        # seeds admin/user into Postgres if empty (idempotent)
    │   │   ├── model/Post.java               # bitácora entry (static now, API later)
    │   │   ├── entity/User.java, entity/Role.java  # JPA user + role enum (users table + user_roles)
    │   │   ├── repository/UserRepository.java      # Spring Data JPA repo (findByUsername, existsBy…)
    │   │   ├── service/JpaUserDetailsService.java   # UserDetailsService backed by Postgres
    │   │   └── controller/PageController.java# GET routes -> template names; feeds POSTS to /public
    │   └── resources/
    │       ├── application.properties        # app config + messages basename/encoding
    │       ├── messages.properties           # i18n default (es)
    │       ├── messages_en.properties        # i18n English
    │       ├── messages_pt.properties        # i18n Português
    │       └── templates/                     # Thymeleaf views
    │           ├── fragments/nav.html         # two navbars: publicNav (bitácora) + appNav (login + private)
    │           ├── public.html               # bitácora landing (hero + entries + about)
    │           ├── login.html
    │           ├── user.html
    │           ├── admin.html
    │           └── 403.html
    └── test/java/com/example/demo/
        └── DemoApplicationTests.java         # contextLoads smoke test
```

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
```

No linter configured. Java 17, Spring Boot 4.1.0. Only test is `DemoApplicationTests.contextLoads` (smoke test, no assertions). It boots the full context, so **`./mvnw test` needs a live Postgres**; builds without a DB use `./mvnw package -DskipTests`.

## Architecture

Spring MVC + Thymeleaf app demonstrating role-based access control. Three moving parts:

- `config/SecurityConfig.java` — the core. Defines the `SecurityFilterChain` (URL → role rules), form login, logout, and 403 handling. Users now come from **Postgres via JPA**: `service/JpaUserDetailsService` (Spring auto-detects it as the `UserDetailsService`) loads `entity/User` rows; `config/DataSeeder` seeds `admin`/`admin123` (ROLE_ADMIN+USER) and `user`/`user123` (ROLE_USER) on first boot if the table is empty. Passwords BCrypt-encoded. Datasource config in `application.properties` (env-overridable, so the same build works locally and inside Docker where the DB host is the compose service name). Postgres itself is not started by the app — see `README.md`.
- `controller/PageController.java` — thin `@Controller`, maps each GET route to a Thymeleaf template name. No business logic.
- `resources/templates/*.html` — Thymeleaf views: `public`, `user`, `admin`, `login`, `403`.
- **Public page (`public.html`) is the *bitácora* (learning log)** — a blog landing: hero + grid of weekly entries + "Acerca de" + footer. Entries come from `POSTS` (a static `List<Post>`) in `PageController`, rendered with `th:each` and message keys; **next iteration these move to an API** — keep the render data-driven. `Post` fields carry i18n *keys* (`tituloKey`/`resumenKey`), resolved in the view with `#{__${p.tituloKey}__}`. Localized dates via `#temporals.format(..., #locale)`.
- **Two navbars** in `fragments/nav.html`: `publicNav` (bitácora: brand + Entradas/Acerca + login + language **dropdown** at the end; needs Bootstrap JS) and `appNav` (login + private/authenticated: brand + language button group). Public page uses `publicNav`; `login`/`user`/`admin`/`403` use `appNav`.
- `config/WebConfig.java` — i18n. `SessionLocaleResolver` (default `es`) + `LocaleChangeInterceptor` on param `lang`. UI strings live in `messages[_en|_pt].properties`; templates read them with `#{key}`.
- `docker/` — containerization. `Dockerfile` is multi-stage (Maven build → `eclipse-temurin:17-jre` runtime, non-root `spring` user); it **must** build with `-DskipTests` since `contextLoads` needs a live DB. `docker-compose.yml` runs `postgres:16` (named volume `pgdata`, `pg_isready` healthcheck) plus `backend`, whose build `context` is the **repo root** (`..`) — that's why `.dockerignore` sits at the root. `depends_on: service_healthy` keeps the app from starting before the DB accepts connections; datasource env vars point at host `postgres` (the service name).

Access rules (SecurityConfig): `/public` open, `/user` needs USER or ADMIN, `/admin` needs ADMIN, everything else authenticated. Login success redirects to `/user`; denied access hits `/403`.

To add a page: add route in `PageController`, template in `templates/`, and an authorization rule in `SecurityConfig` if it needs protection.

## Templates conventions

- UI text is **i18n**: no hardcoded strings — use `#{key}` and add the key to all three `messages*.properties`. Default locale `es`; switch with `?lang=es|en|pt`.
- Styling via **Bootstrap 5.3.3 CDN** link in each `<head>` (no local static assets, no build step for CSS). Every page loads `bootstrap.bundle.min.js` + `bootstrap-icons` CDN because both navbars (`publicNav` collapse+dropdown, `appNav` language dropdown) use JS components and the `bi-translate` icon. `public.html` also keeps its custom look in an inline `<style>` block. Any page using a JS component (dropdown/collapse/modal) must load the Bootstrap JS bundle.
- Shared navbar in `fragments/nav.html`; pull into a page with `<nav th:replace="~{fragments/nav :: appNav}"></nav>` (or `:: publicNav`). Language switcher links `?lang=xx` (handled by `LocaleChangeInterceptor`).
- `thymeleaf-extras-springsecurity6`: `sec:authentication="name"` shows current user, `sec:authentication="principal.authorities"` shows roles.
- **Logout is a POST form** to `@{/logout}` (CSRF-protected by default) — not a plain link.
- `login.html` reads `${param.error}` / `${param.logout}` query flags to show alerts (wired by SecurityConfig's `defaultSuccessUrl` / `logoutSuccessUrl`).
