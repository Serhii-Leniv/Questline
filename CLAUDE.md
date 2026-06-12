# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Questline — conventions for Claude Code

Questline is an AI-powered goal decomposition + streak tracker. Full spec: [SPEC.md](SPEC.md).
Build **phase by phase** per SPEC §13 — never "do it all at once".

## Current state
Phase 1 + Phase 2 are complete, and much of Phase 3. Built and on `main` (CI green): goal/task CRUD,
the AI engine (generate / refine / parse / decompose / replan as JobRunr jobs with validate+repair),
gamification (streaks + freezes, XP/levels, achievements, heatmap, topics), planning (plan-my-day +
week view), per-user **BYOK** AI provider (encrypted key, direct OpenAI-compatible `RestClient` path
in `ai/OpenAiCompatibleClient`; server default still via Spring AI), roadmap sharing (templates),
account export/deletion, Postgres RLS (per-tx `app.user_id` GUC via `RlsTransactionManager` +
pass-through policies in `V10`), and the React SPA (Today/Week/Goals/Stats/Settings). Migrations
run to `V10`. **Not done:** open registration + billing, AI token accounting. Check what exists
before assuming — most of SPEC §7–13 is now real.

## Stack
Java 21, Spring Boot 4.0, Spring Web (REST), Spring Data JPA + Flyway, PostgreSQL,
Spring Security (Google OAuth2 + JWT), Spring AI 2.0 (Google Gemini free tier provider),
JobRunr (async jobs), Bean Validation, JUnit 5 + Mockito + Testcontainers, Gradle (Kotlin DSL).
Frontend: React + TS (Vite) talking to REST.

> **Versions are pinned and were verified against Maven Central / official docs.** Spring AI 2.0
> is still pre-GA: pinned to `2.0.0-RC2` in [backend/build.gradle.kts](backend/build.gradle.kts).
> Moving to GA is a one-line BOM bump. Spring Boot 4 / Spring AI 2.0 are new — follow current
> official docs, not 3.x examples (Jakarta EE 11, Jackson 3, JUnit 5 only — no JUnit 4).

## Commands
All Gradle commands run from `backend/` via the wrapper (`./gradlew` / `.\gradlew.bat`).
- **Build + test:** `./gradlew build test` — **needs Docker running** (Testcontainers starts a real
  Postgres 16). This is the DoD gate before declaring work done.
- **Single test class:** `./gradlew test --tests com.questline.PingIntegrationTest`
- **Single test method:** `./gradlew test --tests "com.questline.PingIntegrationTest.ping_isPublic_andReturnsOk"`
- **Run the app:** load `.env` into the environment first, then `./gradlew bootRun` (the README has
  the PowerShell `.env`-loading snippet). App on `:8080`; verify with `/api/ping` and
  `/actuator/health`.
- **Local Postgres only:** `docker compose up -d db`. **Whole stack:** `docker compose up --build`.
- **Frontend:** from `frontend/` — `npm install`, `npm run dev` (`:5173`, proxies `/api` to
  `:8080`), `npm run build` (`tsc && vite build`).
- Boot jar is named `questline.jar`; entry point is `com.questline.QuestlineApplication`.

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
