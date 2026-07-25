# PROJECT_STATE.md

Snapshot of feature status as of 2026-07-25. Update the affected sections at
the end of each phase — do not rewrite the whole document.

## Completed pages (frontend)

Compile-verified with `npm.cmd run build; npm.cmd run lint`. **Nothing has
been exercised against a running backend / Postgres** — see "Live gaps" below.

- **Login** (`features/auth/LoginPage.tsx`) — email + password, JWT stored in
  `localStorage`, redirects via `HomeRedirect`.
- **AppShell + Sidebar + Topbar** (`components/layout/`) — responsive
  desktop / landscape-tablet layout, role-aware nav items.
- **Dashboard** (`features/dashboard/`) — KPIs (todayReservations,
  upcoming, occupied tables, no-show %), 60 s polling. OWNER/MANAGER only.
- **Tables** (`features/tables/`) — full CRUD, floor view that scales boxes
  with capacity (positions optional). OWNER/MANAGER only.
- **Reservations** (`features/reservations/`) — daily list with
  `?date=&status=&q=` URL state, detail modal, create modal with
  availability chips, status actions (check-in / complete / no-show /
  cancel mapped to backend transitions), move/reschedule modal.
- **Calendar** (`features/calendar/`) — daily table-vs-time grid.

## Placeholder pages (still incomplete)

Each renders `<PagePlaceholder />` only; route + nav entry are wired.

- Analytics (`features/analytics/AnalyticsPage.tsx`)
- Guests (`features/guests/GuestsPage.tsx`)
- Settings (`features/settings/SettingsPage.tsx`)

## Implemented reservation workflows (backend)

- Create (pessimistic table lock, conflict detection, auto guest
  dedup/create, publishes `ReservationConfirmedEvent`).
- Walk-in (immediate `SEATED`, anonymous phone → synthetic `walkin-{uuid}`).
- Move / reschedule (same-lock + `findConflictsExcluding`).
- Check-in / cancel / status change — idempotent, transition-checked.
- Daily reservations list (`/api/reservations/daily?date=…`).
- Available tables (`/api/reservations/available-tables?startTime=&endTime=`).
- Async SMS after commit; 30-min reminder scheduler; 5-min no-show scheduler.

## Known API limitations

- **Wrong password → HTTP 500.** `BadCredentialsException` is not mapped in
  `GlobalExceptionHandler`. Frontend surfaces the generic error string.
  Fix requires a dedicated handler (open issue).
- **No pagination.** `/api/reservations/daily` returns a full-day `List`.
  Fine for expected volumes; revisit if a restaurant exceeds ~200/day.
- **`GET /api/tables` is OWNER/MANAGER-only.** STAFF UI catches 403 and
  falls back to `#{tableId}` labels. Consider opening for STAFF later.
- **Reservation listing is day-scoped only.** No range/search endpoint yet.

## Live / runtime gaps

- No page has been opened in a browser against a running backend and
  Postgres. First live run should validate:
  1. Login (all three seeded roles).
  2. Reservation create → 409 conflict path.
  3. Dashboard KPIs populate.
  4. Move + check-in idempotency.
- SMS runs in mock mode (`SMS_MOCK_MODE=true`) by default — Twilio has
  never been exercised for real.
- Bad-login 500 handling not yet observed live.

## Design-system decisions currently locked

- **No shadcn CLI, no react-hook-form, no zod, no chart lib.** Primitives
  in `components/ui/` are hand-rolled; forms are manual with inline
  validation. Reason: keep dependency surface small and reviewable.
- Status colour palette (frozen across app):
  - Reservation — PENDING amber, CONFIRMED blue, SEATED emerald,
    COMPLETED slate, CANCELLED slate-muted, NO_SHOW red.
  - Table — AVAILABLE emerald, RESERVED blue, OUT_OF_SERVICE slate.
- Semantic Tailwind tokens (`foreground`, `muted`, `border`) come from
  `src/index.css`; prefer them over raw shades in new code.
- Target: desktop + landscape tablet. Mobile is best-effort only.

## Current phase

**Next: Analytics page.** Wire the existing `/api/analytics/**` endpoints
(peak hours, busiest days, no-show rate) into `AnalyticsPage.tsx` using the
same PageHeader + Card + KpiCard patterns already in Dashboard. Do not
introduce a chart library — start with numeric summaries and simple bar
lists built from Tailwind.

Sequence after Analytics: Guests, then Settings. Only after those:
first live end-to-end browser test with a running backend.

## Decisions future sessions must preserve

- Backend is the source of truth for DTOs, statuses, and RBAC — mirror
  changes, do not invent them.
- Do not add npm/Maven dependencies without explicit approval.
- Do not refactor `components/ui/` or `components/shared/` while building
  a feature page.
- Modals use `components/ui/dialog.tsx` + hand-rolled forms; do not
  reintroduce a form library.
- Query keys are shallow, domain-namespaced arrays; mutations invalidate
  by the top-level domain key.
- Any change to `AuthProvider`, `api/client.ts`, `RequireRole`, or
  `StatusBadge` touches every page — verify all "done" features still
  compile.
