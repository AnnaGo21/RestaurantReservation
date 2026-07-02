# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Purpose

Internal SaaS tool for Georgian restaurants to manage reservations digitally — replacing notebooks, WhatsApp, and Excel. Subscription model (50–400+ GEL/month). Focus on daily operational use, not a public booking marketplace.

## Commands

### Build & Run
```bash
mvn clean install          # Build and run all tests
mvn spring-boot:run        # Start dev server on port 8080
```

### Tests
```bash
mvn test                                       # Run all tests
mvn test -Dtest=ReservationServiceTest         # Single test class
mvn test -Dtest=ReservationServiceTest#testX   # Single test method
```

### Database
Flyway runs migrations automatically on startup. Migrations live in `src/main/resources/db/migration/`. Name new files `V{N}__description.sql`.

### Environment Variables
```
JWT_SECRET          # HMAC secret for JWT signing
TWILIO_ACCOUNT_SID  # Optional in mock mode
TWILIO_AUTH_TOKEN
TWILIO_FROM_NUMBER
```

SMS runs in mock mode (logs only) by default in development.

### Build environment
- Java 21 source/target, but the project builds cleanly on JDK 25.
- Lombok is pinned to `1.18.38` in `pom.xml` and wired as an annotation processor on `maven-compiler-plugin` — older Lombok versions fail on JDK 23+ with `TypeTag :: UNKNOWN`.
- Spring Boot parent: `3.3.13`.

## Architecture

Layered Spring Boot monolith. Packages under `org.example.reservations`:

```
auth/          JWT auth filter, JwtService, CustomUserDetailsService, SecurityUtils
config/        SecurityConfig — RBAC rules and filter chain
analytics/     Trend/stats endpoints (peak hours, busiest days, no-show rates)
calendar/      Daily and weekly table-reservation view
dashboard/     Real-time restaurant overview (occupied tables, today's bookings)
guest/         Guest CRUD; phone-based deduplication per restaurant
notification/  Twilio SMS service + 30-minute reminder scheduler + AFTER_COMMIT listener
reservation/   Core booking logic, conflict detection, status lifecycle, walk-in/move/check-in
restaurant/    Restaurant CRUD and configuration (grace period, slot duration)
table/         Table CRUD and status management
user/          User management, role enforcement, profile/password updates
exception/     GlobalExceptionHandler → 400/403/404/409/500 responses
```

### Security & Multi-Tenancy

JWT is stateless. On login the token carries three claims: `sub` (email), `role`, and `restaurantId`. The filter (`JwtAuthenticationFilter`) extracts all three, builds the `Authentication`, and stores `restaurantId` on `authentication.setDetails(...)`.

**Never trust `restaurantId` from a request param or body.** Always read it via `SecurityUtils.currentRestaurantId()`. Controllers must not expose `restaurantId` as a `@RequestParam`. Services must ownership-check any entity loaded by id (`entity.getRestaurant().getId().equals(SecurityUtils.currentRestaurantId())` — throw `AccessDeniedException` otherwise).

Role hierarchy: `OWNER > MANAGER > STAFF`. Access matrix from `SecurityConfig`:

| Path | Roles |
|------|-------|
| `/api/auth/**`, `GET /api/restaurants`, `POST /api/restaurants` | Public |
| `/api/restaurants/**` | OWNER |
| `/api/tables/**` | OWNER, MANAGER |
| `/api/reservations/**`, `/api/guests/**` | OWNER, MANAGER, STAFF |
| `/api/dashboard/**`, `/api/analytics/**` | OWNER, MANAGER |

`JwtAuthenticationFilter` swallows malformed/expired tokens and lets the security chain return 401 — do not throw from the filter.

### Reservation Lifecycle

Statuses: `PENDING`, `CONFIRMED`, `SEATED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`.

Allowed transitions (defined on `ReservationStatus.canTransitionTo`):
- `PENDING   → CONFIRMED | CANCELLED`
- `CONFIRMED → SEATED | COMPLETED | NO_SHOW | CANCELLED`
- `SEATED    → COMPLETED | CANCELLED`
- Terminal: `COMPLETED`, `CANCELLED`, `NO_SHOW`.

