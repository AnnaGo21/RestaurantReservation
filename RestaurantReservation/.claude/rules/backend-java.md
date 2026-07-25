---
description: Backend rules for the Spring Boot / Java 21 reservations service
globs:
  - "src/main/java/org/example/reservations/**/*.java"
  - "src/test/java/org/example/reservations/**/*.java"
alwaysApply: false
---

Tenant isolation (non-negotiable)
- Read `restaurantId` only from `SecurityUtils.currentRestaurantId()`. Never
  from a `@RequestParam`, `@RequestBody`, path variable, or the entity being
  mutated.
- Every service method that loads an entity by id must ownership-check:
  `entity.getRestaurant().getId().equals(SecurityUtils.currentRestaurantId())`.
  Mismatch → throw `AccessDeniedException` (→ 403).
- Controllers must not expose `restaurantId` as a parameter.

Authentication / JWT
- Filter chain lives in `config/SecurityConfig`. Do not add throwing branches
  inside `JwtAuthenticationFilter` — malformed / expired tokens must fall
  through so Spring Security returns 401.
- Role hierarchy `OWNER > MANAGER > STAFF` is enforced in `SecurityConfig`.
  When adding an endpoint, add its `.requestMatchers(...)` rule in the same
  PR.

Transaction boundaries
- Do not call `SmsService` (or any external I/O) inside a `@Transactional`
  method — publish `ReservationConfirmedEvent` and let
  `@TransactionalEventListener(AFTER_COMMIT) @Async` deliver it.
- Concurrent booking on the same table is serialised via
  `tableRepository.findByIdForUpdate(...)`. Reuse that lock for any new
  path (walk-in / move / rebook) — do not `findById`.

Reservation transitions
- Allowed transitions live in `ReservationStatus.canTransitionTo`. If you
  add a status, update the transition matrix, the tests in
  `reservation/ReservationStatusTest`, and the frontend `StatusBadge` +
  `ReservationStatus` type together.
- Status mutators (`updateReservationStatus`, `cancelReservation`,
  `checkIn`) stay idempotent — re-applying the current status must return
  the existing DTO without touching guest counters.
- Conflict queries ignore `CANCELLED`, `NO_SHOW`, `COMPLETED`.

DTO / mapper / validation
- Never return JPA entities from a controller. Add fields to entity + DTO +
  mapper together. Never expose `passwordHash`.
- Request bodies get `@Valid`; the resulting `MethodArgumentNotValidException`
  is mapped to 400. Do not catch it locally.
- Error → status mapping is centralised in `exception/GlobalExceptionHandler`.
  Throw the semantic exception (`ResourceNotFoundException`,
  `TableUnavailableException`, `IllegalStateException`,
  `AccessDeniedException`) rather than building a `ResponseEntity` in-line.

Testing
- Tests live under `src/test/java/org/example/reservations/`, grouped by
  package. Add tests next to the code they cover.
- `mvn test` for the suite; `mvn test -Dtest=ClassName#method` for a single
  method.

Scope discipline
- If the current task is a frontend feature, do not modify Java files just
  because you spotted an unrelated code-smell. Backend changes must have
  their own justification.
- Flyway migrations are append-only. Add `V{N+1}__…sql`; do not edit
  existing `V*` files.
