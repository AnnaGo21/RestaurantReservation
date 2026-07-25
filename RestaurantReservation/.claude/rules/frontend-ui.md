---
description: Frontend UI conventions for the React/Vite/Tailwind SPA
globs:
  - "frontend/src/**/*.ts"
  - "frontend/src/**/*.tsx"
  - "frontend/src/**/*.css"
alwaysApply: false
---

Reuse before invent
- Check `frontend/src/components/shared/` and `components/ui/` first. Extend
  a shared component in place rather than forking a copy into a feature folder.
- API calls live in `frontend/src/api/*.ts`. Never call `axios` directly from
  a component or add a new HTTP path without also adding the wrapper.
- Query hooks live in `features/*/use-*.ts`. Reuse or extend before making
  a new key.

Design system
- shadcn-style visual language, but no shadcn CLI. Hand-rolled primitives in
  `components/ui/` only — do not install `@radix-ui/*`, `react-hook-form`,
  `zod`, or a chart library without explicit approval.
- Use `cn()` from `lib/utils.ts`. Prefer semantic tokens
  (`text-foreground`, `bg-muted`, `border-border`) defined in `src/index.css`;
  match the shade choices in neighbouring files if you need raw `slate-*`.

Status colours (frozen)
- Reservation: PENDING amber, CONFIRMED blue, SEATED emerald, COMPLETED slate,
  CANCELLED slate-muted, NO_SHOW red. Render via `<StatusBadge status={…} />`.
- Table: AVAILABLE emerald, RESERVED blue, OUT_OF_SERVICE slate — see the
  `statusClasses` map in `features/tables/TablesPage.tsx`.

Accessibility
- Every icon-only button needs `aria-label`.
- Every input needs a `<Label>` or `aria-label`.
- Decorative icons inside badges/labels get `aria-hidden="true"`.

Responsive
- Target desktop and landscape tablet first. Wrap wide tables in
  `overflow-x-auto`. Use `sm:` breakpoints only where existing code does.
  Mobile is best-effort.

Do not invent backend APIs
- Endpoints, DTO fields, and statuses must match the actual backend. Before
  adding a new field to a type in `frontend/src/types/`, confirm the DTO
  exists in `src/main/java/org/example/reservations/**/dto` (or `*Dto.java`).

Scope discipline
- Modify only the files the current slice requires. Do not restructure
  `components/ui/*` or `components/shared/*` while building a feature page.
- Do not rename symbols, reorder imports en masse, or "clean up" as a side
  effect of a feature change.
