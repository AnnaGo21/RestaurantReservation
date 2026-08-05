# Changelog

## Frontend

### Shipped

- **Login** (`features/auth/LoginPage.tsx`) — email + password, JWT persisted in `localStorage`, post-login redirect via `HomeRedirect`.
- **App shell** (`components/layout/`) — sidebar + topbar, responsive desktop / landscape-tablet layout, role-aware navigation.
- **Dashboard** (`features/dashboard/`) — today's reservations, upcoming, occupied tables, no-show rate; 60-second polling. OWNER / MANAGER only.
- **Tables** (`features/tables/`) — full CRUD; floor view that scales boxes by capacity (positions optional). OWNER / MANAGER only.
- **Reservations** (`features/reservations/`) — daily list with `?date=&status=&q=` URL state, detail modal, create modal with availability chips, status actions (check-in / complete / no-show / cancel), move / reschedule modal.
- **Calendar** (`features/calendar/`) — daily table-vs-time grid.
- **Analytics** (`features/analytics/`) — date-range picker (default: last 30 days), 5 KPI cards (total, completed, cancelled, no-shows with percentage, average party size), peak-hours ranked bar list, busiest-days ranked bar list (Mon–Sun order), Day / Week / Month reservation-trends vertical bar chart. Zero-count buckets filtered; empty state on zero activity.

### Pending

- Guests page.
- Settings page.

## Backend

### Shipped

- Reservation lifecycle: create, walk-in, move, check-in, cancel, and status transitions (`PENDING → CONFIRMED → SEATED → COMPLETED`, plus `CANCELLED` / `NO_SHOW`).
- Multi-tenant isolation via `restaurantId` JWT claim; never accepted from the client.
- Pessimistic table locking on create / move to prevent double-booking under concurrent staff input.
- Idempotent status mutators; invalid transitions return 409.
- Async confirmation SMS after DB commit; 30-minute reminder scheduler; 5-minute no-show scheduler.
- Analytics endpoint returning peak hours, busiest days, no-show rate, and daily / weekly / monthly reservation trends.
- Dashboard endpoint aggregating today's KPIs and table occupancy.

### Known limitations

- `POST /api/auth/login` with the wrong password returns HTTP 500 (`BadCredentialsException` is not mapped in `GlobalExceptionHandler`).
- `GET /api/reservations/daily` returns a full-day list with no pagination. Fine for expected volumes; revisit past ~200 reservations/day.
- `GET /api/tables` is OWNER / MANAGER only. STAFF gets 403 and the SPA falls back to `#{tableId}` labels.
- Reservation listing is day-scoped only; no range / search endpoint.
- `GET /api/analytics` requires both `start` and `end`. Missing either returns HTTP 500 (`MissingServletRequestParameterException` unmapped). `start > end` returns 200 with empty maps (no server-side validation — the SPA guards this client-side).
- `GET /api/analytics` is timezone-naive: `start.atStartOfDay()` and `end.atTime(MAX)` are treated as server-local, so a client in a different timezone can see off-by-one-day drift at the range boundary.
- `AnalyticsResponse` is aggregate-only. No revenue, occupancy-rate, guest-retention, or period-over-period comparison data.
- SMS runs in mock mode (`SMS_MOCK_MODE=true`) by default. Twilio has not been exercised in production.

## Design decisions currently locked

- No `shadcn` CLI, no `react-hook-form`, no `zod`, no chart library. UI primitives are hand-rolled in `frontend/src/components/ui/`; forms are manual with inline validation. Rationale: keep the dependency surface small and reviewable.
- Reservation status palette: PENDING amber, CONFIRMED blue, SEATED emerald, COMPLETED slate, CANCELLED slate-muted, NO_SHOW red. Table status palette: AVAILABLE emerald, RESERVED blue, OUT_OF_SERVICE slate.
- Semantic Tailwind tokens (`foreground`, `muted`, `border`) come from `src/index.css`; prefer them over raw shades in new code.
- Target: desktop + landscape tablet. Mobile is best-effort only.
- Backend is the source of truth for DTOs, statuses, and RBAC. Mirror on the frontend; never invent.
- Query keys are shallow, domain-namespaced arrays; mutations invalidate by the top-level domain key.
- Changes to `AuthProvider`, `api/client.ts`, `RequireRole`, or `StatusBadge` touch every page — recheck all shipped features still compile after edits to those files.