All status mutators (`updateReservationStatus`, `cancelReservation`, `checkIn`) are **idempotent**: re-applying the same status returns the existing DTO without touching guest counters. Invalid transitions throw `IllegalStateException` (→ HTTP 409).

### Core flow — `ReservationService.createReservation()`

1. `restaurantId` is read from `SecurityUtils.currentRestaurantId()`, **not** the request.
2. Table is loaded via `tableRepository.findByIdForUpdate(...)` — a `PESSIMISTIC_WRITE` lock that serializes concurrent creates per table. Different tables remain parallel.
3. Ownership check: table must belong to caller's restaurant; party size ≤ capacity.
4. `endTime = startTime + restaurant.defaultReservationMinutes` (default 90).
5. `findConflicts()` checks overlap, ignoring `CANCELLED`, `NO_SHOW`, `COMPLETED`.
6. Guest is looked up by `(restaurantId, phone)` or created.
7. Reservation is saved with status `CONFIRMED`.
8. **`ReservationConfirmedEvent` is published — SMS is sent asynchronously after commit (see "SMS pipeline" below). Do not call `SmsService` directly from a transactional method.**

### Walk-in / move / check-in

- `POST /api/reservations/walk-in` — creates a reservation starting now, immediately `SEATED` with `checkedInAt` set. Phone is optional; when blank, a synthetic `walkin-{uuid}` is stored so anonymous walk-ins don't merge into a single guest row.
- `PATCH /api/reservations/{id}/move` — body `{ tableId?, startTime? }`. Rejects terminal states. Locks the new table and runs `findConflictsExcluding(...)` so the reservation being moved doesn't conflict with itself.
- `PATCH /api/reservations/{id}/check-in` — sets `status=SEATED` and `checkedInAt=now()`. Idempotent.

All three use the same lock + capacity check as `createReservation`.

### SMS pipeline

`ReservationService.createReservation()` publishes `ReservationConfirmedEvent`. `ReservationSmsListener` handles it with `@TransactionalEventListener(AFTER_COMMIT) @Async` — Twilio latency cannot hold a DB transaction open, and Twilio failure cannot roll back a valid reservation. `@EnableAsync` is set on `RestaurantReservationApplication`.

Walk-ins skip the event (the guest is already at the table).

### Dashboard data shape

`DashboardDto`:
- `todayReservations` — every reservation today regardless of status (full picture for staff).
- `upcomingReservations` — today's reservations starting after now, excluding `CANCELLED`, `NO_SHOW`, `COMPLETED` (actionable list).
- `occupiedTables` — tables with an active reservation whose window contains `now`.
- `noShowPercentage` — all-time, not daily, so it's a stable health metric.

### Scheduled tasks

- `NoShowScheduler` — every 5 minutes. Marks `CONFIRMED` reservations as `NO_SHOW` once `startTime + gracePeriodMinutes` has passed. Skips rows whose status can no longer transition to `NO_SHOW`.
- `SmsReminderScheduler` — every 30 minutes. Sends reminders for `CONFIRMED` reservations starting in 1–24 hours with `reminderSent = false`. The 1-hour floor lets the guest still act; the flag prevents duplicates.

Both require `@EnableScheduling` on the application class.

### Mappers

Each domain package has a `*Mapper` that converts JPA entities → DTOs. Mappers are simple static-style converters — **never expose `passwordHash`**. Add new fields to the entity, the DTO, and the mapper together.

### Database Migrations

`V1` creates the full schema with indexes and constraints. Critical indexes for performance:
- `reservations(restaurant_id, start_time)` — date-range queries
- `reservations(restaurant_table_id, start_time, end_time)` — conflict detection
- `guests(restaurant_id, phone)` — UNIQUE, enforces per-restaurant deduplication

### Error mapping (`GlobalExceptionHandler`)

| Exception | Status |
|---|---|
| `ResourceNotFoundException` | 404 |
| `MethodArgumentNotValidException` | 400 |
| `TableUnavailableException` | 409 |
| `IllegalStateException` (invalid status transitions) | 409 |
| `AccessDeniedException` (tenant violation) | 403 |
| anything else | 500 (generic body — no exception detail leaked) |
