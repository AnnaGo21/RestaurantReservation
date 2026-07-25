# frontend/CLAUDE.md

React 19 + TypeScript SPA. Alias `@` → `frontend/src` (see `vite.config.ts`).
Backend contract is authoritative — never invent DTO fields or endpoints;
inspect `src/api/*.ts` + `src/types/*.ts` against the actual backend DTO first.

## Stack

React 19, TypeScript, Vite 8, Tailwind 4 (`@tailwindcss/vite`),
TanStack Query 5, react-router 7 (data router), axios, lucide-react,
clsx + tailwind-merge, oxlint. **No shadcn CLI, no react-hook-form, no zod,
no chart lib.** UI primitives are hand-rolled in `components/ui/`.

Commands (PowerShell — `npm`'s `.ps1` shim is blocked; use `npm.cmd`):

```powershell
npm.cmd run dev     # Vite, proxies /api → :8080
npm.cmd run build   # tsc -b && vite build (typecheck + build)
npm.cmd run lint    # oxlint
```

## Source layout

```
src/
  api/         axios wrappers (one file per backend domain). All go through client.ts.
  components/
    layout/    AppShell, Sidebar, Topbar
    shared/    PageHeader, DateNav, FilterBar, SearchInput, EmptyState,
               ErrorState, ListSkeleton, KpiCard, ConfirmDialog,
               SectionHeader, StatusBadge, ErrorPage, PagePlaceholder
    ui/        button, card, dialog, input, select, textarea, label, badge,
               spinner, skeleton, dropdown-menu, toast, toast-context
  features/    One folder per page (analytics, auth, calendar, dashboard,
               guests, reservations, settings, tables) with *Page.tsx,
               use-*.ts hooks, and modals.
  routes/      Router + guards (RequireAuth, RequireRole, HomeRedirect)
  lib/         datetime, query-client, roles, storage (JWT), strings, utils
  types/       Domain types mirroring backend DTOs
```

## Router & auth

`routes/index.tsx` — `RequireAuth` gates everything except `/login`, `/403`, `*`.
`RequireRole roles={MANAGEMENT_ROLES}` gates `/dashboard`, `/tables`,
`/analytics`. `HomeRedirect` sends STAFF to `/reservations` and OWNER/MANAGER
to `/dashboard` (see `lib/roles.ts`).

Auth state in `features/auth/AuthProvider.tsx`; JWT persists in `lib/storage.ts`
(`localStorage`). `api/client.ts` attaches `Bearer` and, on a 401 outside
`/auth/*`, clears storage and calls the handler registered via
`setOnUnauthorized`. No hard reloads.

## API client & React Query

- All HTTP goes through `api` in `api/client.ts` (base `VITE_API_URL ?? /api`).
- Errors normalised to `ApiError { message, status }`; prefer `error.message`
  for user-facing text.
- Query keys are shallow, domain-namespaced arrays: `['tables']`,
  `['dashboard']`, `['reservations', 'daily', date]`. Reuse existing hooks
  under `features/*/use-*.ts` before inventing keys.
- Dashboard + daily reservations poll on `refetchInterval: 60_000`.
- Tables query is `retry: false` — STAFF gets 403; callers fall back to
  `#{tableId}` labels.
- Mutations invalidate by top-level domain key (e.g. `['tables']`).

## UI patterns to reuse

- **Page frame:** `PageHeader` (title + actions) → optional `FilterBar` →
  content `Card`. See `ReservationsPage`, `TablesPage`.
- **Load / error / empty:** `ListSkeleton`, `ErrorState onRetry={refetch}`,
  `EmptyState icon={<Lucide />} title`. No ad-hoc versions.
- **Modals & forms:** `components/ui/dialog.tsx` + hand-rolled forms. See
  `CreateReservationModal`, `TableFormModal`. Validation is manual/inline.
- **Confirm destructive:** `ConfirmDialog destructive isPending error`.
- **Status pill:** `StatusBadge status={ReservationStatus}` for reservations;
  table statuses use the `statusClasses` map in `TablesPage.tsx`.
- **Toasts:** `useToast()` from `components/ui/toast-context`.

## Design system

- Tailwind + `cn()` from `lib/utils.ts`. Prefer semantic tokens
  (`text-foreground`, `bg-muted`, `border-border`) from `src/index.css`.
- Status palette (frozen): PENDING amber, CONFIRMED blue, SEATED emerald,
  COMPLETED slate, CANCELLED slate-muted, NO_SHOW red. Table statuses:
  AVAILABLE emerald, RESERVED blue, OUT_OF_SERVICE slate.
- Target desktop + landscape tablet. Wide tables use `overflow-x-auto`.
  Mobile best-effort only.
- Accessibility: icon-only buttons need `aria-label`; inputs need `<Label>`
  or `aria-label`; decorative icons get `aria-hidden="true"`.

## Do not break

Compile-verified & "done" (nothing browser-tested yet — see
`docs/PROJECT_STATE.md`): Login, AppShell/Sidebar/Topbar, Dashboard,
Tables (CRUD + floor view), Reservations (list, detail, create with
availability chips, status actions, move), Calendar.

Changes to `routes/`, `api/client.ts`, `AuthProvider`, `RequireRole`, or
`StatusBadge` affect every page — recheck each done feature compiles.

## Scope discipline

Touch only files the current slice requires. Do not regenerate unchanged
files, restructure `components/ui/*` or `components/shared/*`, or rename
symbols as a side effect. If a shared component is *almost* what you need,
extend it in place — don't fork.
