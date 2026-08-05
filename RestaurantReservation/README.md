# Restaurant Reservation & Analytics Platform

A lightweight SaaS platform for Georgian restaurants focused on reservation operations, no-show reduction, table organization, and analytics.

## Overview

This is an **internal operational tool** for restaurants to manage reservations received from phone calls, social media, WhatsApp, and walk-ins. It is not a public booking marketplace, not a POS, and not an inventory or accounting system.

**Goal:** faster than notebooks, simpler than POS systems, easier than Excel.

The system ships as two pieces: a **Spring Boot backend** exposing a JSON API under `/api`, and a **React SPA** in `frontend/` that consumes it. Both live in the same repository. The frontend proxies `/api/**` to the backend during development.

---

## Features

- JWT auth with role hierarchy (`OWNER > MANAGER > STAFF`).
- Multi-tenant by design — every request is scoped to the caller's restaurant via a `restaurantId` JWT claim. Clients never pass `restaurantId` themselves.
- Reservation create / move / cancel / check-in / walk-in.
- Table-level pessimistic locking prevents double-booking under concurrent staff input.
- Status state machine — invalid transitions return 409; updates are idempotent.
- No-show automation with configurable grace period (default 15 min).
- SMS confirmation + 24h reminder via Twilio (mock mode for development). Confirmation SMS is sent **after** the DB commit on a separate thread.
- Dashboard, daily/weekly calendar, peak-hours / busiest-days / no-show analytics.
- Guest history with phone-based deduplication per restaurant.
- React SPA covering login, dashboard, tables (list + floor view), reservations (list, create, move, status actions), calendar, and analytics (KPIs, peak hours, busiest days, day/week/month reservation trends).

---

## Tech Stack

**Backend**
- Java 21 (builds on JDK 21–25)
- Spring Boot 3.3.13
- Spring Security + JWT (`jjwt 0.12.6`)
- Spring Data JPA / Hibernate
- PostgreSQL 16
- Flyway
- Twilio SDK
- Lombok 1.18.38 (pinned + wired as an annotation processor for JDK 25 compatibility)
- Maven

**Frontend**
- React 19 + TypeScript
- Vite 8 (dev server proxies `/api` → `:8080`)
- Tailwind 4 (`@tailwindcss/vite`)
- TanStack Query 5, react-router 7, axios, lucide-react
- Hand-rolled UI primitives in `frontend/src/components/ui/` — no shadcn CLI, no chart library, no `react-hook-form`, no `zod`
- oxlint

---

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.6+
- PostgreSQL 16 (Docker recommended)
- Node.js 20+ and npm (for the frontend)

### Environment variables

```bash
DB_USERNAME=postgres
DB_PASSWORD=postgres

JWT_SECRET=replace-me-with-a-strong-secret

SMS_ENABLED=false
SMS_MOCK_MODE=true

# Only required if SMS_ENABLED=true and SMS_MOCK_MODE=false
TWILIO_ACCOUNT_SID=
TWILIO_AUTH_TOKEN=
TWILIO_FROM_NUMBER=
```

### Run

Backend (repo root):

```bash
docker-compose up -d        # start Postgres
mvn clean install           # build + tests
mvn spring-boot:run         # start on :8080
```

Flyway runs migrations on startup. No manual schema setup needed. `V2` seeds a demo restaurant, three users (one per role), tables, and today's mixed-status reservations. `V4` wipes and reseeds the reservation table with ~9 weeks of realistic historical data so the Analytics page shows meaningful shape out of the box.

Frontend (`frontend/`):

```bash
npm install
npm run dev                 # Vite dev server, proxies /api → :8080
npm run build               # tsc -b && vite build
npm run lint                # oxlint
```

On Windows PowerShell, `npm`'s `.ps1` shim is blocked — use `npm.cmd` instead of `npm`.

### Seeded credentials

| Role | Email | Password |
|---|---|---|
| OWNER | owner@demo.com | password123 |
| MANAGER | manager@demo.com | password123 |
| STAFF | staff@demo.com | password123 |

---

## API

All endpoints except `/api/auth/**` and `GET/POST /api/restaurants` require a `Authorization: Bearer <jwt>` header. The JWT encodes the caller's `restaurantId`, so list/dashboard/analytics endpoints take no `restaurantId` parameter.

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`

### Restaurants (OWNER)
- `GET /api/restaurants`
- `GET /api/restaurants/{id}`
- `POST /api/restaurants`
- `PUT /api/restaurants/{id}`
- `DELETE /api/restaurants/{id}`

### Tables (OWNER, MANAGER)
- `GET /api/tables`
- `GET /api/tables/{id}`
- `POST /api/tables`
- `PUT /api/tables/{id}`
- `DELETE /api/tables/{id}`

### Reservations (OWNER, MANAGER, STAFF)
- `POST /api/reservations` — body: `{ tableId, guestName, guestPhone, guestEmail?, startTime, partySize, notes? }`
- `GET /api/reservations/{id}`
- `GET /api/reservations/daily?date=YYYY-MM-DD`
- `GET /api/reservations/weekly?weekStart=YYYY-MM-DD`
- `GET /api/reservations/available-tables?startTime=...&endTime=...`
- `PATCH /api/reservations/{id}/status?status=...`
- `PATCH /api/reservations/{id}/cancel`
- `POST /api/reservations/walk-in` — body: `{ tableId, guestName, guestPhone?, partySize, notes? }` (immediate `SEATED`)
- `PATCH /api/reservations/{id}/move` — body: `{ tableId?, startTime? }`
- `PATCH /api/reservations/{id}/check-in`

### Guests (OWNER, MANAGER, STAFF)
- `GET /api/guests`
- `GET /api/guests/{id}`
- `GET /api/guests/search?phone=...`
- `POST /api/guests`
- `PUT /api/guests/{id}`
- `DELETE /api/guests/{id}`

### Dashboard (OWNER, MANAGER)
- `GET /api/dashboard`

### Analytics (OWNER, MANAGER)
- `GET /api/analytics?start=YYYY-MM-DD&end=YYYY-MM-DD`

### Calendar
- `GET /api/calendar/daily?date=YYYY-MM-DD`
- `GET /api/calendar/weekly?startDate=YYYY-MM-DD`

### Users
- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users` (OWNER)
- `PUT /api/users/{id}`
- `PUT /api/users/{id}/password`
- `DELETE /api/users/{id}` (OWNER)

