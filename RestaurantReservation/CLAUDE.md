# CLAUDE.md

Guidance for Claude Code when working in this repository. Two nested `CLAUDE.md`
files hold the per-tier detail — this file stays project-wide.

## Product

Internal SaaS for Georgian restaurants to manage reservations digitally
(replaces notebooks, WhatsApp, Excel). Subscription model, 50–400+ GEL/month.
Operational tool for staff, **not** a public booking marketplace.

## Repository layout

```
src/main/java/org/example/reservations/   Spring Boot backend (see nested CLAUDE.md)
src/main/resources/db/migration/          Flyway migrations (V{N}__description.sql)
src/test/java/...                         JUnit + Spring tests
frontend/                                 React 19 + Vite 8 SPA (see frontend/CLAUDE.md)
docs/PROJECT_STATE.md                     Current feature status and open gaps
.claude/rules/                            Path-scoped rules loaded per file
pom.xml, docker-compose.yml               Backend build + local Postgres
```

Backend serves under `/api/**` on port 8080; frontend dev server proxies `/api`
to it (see `frontend/vite.config.ts`).

## Commands

Backend (repo root):

```bash
mvn spring-boot:run                                # dev server on :8080
mvn clean install                                  # full build + tests
mvn test                                           # all tests
mvn test -Dtest=ReservationServiceTest             # single class
mvn test -Dtest=ReservationServiceTest#methodName  # single method
```

Frontend (`frontend/`, PowerShell — `npm`'s `.ps1` shim is blocked, use `npm.cmd`):

```powershell
npm.cmd run dev      # Vite dev server (proxies /api to :8080)
npm.cmd run build    # tsc -b && vite build (typecheck + prod bundle)
npm.cmd run lint     # oxlint
```

Database: Flyway auto-runs on backend startup. Postgres via `docker-compose up -d`.
Seed data in `V2__seed_dev_data.sql`; test credentials in `Hints.txt`.

## Stack (do not change without discussion)

- Backend: Java 21, Spring Boot 3.3.13, Spring Security, JPA/Hibernate,
  PostgreSQL 42.7.7, Flyway, JJWT 0.12.6, Twilio SDK 10.4.1, Lombok 1.18.38,
  springdoc-openapi 2.6.0.
- Frontend: React 19, TypeScript, Vite 8, Tailwind 4, TanStack Query 5,
  react-router 7, axios, lucide-react. No shadcn CLI — hand-rolled primitives
  in `frontend/src/components/ui/`.
- Build note: JDK 25 works because Lombok is pinned to 1.18.38 as an annotation
  processor (`pom.xml`). Older Lombok fails on JDK 23+.

## Cross-cutting invariants

- **Backend is the single source of truth.** Never invent endpoints, DTO fields,
  RBAC rules, statuses, metrics, or business logic. Read the actual controller /
  service / DTO before assuming shape.
- **Multi-tenancy.** `restaurantId` comes from `SecurityUtils.currentRestaurantId()`,
  never from a request param or body. Every entity load by id must ownership-check.
- **Auth.** Stateless JWT. Roles: `OWNER > MANAGER > STAFF`. RBAC rules live in
  `SecurityConfig`. `JwtAuthenticationFilter` never throws — malformed tokens
  fall through to the security chain and return 401.
- **Reservation lifecycle & SMS pipeline** are documented in the backend
  `CLAUDE.md`. Do not duplicate here.

## Workflow

1. Before touching a feature, read `docs/PROJECT_STATE.md` for current status
   and the relevant nested `CLAUDE.md` for conventions.
2. Modify **only** files the current task requires. Do not refactor unrelated
   code, rename symbols, or "clean up" as a side effect.
3. Preserve working functionality. If a change forces an API/DTO update,
   update entity + DTO + mapper together (backend) or the API client + types
   together (frontend).
4. Do not add npm or Maven dependencies without asking.
5. Verify: backend `mvn test`; frontend `npm.cmd run build; npm.cmd run lint`.
   State explicitly when a change is compile-verified only vs. exercised in a
   browser/live backend.
6. Final report: files changed, verification performed, next smallest step.
   Keep it terse — no restating architecture, no unchanged-file summaries.

## What lives elsewhere

- Endpoint list, package-level rules, tenant/reservation invariants → backend
  `CLAUDE.md` (`src/main/java/org/example/reservations/CLAUDE.md`).
- Frontend conventions, React Query keys, shared component locations →
  `frontend/CLAUDE.md`.
- Current phase, done/pending pages, live-verification gaps →
  `docs/PROJECT_STATE.md`.
- Path-scoped coding rules → `.claude/rules/`.
