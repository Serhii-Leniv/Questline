# Questline

AI-powered goal decomposition + streak tracker. Formulate (or import) a big goal → an AI engine
decomposes it into a tree of daily tasks → close a few each day and keep your streak.

This repository is at **Phase 0 (skeleton)**. See [SPEC.md](SPEC.md) for the full spec and
[CLAUDE.md](CLAUDE.md) for conventions.

## Stack

- **Backend:** Java 21, Spring Boot 4.0, Spring Web, Spring Data JPA + Flyway, PostgreSQL,
  Spring Security (Google OAuth2 + JWT), Spring AI 2.0 → Google Gemini (free tier), JobRunr,
  Actuator. Gradle (Kotlin DSL).
- **Frontend:** Vite + React + TypeScript.
- **Tests:** JUnit 5 + Testcontainers (real Postgres) + RestAssured.

> **Spring AI 2.0 is pre-GA**; pinned to `2.0.0-RC2` (the only Spring AI line compatible with
> Spring Boot 4). Bumping to GA is a one-line change in `backend/build.gradle.kts`.

## Repository layout

```
questline/
├── backend/    Spring Boot app (Gradle). Packages: web, service, repository, ai, jobs,
│               domain, security, common.
├── frontend/   Vite + React + TS SPA.
├── docker-compose.yml   Postgres + backend.
├── .env.example         Copy to .env (git-ignored) and fill in.
├── SPEC.md / CLAUDE.md
```

## Prerequisites

- **JDK 21** (e.g. Temurin). The Gradle wrapper is included.
- **Node 20+** and npm.
- **Docker** (for Postgres locally and for Testcontainers in tests).

## 1. Configure environment

```bash
cp .env.example .env
```

Fill in `.env`:

- `GEMINI_API_KEY` — free key from [Google AI Studio](https://aistudio.google.com/apikey)
  (NOT Vertex AI). Model defaults to `gemini-2.5-flash`.
- `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` — Google Cloud Console → Credentials → OAuth client.
  Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`.
- `JWT_SECRET` — any random string ≥ 32 bytes.
- `POSTGRES_*` / `DB_*` — local database credentials.

> `.env` is git-ignored — never commit it. AI features are inactive in Phase 0, but the key is
> wired so the provider bean is configured.

## 2. Start Postgres

```bash
docker compose up -d db
```

(Or run the whole stack — backend included — with `docker compose up --build`.)

## 3. Run the backend

The backend reads `.env` values from your environment. Load them, then run the wrapper:

```bash
# bash / WSL — export the .env values into the shell:
set -a; source .env; set +a
cd backend
./gradlew bootRun
```

```powershell
# PowerShell — load .env into the process, then run:
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
  $name, $value = $_ -split '=', 2
  Set-Item "env:$($name.Trim())" $value.Trim('"').Trim()
}
cd backend
.\gradlew.bat bootRun
```

Verify:

```bash
curl http://localhost:8080/api/ping            # -> {"status":"ok"}
curl http://localhost:8080/actuator/health     # -> {"status":"UP"}
```

`GET /api/me` requires authentication (start login at
`http://localhost:8080/oauth2/authorization/google`).

## 4. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173> — the page calls `/api/ping` (proxied to the backend) and shows the
result.

## 5. Build & test

```bash
cd backend
./gradlew build test     # needs Docker running (Testcontainers spins up Postgres)
```

CI (GitHub Actions) runs `./gradlew build test` plus a frontend build on every push/PR.
