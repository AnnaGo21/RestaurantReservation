# backend CLAUDE.md

Spring Boot 3.3.13 / Java 21 layered monolith under `org.example.reservations`.
Do not duplicate frontend or product info from the root `CLAUDE.md`.

## Packages

```
auth/          JwtAuthenticationFilter, JwtService, CustomUserDetailsService, SecurityUtils
config/        SecurityConfig — filter chain, CORS, RBAC
analytics/     Peak hours, busiest days, no-show rates
calendar/      Daily / weekly views
dashboard/     Real-time overview
guest/         Guest CRUD, phone dedup per restaurant
notification/  Twilio SmsService, SmsReminderScheduler, ReservationSmsListener
reservation/   Booking, conflicts, statuses, walk-in/move/check-in, NoShowScheduler
restaurant/    CRUD + config (grace period, slot duration)
table/         RestaurantTable CRUD + status
user/          User CRUD, profile/password
exception/     GlobalExceptionHandler
```

`RestaurantReservationApplication` has `@EnableAsync` + `@EnableScheduling`.
Migrations: `src/main/resources/db/migration/V{N}__…sql`. Tests:
`src/test/java/org/example/reservations/{analytics,auth,dashboard,reservation}/`.

## Auth & JWT

Stateless JWT with claims `sub` (email), `role`, `restaurantId`.
`JwtAuthenticationFilter` extracts them and stores `restaurantId` on
`authentication.setDetails(...)`. The filter **must never throw** —
malformed/expired tokens fall through so the chain returns 401.

Role hierarchy `OWNER > MANAGER > STAFF`. `SecurityConfig` rules:

| Path | Roles |
|---|---|
| `/api/auth/**`, `GET/POST /api/restaurants` | public |
| `/api/restaurants/**` | OWNER |
| `/api/tables/**` | OWNER, MANAGER |
| `/api/reservations/**`, `/api/guests/**` | OWNER, MANAGER, STAFF |
| `/api/dashboard/**`, `/api/analytics/**` | OWNER, MANAGER |

## Tenant isolation (non-negotiable)

- `restaurantId` only from `SecurityUtils.currentRestaurantId()` — never from
  a `@RequestParam`, `@RequestBody`, path variable, or the mutated entity.
- Any entity loaded by id must ownership-check
  (`entity.getRestaurant().getId().equals(SecurityUtils.currentRestaurantId())`);
  mismatch → `AccessDeniedException` (→ 403).
- Controllers must not expose `restaurantId` as a parameter.

## Reservation lifecycle

Statuses: `PENDING`, `CONFIRMED`, `SEATED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`.
Transitions (`ReservationStatus.canTransitionTo`):
`PENDING → CONFIRMED|CANCELLED`; `CONFIRMED → SEATED|COMPLETED|NO_SHOW|CANCELLED`;
`SEATED → COMPLETED|CANCELLED`. Terminal: `COMPLETED`, `CANCELLED`, `NO_SHOW`.

Status mutators (`updateReservationStatus`, `cancelReservation`, `checkIn`) are
**idempotent** — re-applying the current status returns the current DTO without
touching guest counters. Invalid transitions → `IllegalStateException` → 409.

## Table locking & conflicts

`ReservationService.createReservation()`:

1. `restaurantId` from `SecurityUtils`.
2. `tableRepository.findByIdForUpdate(id)` — `PESSIMISTIC_WRITE` serialises
   concurrent creates per table; different tables stay parallel.
3. Ownership + capacity check (party ≤ capacity).
4. `endTime = startTime + restaurant.defaultReservationMinutes` (default 90).
5. `findConflicts()` ignores `CANCELLED`, `NO_SHOW`, `COMPLETED`.
6. Guest resolved by `(restaurantId, phone)` or created.
7. Save with `CONFIRMED`.
8. Publish `ReservationConfirmedEvent`. **Never** call `SmsService` inside a
   `@Transactional` — Twilio latency/failure must not touch the DB tx.

`walkIn`, `move`, `checkIn` reuse the same lock + capacity check. `move`
uses `findConflictsExcluding(...)`. Walk-ins skip the event.

## SMS & schedulers

- `ReservationSmsListener` — `@TransactionalEventListener(AFTER_COMMIT) @Async`.
- `SmsReminderScheduler` — every 30 min; `CONFIRMED` 1–24 h out, `reminderSent = false`.
- `NoShowScheduler` — every 5 min; marks `CONFIRMED` → `NO_SHOW` past
  `startTime + gracePeriodMinutes`; skips rows that can't transition.

## DTOs, mappers, errors

- One DTO per read/write shape (`*Dto`, `*Request`). Never expose
  `passwordHash` or JPA entities.
- Static-style `*Mapper` per package: entity → DTO. Add fields to entity +
  DTO + mapper together.
- `@Valid` on request bodies → 400 via `MethodArgumentNotValidException`.

| Exception | Status |
|---|---|
| `ResourceNotFoundException` | 404 |
| `MethodArgumentNotValidException` | 400 |
| `TableUnavailableException` | 409 |
| `IllegalStateException` (invalid transition) | 409 |
| `AccessDeniedException` (tenant violation) | 403 |
| anything else | 500 (generic body — no exception detail leaked) |

Known gap: bad login → 500 (`BadCredentialsException` unmapped). Swagger UI: `/swagger-ui/index.html`.

## Tests

`mvn test`; single: `mvn test -Dtest=ClassName#method`. Add tests next to code.

## Invariants the frontend depends on

- `ReservationStatus` values/names frozen — the SPA maps 1:1.
- `/api/reservations/daily?date=YYYY-MM-DD` returns a full-day list; no pagination.
- `GET /api/tables` is OWNER/MANAGER-only — STAFF gets 403 and the SPA
  falls back to `#{tableId}` labels.
- Anonymous walk-ins are stored with synthetic `walkin-{uuid}` phones so
  they don’t collide on the `(restaurantId, phone)` unique index.
