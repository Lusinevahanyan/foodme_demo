# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

FoodMe is a food-ordering demo used for a QA/DevOps course. It is a monorepo of three apps plus deploy/monitoring config:

- `apps/backend` — Spring Boot 3.3 (Java 17, Gradle), PostgreSQL + Flyway, JWT auth.
- `apps/web` — customer storefront: React 19 + TypeScript + Vite, Tailwind v4, shadcn-style UI (`src/components/ui`), TanStack Query, i18next, Dexie (IndexedDB) cart.
- `apps/admin` — back office: react-admin 5 + MUI, plain JS/JSX, axios.
- `render.yaml` / `render-monitoring.yaml` — Render blueprints; `infra/monitoring/stack` — single-container Prometheus + Loki + Grafana + Grafana MCP behind nginx (see `infra/monitoring/README.md`).

Some behavior is intentionally "demo-only" for QA exercises. Don't "fix" it unless asked:
- `SimulatedLatencyConfig` adds a random 200–1500 ms delay to `/api/**` and `/admin/**` (not `/api/images/**`).
- `FlakyHeartbeatJob` (backend, `@Profile("!test")`) and `lib/flakyHeartbeat.{ts,js}` (web/admin) deliberately report periodic errors to Sentry/GlitchTip.
- Playwright specs named `flake-*.spec.ts` exercise intentionally flaky scenarios.

## Commands

Backend (`apps/backend`):
```bash
./gradlew build                     # compile + tests (what CI runs)
./gradlew test --tests OrderControllerTest
./gradlew test --tests 'OrderControllerTest.someMethod'
./gradlew bootRun                   # needs Postgres on localhost:5432 (db/user/pass: foodme)
```
Tests use the `test` profile (`application-test.properties`): H2 in PostgreSQL mode, Flyway disabled, Hibernate `create-drop`, seed data from `src/test/resources/data.sql`, latency 0, HTTP logging off.

Web (`apps/web`):
```bash
npm run dev            # Vite; calls backend at http://localhost:8081 in dev
npm run lint           # oxlint
npm run build          # tsc -b && vite build (type errors fail the build)
npx playwright test                       # e2e; auto-starts dev server on :5180
npx playwright test e2e/cart-decrement.spec.ts
npx playwright test -g "test name"
npm run test:e2e:all   # web + admin e2e
```

Admin (`apps/admin`):
```bash
npm run dev     # local dev serves at /, production build at /backoffice/
npm run lint    # eslint
npm run build
npx playwright test   # dev server on :5174
```
E2E suites hit a real backend, so start one on :8081 first. Override targets with `PLAYWRIGHT_BASE_URL` / `ADMIN_BASE_URL`.

CI (`.github/workflows/ci.yml`) runs the backend build, web/admin lint + build, Docker image builds, and Playwright. The Playwright job references `infra/docker-compose.yml` (core profile) and `.env.example`. That compose file is **not currently in the repo**, so that job can't work as written.

## Architecture

**Single-origin deploy.** `apps/backend/Dockerfile` uses the **repo root** as build context. It builds both SPAs, copies `web/dist` into `static/` and `admin/dist` into `static/backoffice/`, and bundles them into the Spring Boot jar. `SpaWebConfig` serves the storefront at `/` and admin at `/backoffice`. Extensionless paths fall back to the matching `index.html`, except under `api/`, `admin/`, `actuator/`, `swagger-ui`, and `v3/`. Because of this, both frontends default to a relative API base in production and `http://localhost:8081` in dev. `VITE_API_BASE_URL` overrides both. Vite env vars (including `VITE_SENTRY_DSN`) are baked in at build time.

**Two API surfaces** (`controller/api` vs `controller/admin`):
- `/api/**` is the storefront API. It's public except `/api/customer/**` and `POST /api/order`, which require `ROLE_CUSTOMER`. `/api/auth/**` handles customer login/register.
- `/admin/**` is the admin REST API and needs an admin JWT. Exceptions: `/admin/auth/login`, and `GET /admin/dish/**` is public.
- Do not confuse `/admin` (API) with `/backoffice` (admin SPA).
- Auth is stateless JWT (`JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`). Web keeps the token via `lib/auth-storage.ts`; admin keeps it in `localStorage.token`, and a 401 redirects to login.

**Backend layering:** controller → service → Spring Data repository (plus a custom `DishSearchRepositoryImpl`), DTOs in `dto/`, errors via `exceptionHandler/GlobalExceptionHandler` (`BadRequestException`, `NotFoundException`). Schema lives in the `foodme` schema and is owned by Flyway (`db/migration/V*__*.sql`). `ddl-auto=validate`, so entity changes need a new migration.

**Images** are stored in Postgres (`foodme.image`) and served at `/api/images/**`. `ImageSeedRunner` loads `src/main/resources/img-seed/` on first start (`foodme.images.seed-enabled`). `ImageUrlResponseAdvice` rewrites image URLs in responses.

**DB config:** `DatabaseUrlEnvironmentPostProcessor` (registered in `META-INF/spring.factories`) turns a `postgresql://…` `DATABASE_URL` (Render/Neon) into JDBC properties. Otherwise the app uses `SPRING_DATASOURCE_URL` or the discrete `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` variables.

**Web cart** is client-only in IndexedDB (`lib/db.ts`, `hooks/useCart.ts`, keyed by `uid = chefId-dishId-sortedAdditionIds`). It holds dishes from only one chef at a time: adding from another chef returns `"mismatch"` unless `replaceOtherChef` is set. Each item has a `limitations.minQuantity` (from `minimumOrderCount`), and decrementing at the minimum removes the item.

**Admin data layer:** `providers/dataProvider.js` maps react-admin plural resources (`orders`, `chefs`, `dishes`) to singular backend paths (`/admin/order`, …). It converts `{ list, count }` responses to `{ data, total }` and turns pages from 1-based into 0-based. Sorting happens client-side on the current page.

**Observability:** Actuator exposes `health`, `info`, and `prometheus`. Logs are JSON via `logback-spring.xml`, and the Loki appender is enabled only when `LOKI_PUSH_URL` is set. `HttpLoggingFilter` logs `/api/**` and `/admin/**` requests and responses with secrets redacted. Sentry/GlitchTip is enabled via `SENTRY_DSN` on the backend and `VITE_SENTRY_DSN` on each frontend. Swagger UI is at `/swagger-ui.html`.

**Deployment target** is Render's free tier (512 MB RAM, 0.1 CPU). That's why Tomcat is capped at 32 threads, the Dockerfile sets JVM flags (C1-only, SerialGC), and Flyway uses connect retries. Keep those constraints in mind before adding heavy startup work.