---

## Business rules

### Reservation lifecycle
Statuses: `PENDING`, `CONFIRMED`, `SEATED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`.

Allowed transitions:
- `PENDING   → CONFIRMED | CANCELLED`
- `CONFIRMED → SEATED | COMPLETED | NO_SHOW | CANCELLED`
- `SEATED    → COMPLETED | CANCELLED`
- `COMPLETED`, `CANCELLED`, `NO_SHOW` are terminal.

Invalid transitions return **409 Conflict**. Re-applying the same status is a no-op (guest visit/no-show counters are not double-incremented).

### Double-booking prevention
`createReservation` and `moveReservation` take a `PESSIMISTIC_WRITE` lock on the table row before running the conflict check. Concurrent creates against the same table are serialized at the DB; different tables remain parallel.

### Multi-tenant isolation
`restaurantId` is a JWT claim. Controllers do not accept it from clients. Services derive it from the security context and ownership-check any entity fetched by id. Cross-restaurant access returns **403 Forbidden**.

### No-show automation
`NoShowScheduler` runs every 5 minutes and flips `CONFIRMED → NO_SHOW` once `startTime + gracePeriodMinutes` has passed. Grace period is configurable per restaurant (default 15 min).

### SMS pipeline
- **Confirmation:** published as a `ReservationConfirmedEvent` after `createReservation` returns; sent by an `AFTER_COMMIT @Async` listener. Twilio latency or failure cannot affect the reservation.
- **Reminder:** `SmsReminderScheduler` runs every 30 minutes and sends to `CONFIRMED` reservations starting in 1–24 hours that have `reminderSent = false`. The 1-hour floor ensures the guest can still act on the message.

---

## Project structure

```
src/main/java/org/example/reservations/
├── analytics/      Analytics service and controller
├── auth/           JWT service, filter, SecurityUtils (multi-tenant helper)
├── calendar/       Daily/weekly calendar service and controller
├── config/         SecurityConfig (RBAC + filter chain)
├── dashboard/      Dashboard service, controller, DTO
├── exception/      GlobalExceptionHandler + custom exceptions
├── guest/          Guest CRUD
├── notification/   SmsService, SmsReminderScheduler, ReservationSmsListener
├── reservation/    Core reservation logic, status state machine, walk-in/move/check-in
├── restaurant/     Restaurant CRUD
├── table/          Table CRUD + locked lookup
└── user/           User CRUD, password change

src/main/resources/
├── db/migration/   Flyway migrations (V1..)
└── application.yml

frontend/
├── src/
│   ├── api/           axios wrappers, one file per backend domain
│   ├── components/    layout, shared (KpiCard, PageHeader, ErrorState, …), ui primitives
│   ├── features/      one folder per page (analytics, auth, calendar, dashboard,
│   │                  guests, reservations, settings, tables)
│   ├── routes/        router + RequireAuth / RequireRole guards
│   ├── lib/           datetime, roles, storage, strings, query-client, utils
│   └── types/         domain types mirroring backend DTOs
├── vite.config.ts     dev-server proxy for /api
└── tailwind config    via @tailwindcss/vite
```

---

## Configuration

`application.yml` (excerpt):
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: 86400000   # 24h

sms:
  enabled: false
  mock-mode: true

twilio:
  account-sid: ${TWILIO_ACCOUNT_SID:}
  auth-token: ${TWILIO_AUTH_TOKEN:}
  from-number: ${TWILIO_FROM_NUMBER:}
```

Reservation duration and grace period are per-restaurant fields (`defaultReservationMinutes`, `gracePeriodMinutes`), not global config.

---

## Production checklist

- [ ] Strong `JWT_SECRET` (min 32 random bytes)
- [ ] Real DB credentials, not the defaults
- [ ] SSL/TLS in front of the app
- [ ] Twilio credentials configured (or `SMS_ENABLED=false`)
- [ ] Backup strategy for Postgres
- [ ] Monitoring / alerting (the schedulers are silent if they fall over)

> **Migration note:** existing JWTs issued before the multi-tenant security fix do not carry the `restaurantId` claim. After deploying, all users must re-login.

---

## Roadmap (post-MVP)

- Structured per-weekday opening hours
- Best-table suggestion (smallest-fit by capacity)
- Repeat no-show flag surfaced during create
- Guest notes / preferences
- Docker image + compose for the app itself (Postgres compose already exists)
- Test coverage on `ReservationService` (conflict detection, state transitions)

Out of scope until Phase 2/3: public discovery, customer accounts, reviews, payments, AI forecasting.

---

## License

[Add your license here]
