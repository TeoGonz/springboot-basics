# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./mvnw spring-boot:run          # run app at http://localhost:8080
./mvnw test                     # run all tests
./mvnw test -Dtest=DemoApplicationTests#methodName   # run single test
./mvnw package                  # build jar to target/
./mvnw clean                    # wipe target/
```

No linter configured. Java 17, Spring Boot 4.1.0. Only test is `DemoApplicationTests.contextLoads` (smoke test, no assertions).

## Architecture

Spring MVC + Thymeleaf app demonstrating role-based access control. Three moving parts:

- `config/SecurityConfig.java` — the core. Defines the `SecurityFilterChain` (URL → role rules), form login, logout, and 403 handling. Also holds an **in-memory** user store (`InMemoryUserDetailsManager`) with hardcoded users: `admin`/`admin123` (ROLE_ADMIN), `user`/`user123` (ROLE_USER). Passwords BCrypt-encoded. No database.
- `controller/PageController.java` — thin `@Controller`, maps each GET route to a Thymeleaf template name. No business logic.
- `resources/templates/*.html` — Thymeleaf views: `public`, `user`, `admin`, `login`, `403`.

Access rules (SecurityConfig): `/public` open, `/user` needs USER or ADMIN, `/admin` needs ADMIN, everything else authenticated. Login success redirects to `/user`; denied access hits `/403`.

To add a page: add route in `PageController`, template in `templates/`, and an authorization rule in `SecurityConfig` if it needs protection.

## Templates conventions

- UI text in **Spanish**, `lang="es"`.
- Styling via **Bootstrap 5.3.3 CDN** link in each `<head>` (no local static assets, no build step for CSS).
- `thymeleaf-extras-springsecurity6`: `sec:authentication="name"` shows current user, `sec:authentication="principal.authorities"` shows roles.
- **Logout is a POST form** to `@{/logout}` (CSRF-protected by default) — not a plain link.
- `login.html` reads `${param.error}` / `${param.logout}` query flags to show alerts (wired by SecurityConfig's `defaultSuccessUrl` / `logoutSuccessUrl`).
