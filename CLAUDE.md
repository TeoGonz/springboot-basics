# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project tree

> **IMPORTANT: keep this tree up to date.** Whenever you add, remove, move, or rename a file or directory, update this section in the same change so it always mirrors the real project. Excludes build/IDE artifacts (`target/`, `.idea/`, `.DS_Store`).

```
springboot_java_project/
├── pom.xml                     # Maven config, deps, Java 17 / Spring Boot 4.1.0
├── mvnw, mvnw.cmd              # Maven wrapper
├── .mvn/wrapper/               # wrapper properties
├── HELP.md                     # Spring Initializr help (gitignored)
├── CLAUDE.md                   # this file
└── src/
    ├── main/
    │   ├── java/com/example/demo/
    │   │   ├── DemoApplication.java          # @SpringBootApplication entry point
    │   │   ├── config/SecurityConfig.java    # filter chain + in-memory users
    │   │   ├── config/WebConfig.java         # i18n: LocaleResolver + lang interceptor
    │   │   └── controller/PageController.java# GET routes -> template names
    │   └── resources/
    │       ├── application.properties        # app config + messages basename/encoding
    │       ├── messages.properties           # i18n default (es)
    │       ├── messages_en.properties        # i18n English
    │       ├── messages_pt.properties        # i18n Português
    │       └── templates/                     # Thymeleaf views
    │           ├── fragments/nav.html         # navbar + language switcher (reusable)
    │           ├── public.html
    │           ├── login.html
    │           ├── user.html
    │           ├── admin.html
    │           └── 403.html
    └── test/java/com/example/demo/
        └── DemoApplicationTests.java         # contextLoads smoke test
```

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
- `resources/templates/*.html` — Thymeleaf views: `public`, `user`, `admin`, `login`, `403`. All include the shared navbar via `th:replace="~{fragments/nav :: nav}"`.
- `config/WebConfig.java` — i18n. `SessionLocaleResolver` (default `es`) + `LocaleChangeInterceptor` on param `lang`. UI strings live in `messages[_en|_pt].properties`; templates read them with `#{key}`.

Access rules (SecurityConfig): `/public` open, `/user` needs USER or ADMIN, `/admin` needs ADMIN, everything else authenticated. Login success redirects to `/user`; denied access hits `/403`.

To add a page: add route in `PageController`, template in `templates/`, and an authorization rule in `SecurityConfig` if it needs protection.

## Templates conventions

- UI text is **i18n**: no hardcoded strings — use `#{key}` and add the key to all three `messages*.properties`. Default locale `es`; switch with `?lang=es|en|pt`.
- Styling via **Bootstrap 5.3.3 CDN** link in each `<head>` (no local static assets, no build step for CSS). CSS only — no Bootstrap JS bundle, so avoid JS-dependent components (dropdowns/modals).
- Shared navbar in `fragments/nav.html`; pull into a page with `<nav th:replace="~{fragments/nav :: nav}"></nav>`. Language switcher links `?lang=xx` (handled by `LocaleChangeInterceptor`).
- `thymeleaf-extras-springsecurity6`: `sec:authentication="name"` shows current user, `sec:authentication="principal.authorities"` shows roles.
- **Logout is a POST form** to `@{/logout}` (CSRF-protected by default) — not a plain link.
- `login.html` reads `${param.error}` / `${param.logout}` query flags to show alerts (wired by SecurityConfig's `defaultSuccessUrl` / `logoutSuccessUrl`).
