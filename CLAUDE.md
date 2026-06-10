# Questline — conventions for Claude Code

Questline is an AI-powered goal decomposition + streak tracker. Full spec: [SPEC.md](SPEC.md).
Build **phase by phase** per SPEC §13 — never "do it all at once".

## Stack
Java 21, Spring Boot 4.0, Spring Web (REST), Spring Data JPA + Flyway, PostgreSQL,
Spring Security (Google OAuth2 + JWT), Spring AI 2.0 (Google Gemini free tier provider),
JobRunr (async jobs), Bean Validation, JUnit 5 + Mockito + Testcontainers, Gradle (Kotlin DSL).
Frontend: React + TS (Vite) talking to REST.

> **Versions are pinned and were verified against Maven Central / official docs.** Spring AI 2.0
> is still pre-GA: pinned to `2.0.0-RC2` in [backend/build.gradle.kts](backend/build.gradle.kts).
> Moving to GA is a one-line BOM bump. Spring Boot 4 / Spring AI 2.0 are new — follow current
> official docs, not 3.x examples (Jakarta EE 11, Jackson 3, JUnit 5 only — no JUnit 4).

## Architecture rules
- Layers: `web/` (@RestController, thin: validation + call service) → `service/` (business logic,
  nothing about HTTP/JPA leaks out) → `repository/` (Spring Data JPA only). AI in `ai/`, jobs in
  `jobs/`, entities + enums in `domain/`, security in `security/`, cross-cutting in `common/`.
- **No business logic in controllers or repositories.**
- Every domain query MUST be scoped by the authenticated `userId`. **Never trust `userId` from
  the request body** — read it from the JWT (`Authentication`).
- DTOs are Java records with Bean Validation. Validate every input AND every AI output.
- **AI isolation:** the rest of the app depends only on the `ai/` package's own interface
  (`LlmClient` + our own records), never on Spring AI's `ChatClient` directly. The Spring-AI-backed
  impl is the only place that imports Spring AI. This keeps RC→GA churn (or a provider swap to
  LangChain4j / a custom Gemini RestClient) contained to one module.
- All LLM calls run inside JobRunr jobs, never inline in a request thread.
- AI provider is abstracted via Spring AI `ChatClient`; default = Google Gemini free tier
  (`gemini-2.5-flash`). No hardcoded model or keys — everything from env.
- "Today" and streaks are computed in the user's **IANA timezone** (local midnight boundary),
  never from UTC dates.

## Conventions
- Schema is owned by Flyway migrations (`db/migration/V*.sql`); Hibernate runs with
  `ddl-auto: validate`. Each new table/column ships as a new migration — never edit a released one.
- UUID primary keys, generated app-side. `tasks.resources` is a JSON (`jsonb`) column.
- Consistent error contract: a single `@RestControllerAdvice` → `{ code, message }`.

## Workflow
- Small, reviewable commits. Every feature: code + tests for core logic (streak/XP/AI-output
  validation) + error states.
- Run `./gradlew build test` (with Testcontainers) before declaring done. DoD from SPEC §4.
- Secrets live only in env / `.env` (git-ignored). Never commit `.env`; never put provider keys
  in the client/frontend.

## Don'ts
- No business logic in controllers or repositories. No unscoped DB queries.
- No provider secrets in client/frontend. No hardcoded model or keys.
- No skipping AI-output validation / repair-loop. No blocking HTTP threads on LLM calls.

## Boot 4 / toolchain gotchas (learned the hard way)
- **Spring Boot 4 modularized its autoconfigurations.** Plain `flyway-core` no longer triggers
  migrations — use `spring-boot-starter-flyway` (+ `flyway-database-postgresql`). If a "just add
  the library" integration silently does nothing, check whether its autoconfig now lives in a
  dedicated starter/module.
- **Testcontainers 2.0** (what Boot 4 aligns to) renamed its modules to the `testcontainers-*`
  prefix: `testcontainers-junit-jupiter`, `testcontainers-postgresql`. Boot's BOM does not manage
  Testcontainers or rest-assured versions — pin them explicitly.
- **Spring AI 2.0 is pre-GA**; pinned to `2.0.0-RC2` (the only Spring AI line compatible with
  Boot 4). Follow current 2.0 docs, not 3.x-era examples.
