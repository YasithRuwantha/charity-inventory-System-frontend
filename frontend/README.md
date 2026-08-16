# Charity Inventory Management System — Frontend

React + TypeScript frontend for the [Charity Inventory Management System backend](../Charity-Inventory-Management-System-Backend). Provides role-based dashboards for donations, inventory, distributions, beneficiaries, volunteers, reporting and administration.

## Stack

- React 19 + TypeScript, built with Vite
- Tailwind CSS v4 for styling
- React Router for navigation
- TanStack Query for server state
- React Hook Form + Zod for forms and validation
- Recharts for dashboard/report charts
- Zustand for auth session state

## Getting started

```bash
npm install
npm run dev
```

The app runs at `http://localhost:5173` and expects the backend at `http://localhost:8080` (configurable via `VITE_API_BASE_URL` in `.env`).

Before starting, make sure the backend is running — see its README for setup (MySQL, `JAVA_HOME` pointed at a JDK 17 install, `mvnw spring-boot:run`). Register a user via the app's **Create an account** link, or `POST /api/v1/auth/register` directly, to get your first login.

## Roles

- **ADMIN** — full access, including user administration, distribution approvals, duplicate overrides, volunteer management and the audit log.
- **INVENTORY_STAFF** — day-to-day operations: inventory, donations, beneficiaries, distributions (create/allocate/complete), reports.
- **VOLUNTEER** — read-only inventory/category lookups, plus their own assigned tasks under **My tasks**. Since the backend has no endpoint linking a login account to a volunteer profile, a volunteer enters their Volunteer ID (given by their coordinator) once; it's stored locally and used for all subsequent task lookups.

## Project structure

```text
src/
├── api/            Typed API client functions, one module per backend resource
├── app/            Router, route guards, top-level pages (404, not-authorized)
├── components/     Shared UI kit (ui/), layout (layout/), reusable pickers (shared/), charts (charts/)
├── features/       Page-level feature modules, grouped by domain
├── hooks/          Shared React hooks
├── lib/            Utilities (formatting, toast helpers)
├── store/          Zustand auth store
└── types/          Shared enum definitions mirrored from the backend
```

## Scripts

```bash
npm run dev        # start dev server
npm run build      # type-check and produce a production build in dist/
npm run preview    # preview the production build locally
```
