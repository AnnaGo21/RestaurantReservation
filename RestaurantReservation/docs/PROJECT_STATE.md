# PROJECT_STATE.md

Snapshot of feature status as of 2026-07-27. Update the affected sections at
the end of each phase — do not rewrite the whole document.

## Completed pages (frontend)

All pages below are compile-verified with `npm.cmd run build; npm.cmd run lint`.
Live-browser status is called out per page — see "Live gaps" for exactly what
was and was not exercised.

- **Login** (`features/auth/LoginPage.tsx`) — email + password, JWT stored in
  `localStorage`, redirects via `HomeRedirect`. Opened in a browser against
  the running backend.
- **AppShell + Sidebar + Topbar** (`components/layout/`) — responsive
  desktop / landscape-tablet layout, role-aware nav items. Rendered against
  the running backend.
- **Dashboard** (`features/dashboard/`) — KPIs (todayReservations,
  upcoming, occupied tables, no-show %), 60 s polling. OWNER/MANAGER only.
  Opened in a browser against the running backend.
- **Tables** (`features/tables/`) — full CRUD, floor view that scales boxes
  with capacity (positions optional). OWNER/MANAGER only. Opened in a
  browser against the running backend; individual mutations not each
  systematically exercised.
- **Reservations** (`features/reservations/`) — daily list with
  `?date=&status=&q=` URL state, detail modal, create modal with
  availability chips, status actions (check-in / complete / no-show /
  cancel mapped to backend transitions), move/reschedule modal. Page
  opened in a browser against the running backend; mutation and
  conflict/error paths (create → 409, move, check-in, cancel, no-show)
  not yet exercised end-to-end.
- **Calendar** (`features/calendar/`) — daily table-vs-time grid. Opened
  in a browser against the running backend.
- **Analytics** (`features/analytics/`) — date-range picker (defaults to
  last 30 days), 5 KPI cards (totalReservations, completedReservations,
  cancelledReservations, noShows with `noShowPercentage` hint,
  averagePartySize), peak-hours ranked bar list (`HH:00` labels, sorted
  by count desc), busiest-days ranked bar list (fixed Mon–Sun order,
  count desc within populated weekdays), and a trends card with a
  Day/Week/Month segmented toggle bound to `dailyTrends` /
  `weeklyTrends` / `monthlyTrends`. Bar widths computed as
  `count / max(counts)` with a zero-guard; exact counts rendered
  beside every bar with `aria-label`. OWNER/MANAGER only. API-verified
  live for OWNER (200) / MANAGER (200) / STAFF (403) / anonymous (403);
  real payload shape matches `AnalyticsResponse`. UI opened in a browser
  against the running backend; every backend-returned metric renders and
  the empty-range path returns all-zero KPIs + `EmptyState` per section
  without divide-by-zero.

## Placeholder pages (still incomplete)

Each renders `<PagePlaceholder />` only; route + nav entry are wired.

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
- **`/api/analytics` missing `start` or `end` → HTTP 500.**
  `MissingServletRequestParameterException` is not mapped in
  `GlobalExceptionHandler`. Frontend never triggers this (both inputs
  default to a valid ISO date on mount), but the endpoint should be
  fixed alongside the bad-login handler.
- **`/api/analytics` with `start > end` → HTTP 200** with all-empty maps
  (no server-side validation). Frontend guards this client-side with
  `enabled: start <= end` on the query plus an inline warning.
- **`/api/analytics` is timezone-naive.** `start.atStartOfDay()` and
  `end.atTime(MAX)` are treated as server-local, so a client in a
  different tz can see off-by-one-day drift at the range boundary.
- **`AnalyticsResponse` is aggregate-only.** No revenue, occupancy-rate,
  guest-retention, or period-over-period comparison data — the frontend
  cannot render metrics the backend does not expose.

## Live / runtime gaps

Live-browser status per page — "opened" means the page rendered against
the running backend and no runtime crash was observed. It does **not**
mean every workflow, mutation, or error branch on the page has been
exercised.

- **Opened in a browser against running backend + Postgres:** Login,
  Dashboard, Reservations (list/detail views only), Calendar, Tables
  (list/floor views only), Analytics.
- **Analytics — API-level checks also performed:** OWNER/MANAGER 200,
  STAFF/anonymous 403; real payload matches `AnalyticsResponse`; empty
  range returns all-zero maps and renders `EmptyState` per section.
- **Reservation mutations and error paths still pending live
  verification:** create → 409 conflict, move/reschedule, check-in,
  cancel, mark no-show, status-transition 409s.
- **Tables mutations still pending live verification:** create, edit,
  delete, position drag, status change.
- **Login negative path still pending:** bad-password 500 (unmapped
  `BadCredentialsException`) not yet observed live.
- **Auth expiry path still pending:** 401 → `setOnUnauthorized` clearing
  storage and routing to `/login` not yet observed live.
- **SMS runs in mock mode** (`SMS_MOCK_MODE=true`) by default — Twilio
  has never been exercised for real; the reminder + no-show schedulers
  have not been observed firing against real data.

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

**Next: Guests page.** Analytics is done (compile-verified + browser-opened
against the running backend, with API-level RBAC and payload checks). The
`AnalyticsPage.tsx` implementation lives at `features/analytics/` alongside
`use-analytics.ts`; the shared type is `types/analytics.ts` and the HTTP
wrapper is `api/analytics.ts`. Strings live under `strings.analytics.*`.

Sequence after Guests: Settings. In parallel, work through the pending
mutation/error paths listed in "Live / runtime gaps" so the
already-opened pages graduate from "rendered live" to "exercised live".

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
