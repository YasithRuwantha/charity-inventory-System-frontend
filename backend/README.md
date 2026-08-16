# 🎗️ Charity Inventory Management System — Backend

[![Java Version](https://img.shields.io/badge/Java-17-orange.svg?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot Version](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security Version](https://img.shields.io/badge/Spring%20Security-6.2-blue.svg?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![Database](https://img.shields.io/badge/MySQL-8.0+-blue.svg?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Build Tool](https://img.shields.io/badge/Maven-3.x-red.svg?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)

Backend REST API for charities, NGOs and donation centres. It tracks what was donated, what is in
store, who received aid, and who did the work — with a full audit trail behind every stock movement.

Everything is **backend only**: JSON APIs, no UI. Authentication is JWT-based and stateless.

---

## 📚 Table of contents

- [Getting started (clone & run locally)](#-getting-started-clone--run-locally)
- [What the system does](#-what-the-system-does)
- [Architecture](#-architecture)
- [Modules](#-modules)
- [Business rules that matter](#-business-rules-that-matter)
- [Prerequisites](#-prerequisites)
- [Database setup](#-database-setup)
- [Configuration & environment variables](#-configuration--environment-variables)
- [Running the application](#-running-the-application)
- [Authentication & roles](#-authentication--roles)
- [API overview](#-api-overview)
- [Response format](#-response-format)
- [Search, filtering, sorting, pagination](#-search-filtering-sorting-pagination)
- [Swagger / OpenAPI](#-swagger--openapi)
- [Postman](#-postman)
- [Health checks](#-health-checks)
- [Testing](#-testing)
- [Project structure](#-project-structure)
- [Troubleshooting](#-troubleshooting)

---

## 🚀 Getting started (clone & run locally)

New to this repo? Welcome 👋 — follow the steps below in order and you'll have the API running
locally in about 10–15 minutes. Nothing here is destructive, so don't worry about breaking anything;
if a step doesn't work, jump to [Troubleshooting](#-troubleshooting) or ask a teammate.

### ✅ Before you start, install these

| Tool | Version | How to check you have it |
| --- | --- | --- |
| ☕ JDK | 17 | `java -version` |
| 🧰 Maven | 3.6+ *(optional — see note below)* | `mvn -version` |
| 🐬 MySQL | 8.0+ | `mysql --version` |
| 🌿 Git | any recent version | `git --version` |

> 💡 **No Maven install? No problem.** The repo ships with a Maven *wrapper* (`mvnw` /
> `mvnw.cmd`), so every command below works without Maven being on your machine at all.

Pick an IDE if you don't already have one — **IntelliJ IDEA** is the easiest for a Spring Boot
project, but VS Code (with the Java Extension Pack) and Eclipse both work fine.

### 1️⃣ Get the code

```bash
git clone https://github.com/PawanBulathwaththa/Charity-Inventory-Management-System-Backend.git
cd Charity-Inventory-Management-System-Backend
```

Working from the active feature branch rather than `main`? Switch to it:

```bash
git checkout feature/charity-inventory-backend
```

### 2️⃣ Set up the database

The app expects a MySQL user/database it can use — the values below match the defaults already in
`1`, so if you run this exact SQL you won't need to change any config:

```sql
CREATE DATABASE IF NOT EXISTS charity_db;
CREATE USER IF NOT EXISTS 'charity_user'@'localhost' IDENTIFIED BY 'Charity@123';
GRANT ALL PRIVILEGES ON charity_db.* TO 'charity_user'@'localhost';
FLUSH PRIVILEGES;
```

Prefer to use your own MySQL user (e.g. `root`)? That's fine too — just set the `DB_USERNAME` and
`DB_PASSWORD` environment variables when you run the app in step 4. See
[Database setup](#-database-setup) for the full details.

You don't need to create any tables by hand — the app builds its own schema the first time it
starts up.

### 3️⃣ Open the project in your IDE *(optional, but makes life easier)*

- **IntelliJ IDEA:** `File → Open`, pick the folder you cloned, choose "Open as Maven Project".
- **VS Code:** just open the folder — the Java Extension Pack detects `pom.xml` automatically.

Give it a minute or two to download dependencies the first time. If you see red squiggles under
things like `getName()` or `@RequiredArgsConstructor`, that's Lombok — install the **Lombok
plugin** for your IDE and turn on **annotation processing**. This is an IDE-only quirk; the actual
build works without it.

### 4️⃣ Run the app

```bash
# Windows
mvnw.cmd clean spring-boot:run

# macOS / Linux
chmod +x mvnw
./mvnw clean spring-boot:run
```

The first start takes ~20–30 seconds while the database schema is being built — that's normal.
Once you see Spring Boot's log settle down, the API is live at **`http://localhost:8080`**.

### 5️⃣ Check it actually worked

Open this in a browser or run it in a terminal:

```bash
curl http://localhost:8080/api/health
```

A healthy response looks like `{"status":"UP","database":"CONNECTED",...}`.

Then take a look around:

- 📖 **Swagger UI** — `http://localhost:8080/swagger-ui.html` — browse and try every endpoint from
  the browser, no extra tools needed.
- 📮 **Postman** — import `postman/charity-inventory-management.postman_collection.json` (see
  [Postman](#-postman)) if you'd rather work from there.

### 6️⃣ (Optional) Run the test suite

```bash
./mvnw clean test
```

These tests run against an in-memory database, completely separate from your local `charity_db`,
so there's no risk to your data — a great way to confirm your setup is healthy before writing code.

---

🎉 **That's it — you're set up.** If anything didn't go to plan, the
[Troubleshooting](#-troubleshooting) section below covers the most common hiccups (wrong DB
password, port already in use, Lombok errors, etc.).

---

## 🎯 What the system does

| Area | Capability |
| --- | --- |
| **Inventory** | Categories, items, stock levels, units, expiry tracking, low/out-of-stock detection, manual adjustments, an immutable transaction ledger |
| **Donations** | Donors, multi-item donations, automatic stock increase, receipts, donation history, donor statistics, bulk CSV upload with preview |
| **Beneficiaries** | Registration, family size, priority levels, distribution history, per-beneficiary statistics |
| **Distributions** | Requests, inventory allocation, approval/rejection/cancellation, confirmed hand-over with stock deduction, duplicate-distribution prevention with ADMIN override |
| **Volunteers** | Volunteer records, task assignment, task status workflow, activity history |
| **Reporting** | Seven reports, CSV exports, dashboard summary, trends, alerts — all computed from live SQL |
| **Auditing** | Every significant action written to `audit_logs` with the acting user |

---

## 🏗 Architecture

This is a **single Spring Boot application organised as a modular monolith**. Each business area is
a self-contained package with its own controllers, services, repositories, entities, DTOs, enums and
mappers, so a module can later be lifted into its own deployable with little rework.

**Why one application and not physical microservices:** the core workflows — donation → stock
increase → ledger, and distribution completion → stock deduction → ledger — must be atomic. A
donation that half-applies, or a distribution that deducts rice and then fails on medicine, corrupts
the inventory permanently. Inside one application these run under a single `@Transactional` boundary
and roll back cleanly. Splitting them across HTTP services would mean giving that up and rebuilding
it as a saga with compensation, which adds significant failure modes for no benefit at this scale.

Because there are no separate services, there is **no API Gateway and no Eureka discovery server** —
there is nothing to route between or discover. Their responsibilities are handled in-process:

| Gateway concern | Where it lives |
| --- | --- |
| Routing | Spring MVC request mappings |
| CORS | `common/config/CorsConfig.java` (`charity.cors.allowed-origins`) |
| Correlation IDs | `common/security/CorrelationIdFilter.java` — sets `X-Correlation-Id`, echoed on responses and in every log line |
| Authentication | The existing `JwtAuthenticationFilter` in the security filter chain |

If the system is ever split, each extracted service still points at the same `charity_db` schema, and
the cross-service stock operations would need saga/compensation handling at that point.

### Data integrity

- **One database for everything: `charity_db`.** There is no `inventory_db`, `donation_db`, etc.
- **Stock is never overwritten blindly.** Every change to `inventory_items.quantity` writes an
  `inventory_transactions` row recording `quantityBefore`, the signed movement and `quantityAfter`,
  so current stock is always reconstructable from history.
- **Concurrency-safe deduction.** Stock reads for update take a pessimistic lock, so two simultaneous
  requests for the same item cannot both pass the availability check.
- **Reference codes are generated from a database-backed counter** (`reference_sequences`), not
  `count() + 1`, so concurrent inserts cannot collide.
- **History is never hard-deleted.** Items, donors, beneficiaries and volunteers referenced by
  history are archived/deactivated instead.

---

## 🧩 Modules

| Package | Responsibility |
| --- | --- |
| `auth` | **Pre-existing.** Registration, login, JWT issuing, user administration |
| `common` | Response envelope, exception handling, base entity/auditing, reference generator, security helpers, CORS, OpenAPI, health |
| `inventory` | Categories, items, stock service, transaction ledger |
| `donation` | Donors, donations, donation items, receipts, bulk CSV import |
| `beneficiary` | Beneficiaries, priority, statistics |
| `distribution` | Requests, items, allocation, approval workflow, duplicate detection, overrides |
| `volunteer` | Volunteers, tasks, activity |
| `reporting` | Reports, CSV export, dashboard analytics |
| `audit` | Audit log writing and querying |

---

## ⚖️ Business rules that matter

These are enforced server-side and covered by tests:

1. **Stock can never go negative.** Any operation that would drive it below zero is refused with
   `409 INSUFFICIENT_STOCK`.
2. **Only completion moves stock.** Creating a request, allocating, and approving all leave inventory
   untouched. Stock is deducted at `POST /api/distributions/{id}/complete` and nowhere else.
3. **All-or-nothing distributions.** If any line fails (expired, insufficient, duplicate), the whole
   completion rolls back — no partially deducted stock, and the request stays un-completed.
4. **A completed distribution cannot complete again** — `409 DISTRIBUTION_ALREADY_COMPLETED`, with no
   second deduction.
5. **Expired inventory cannot be distributed** — `EXPIRED_INVENTORY`. Expired rows are kept for audit.
6. **Duplicate distributions are detected.** If a beneficiary already received the same item within
   `charity.distribution.duplicate-days` (default 30), completion is refused with
   `409 DUPLICATE_DISTRIBUTION` and details of the previous hand-over.
7. **Only an ADMIN may override a duplicate**, and `overrideReason` is mandatory. Each override
   writes a `distribution_overrides` row and an `OVERRIDE_DUPLICATE` audit entry.
8. **Bulk import is all-or-nothing by default.** One invalid row aborts the import and writes
   nothing; pass `allowPartial=true` to import only the valid rows. Invalid rows are always reported,
   never silently dropped.
9. **Identity comes from the token**, never from a request body or query parameter.

### Inventory status

Recalculated after every stock change:

```text
expiryDate < today            -> EXPIRED
else quantity == 0            -> OUT_OF_STOCK
else quantity <= minimumStock -> LOW_STOCK
else                          -> IN_STOCK
```

### Reference codes

| Entity | Format | Example |
| --- | --- | --- |
| Inventory item | `INV-000001` | `INV-000042` |
| Donor | `DONOR-000001` | `DONOR-000007` |
| Donation | `DON-{year}-000001` | `DON-2026-000015` |
| Beneficiary | `BEN-000001` | `BEN-000003` |
| Distribution | `DIST-{year}-000001` | `DIST-2026-000009` |
| Volunteer | `VOL-000001` | `VOL-000004` |

---

## 📋 Prerequisites

| Requirement | Version |
| --- | --- |
| JDK | 17 (the project targets `java.version=17`) |
| Maven | 3.6+, or just use the bundled `mvnw` wrapper |
| MySQL | 8.0+, running on `localhost:3306` |

```bash
java -version
mysql --version
```

---

## 🗄 Database setup

The whole system uses **one schema: `charity_db`**.

The JDBC URL includes `createDatabaseIfNotExist=true`, so the schema is created on first start. You
only need a MySQL user with rights to it. The defaults expect `charity_user` / `Charity@123`:

```sql
CREATE DATABASE IF NOT EXISTS charity_db;
CREATE USER IF NOT EXISTS 'charity_user'@'localhost' IDENTIFIED BY 'Charity@123';
GRANT ALL PRIVILEGES ON charity_db.* TO 'charity_user'@'localhost';
FLUSH PRIVILEGES;
```

Prefer `root`? Skip the SQL above and set `DB_USERNAME` / `DB_PASSWORD` instead.

### Tables are created automatically

`spring.jpa.hibernate.ddl-auto=update` — Hibernate adds missing tables and columns on startup and
**never drops anything**. `create` and `create-drop` are deliberately not used, so restarting the
application never destroys data.

After a first successful start `charity_db` contains 15 tables:

```text
users                       # pre-existing authentication table
reference_sequences         # concurrency-safe code counters

inventory_categories
inventory_items
inventory_transactions

donors
donations
donation_items

beneficiaries

distribution_requests
distribution_items
distribution_overrides

volunteers
volunteer_tasks

audit_logs
```

Indexes are declared on the columns actually used for lookup and filtering: email, item code/name,
category, status, expiry date, donor code/name, donation reference/date, beneficiary
code/name/identification number, priority, distribution reference/status/date, volunteer code/name,
and audit timestamps.

---

## ⚙️ Configuration & environment variables

Every setting in `src/main/resources/application.properties` has a working default, so the app runs
with no environment configuration. Override any of these for your machine:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `charity_db` | Schema name |
| `DB_USERNAME` | `charity_user` | MySQL user |
| `DB_PASSWORD` | `Charity@123` | MySQL password |
| `JWT_SECRET` | *(dev key)* | Base64/hex 256-bit signing key — **change this in production** |
| `JWT_EXPIRATION` | `86400000` | Token lifetime in ms (24 h) |

Business settings:

| Property | Default | Meaning |
| --- | --- | --- |
| `charity.distribution.duplicate-days` | `30` | Window for the duplicate-distribution warning |
| `charity.inventory.expiring-soon-days` | `30` | Horizon for "expiring soon" and dashboard alerts |
| `charity.organization.name` | `Charity Inventory Management System` | Printed on donation receipts |
| `charity.organization.address` / `.contact` | *(empty)* | Receipt header details |
| `charity.cors.allowed-origins` | `*` | Comma-separated origins, or `*` |
| `charity.seed.default-categories` | `true` | Seeds Food, Clothing, Medicine, Hygiene, Education, Household, Other on first start. Existing rows are never modified |
| `charity.seed.admin.enabled` | `true` | Creates a bootstrap ADMIN if the email below is not already present |
| `charity.seed.admin.email` | `admin@charity.local` | Bootstrap administrator email |
| `charity.seed.admin.password` | `Admin@123` | Bootstrap administrator password (change after first login) |
| `charity.seed.admin.name` | `System Administrator` | Display name for the seeded admin |

Example:

```bash
# Windows (PowerShell)
$env:DB_USERNAME="root"; $env:DB_PASSWORD="secret"; .\mvnw.cmd spring-boot:run

# macOS / Linux
DB_USERNAME=root DB_PASSWORD=secret ./mvnw spring-boot:run
```

---

## ▶️ Running the application

```bash
# Windows
mvnw.cmd clean spring-boot:run

# macOS / Linux
chmod +x mvnw
./mvnw clean spring-boot:run
```

Or build and run the jar:

```bash
./mvnw clean package
java -jar target/charity-management-back-0.0.1-SNAPSHOT.jar
```

The API starts on **`http://localhost:8080`**. There is a single process — no service start-up order
to worry about.

Startup takes roughly 20–30 seconds on first run while Hibernate reconciles the schema.

---

## 🔐 Authentication & roles

Authentication is the project's original JWT implementation and is unchanged. Endpoints live under
`/api/v1/auth`.

### 1. Register

Self-service registration accepts `INVENTORY_STAFF` or `VOLUNTEER` only. `ADMIN` is rejected —
use the bootstrap seeder (`admin@charity.local` / `Admin@123` by default) or promote a user via
`PATCH /api/users/{id}/role`.

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "securepassword123",
  "role": "INVENTORY_STAFF"
}
```

### 2. Log in

```http
POST /api/v1/auth/login
Content-Type: application/json

{ "email": "jane@example.com", "password": "securepassword123" }
```

Both return a token:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "name": "Jane Doe",
  "email": "jane@example.com",
  "role": "INVENTORY_STAFF"
}
```

### 3. Call protected endpoints

```http
Authorization: Bearer <YOUR_JWT_TOKEN>
```

Every endpoint except `/api/v1/auth/**`, `/api/health`, `/actuator/health` and the Swagger routes
requires a valid token. The caller's identity is always taken from the token — passing `userId` or
`role` in a request body or query string has no effect.

### Roles

| Role | Scope |
| --- | --- |
| **ADMIN** | Everything: user administration, approvals, rejections, duplicate overrides, volunteer management, audit logs |
| **INVENTORY_STAFF** | Day-to-day operations: categories, inventory, stock adjustments, donors, donations, beneficiaries, distribution requests/allocation/completion, reports, dashboard |
| **VOLUNTEER** | Read-only inventory and category lookups, own task list and activity, updating task status |

Notable ADMIN-only actions: approving and rejecting distributions, overriding a duplicate
distribution, creating/updating volunteers, changing a user's role or status, and reading audit logs.

Enforcement is `@PreAuthorize` on controller methods, using the shared constants in
`common/security/Roles.java`. Failures return `403` with `errorCode: "FORBIDDEN"`.

---

## 🔌 API overview

All business endpoints are under `/api`. Full request/response schemas are in Swagger.

### Auth & users

```text
POST   /api/v1/auth/register
POST   /api/v1/auth/login
GET    /api/users/me
GET    /api/users                     # ADMIN
GET    /api/users/{id}                # ADMIN
PATCH  /api/users/{id}/role           # ADMIN
PATCH  /api/users/{id}/status         # ADMIN
```

### Categories

```text
POST   /api/categories
GET    /api/categories
GET    /api/categories/{id}
PUT    /api/categories/{id}
PATCH  /api/categories/{id}/status
```

### Inventory

```text
POST   /api/inventory
GET    /api/inventory
GET    /api/inventory/{id}
PUT    /api/inventory/{id}
DELETE /api/inventory/{id}            # archive — history is preserved
POST   /api/inventory/{id}/restore
POST   /api/inventory/{id}/adjust-stock
GET    /api/inventory/{id}/transactions

GET    /api/inventory/low-stock
GET    /api/inventory/out-of-stock
GET    /api/inventory/expired
GET    /api/inventory/expiring-soon
GET    /api/inventory-transactions
```

Filters: `?search=&categoryId=&status=&expiryStatus=&page=&size=&sort=`
Expiry filters: `EXPIRED`, `EXPIRING_7_DAYS`, `EXPIRING_30_DAYS`, `NO_EXPIRY`

### Donors & donations

```text
POST   /api/donors
GET    /api/donors
GET    /api/donors/{id}
PUT    /api/donors/{id}
PATCH  /api/donors/{id}/status
GET    /api/donors/{id}/donations
GET    /api/donors/{id}/statistics

POST   /api/donations
GET    /api/donations
GET    /api/donations/{id}
GET    /api/donations/reference/{reference}
GET    /api/donations/{id}/items
GET    /api/donations/{id}/receipt

GET    /api/donations/bulk/template
POST   /api/donations/bulk/preview     # validates only, writes nothing
POST   /api/donations/bulk/import      # ?allowPartial=true to keep valid rows
```

Creating a donation:

```json
{
  "donorId": 1,
  "donationDate": "2026-08-12",
  "notes": "Monthly food donation",
  "items": [
    { "inventoryItemId": 1, "quantity": 50, "expiryDate": "2027-01-01" },
    { "inventoryItemId": 2, "quantity": 20 }
  ]
}
```

This validates the donor and every line, creates the donation and its items, increases each item's
stock, writes a `DONATION_IN` ledger row per item, recalculates statuses, and audits the action — all
in one transaction. Any failure rolls the whole thing back.

Bulk CSV columns: `Donor,Item,Category,Quantity,Unit,Expiry Date,Donation Date`

### Beneficiaries

```text
POST   /api/beneficiaries
GET    /api/beneficiaries
GET    /api/beneficiaries/{id}
PUT    /api/beneficiaries/{id}
PATCH  /api/beneficiaries/{id}/status
GET    /api/beneficiaries/{id}/distributions
GET    /api/beneficiaries/{id}/statistics
```

Filters: `?search=&priority=&status=&page=&size=&sort=` — search covers code, name, identification
number and contact number.

### Distributions

```text
POST   /api/distributions
GET    /api/distributions
GET    /api/distributions/{id}
PUT    /api/distributions/{id}
POST   /api/distributions/{id}/allocate
POST   /api/distributions/{id}/approve     # ADMIN
POST   /api/distributions/{id}/reject      # ADMIN
POST   /api/distributions/{id}/cancel
POST   /api/distributions/{id}/complete
GET    /api/distributions/{id}/report
GET    /api/distributions/beneficiary/{beneficiaryId}
GET    /api/distributions/check-duplicate?beneficiaryId=&inventoryItemId=
```

Lifecycle: `PENDING → APPROVED → COMPLETED`, or `REJECTED` / `CANCELLED`.

Allocation and completion address lines by **`distributionItemId`** (the line's own id from the
distribution response), not by inventory item id:

```json
{ "items": [ { "distributionItemId": 12, "allocatedQuantity": 20 } ] }
```

Overriding a duplicate warning at completion (ADMIN only):

```json
{
  "items": [ { "distributionItemId": 12, "distributedQuantity": 10 } ],
  "overrideDuplicates": true,
  "overrideReason": "Emergency assistance approved."
}
```

### Volunteers

```text
POST   /api/volunteers                # ADMIN
GET    /api/volunteers
GET    /api/volunteers/{id}
PUT    /api/volunteers/{id}           # ADMIN
PATCH  /api/volunteers/{id}/status    # ADMIN
GET    /api/volunteers/{id}/tasks
GET    /api/volunteers/{id}/activity

POST   /api/volunteer-tasks
GET    /api/volunteer-tasks
GET    /api/volunteer-tasks/{id}
PUT    /api/volunteer-tasks/{id}
PATCH  /api/volunteer-tasks/{id}/status
```

Task statuses: `PENDING → IN_PROGRESS → COMPLETED`, or `CANCELLED` (which requires a reason).

### Reports & dashboard

```text
GET /api/reports/inventory
GET /api/reports/donations
GET /api/reports/distributions
GET /api/reports/beneficiaries
GET /api/reports/volunteers
GET /api/reports/expiry
GET /api/reports/stock-movements

GET /api/reports/{report}/export        # CSV download

GET /api/dashboard/summary
GET /api/dashboard/donation-trend?period=6months
GET /api/dashboard/distribution-trend?period=6months
GET /api/dashboard/inventory-by-category
GET /api/dashboard/beneficiary-priority
GET /api/dashboard/recent-activity
GET /api/dashboard/alerts
```

`period` accepts `3months`, `6months` or `12months`. Every figure is a live database query — nothing
is hardcoded. Exports send `text/csv` with a `Content-Disposition` attachment filename.

### Audit logs

```text
GET /api/audit-logs                     # ADMIN
```

Filters: `?userId=&action=&entityType=&entityId=&startDate=&endDate=&page=&size=`

Audit entries record the acting user, action, entity, description and changed values. Passwords,
password hashes and tokens are never written.

---

## 📦 Response format

Every business endpoint returns the same envelope. (The two `/api/v1/auth` endpoints keep their
original token shape.)

**Success**

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { },
  "timestamp": "2026-08-12T10:00:00"
}
```

**Paginated**

```json
{
  "success": true,
  "data": [],
  "pagination": { "page": 0, "size": 20, "totalElements": 100, "totalPages": 5 }
}
```

**Error**

```json
{
  "success": false,
  "message": "Insufficient inventory",
  "errorCode": "INSUFFICIENT_STOCK",
  "timestamp": "2026-08-12T10:00:00"
}
```

**Validation error**

```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "errors": { "quantity": "Quantity must be greater than zero" }
}
```

### Error codes

| Code | HTTP | Meaning |
| --- | --- | --- |
| `VALIDATION_ERROR` | 400 | Request body failed validation |
| `BAD_REQUEST` | 400 | Business input rejected |
| `MALFORMED_REQUEST` | 400 | Unparseable body or bad enum value |
| `UNAUTHORIZED` | 401 | Missing, invalid or expired token |
| `FORBIDDEN` | 403 | Role not permitted |
| `RESOURCE_NOT_FOUND` | 404 | Entity does not exist |
| `ENDPOINT_NOT_FOUND` | 404 | No such URL |
| `DUPLICATE_RESOURCE` | 409 | Name/code/identifier already exists |
| `INSUFFICIENT_STOCK` | 409 | Would drive stock below zero |
| `EXPIRED_INVENTORY` | 409 | Item is past its expiry date |
| `DUPLICATE_DISTRIBUTION` | 409 | Beneficiary recently received this item |
| `OVERRIDE_REASON_REQUIRED` | 409 | Override attempted without a reason |
| `DISTRIBUTION_ALREADY_COMPLETED` | 409 | Repeat completion |
| `CONCURRENT_MODIFICATION` | 409 | Lost a lock race; retry |
| `FILE_TOO_LARGE` | 413 | Upload exceeds 5 MB |
| `INTERNAL_ERROR` | 500 | Unexpected fault (details logged, never returned) |

Stack traces, SQL and credentials are never exposed to callers.

---

## 🔎 Search, filtering, sorting, pagination

Every list endpoint is paged and filtered in SQL — nothing is loaded into memory and filtered in Java.

```text
?page=0&size=20&sort=createdAt,desc
```

Defaults to `page=0&size=20`; `size` is capped at **100**. Sorting accepts any field on the response
DTO. Filters are documented per endpoint above and in Swagger.

---

## 📖 Swagger / OpenAPI

| Resource | URL |
| --- | --- |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |

All 81 endpoints are documented with descriptions, request/response schemas, validation rules and
error cases.

**Testing secured endpoints from Swagger:**

1. Call `POST /api/v1/auth/login` and copy the `token`.
2. Click **Authorize** (top right).
3. Paste just the token — the `bearerAuth` scheme adds the `Bearer ` prefix.
4. Every subsequent call from the UI is authenticated.

---

## 📮 Postman

`postman/charity-inventory-management.postman_collection.json` covers login, inventory, donors,
donations, bulk upload, beneficiaries, distributions, volunteers, reports and the dashboard.

1. Import the collection.
2. Set the collection variable `baseUrl` (default `http://localhost:8080`).
3. Run **Login** — a test script stores the JWT into the `token` variable automatically, and every
   other request inherits it.

No real credentials are stored in the collection.

---

## 🩺 Health checks

```text
GET /api/health          -> {"status":"UP","database":"CONNECTED","timestamp":"..."}
GET /actuator/health     -> {"status":"UP","groups":["liveness","readiness"]}
```

Both are public. Only `health` is exposed from Actuator, with `show-details=never`, so no
environment, bean or configuration data is reachable.

---

## 🧪 Testing

```bash
./mvnw clean test           # full suite
./mvnw test -Dtest=DistributionIntegrationTest
./mvnw clean verify         # tests + package
```

**112 tests** run against an in-memory H2 schema under the `test` profile, so the suite never touches
your MySQL data. Production always runs on MySQL.

| Suite | Covers |
| --- | --- |
| `AuthenticationIntegrationTest` | Existing login still works, bad credentials, missing/invalid/expired tokens, inactive users, role restrictions, unknown URLs |
| `InventoryIntegrationTest` | Categories, duplicate names, items, adjustments, ledger rows, status transitions, expiry queries |
| `InventoryConcurrencyTest` | Parallel deductions never produce negative stock |
| `DonationIntegrationTest` | Multi-item donations, stock increase, ledger, receipts, invalid donor/item/quantity, rollback |
| `BulkDonationIntegrationTest` | Preview validation, row-level errors, import, all-or-nothing vs partial |
| `BeneficiaryIntegrationTest` | Registration, family-size validation, priority, search, history, statistics |
| `DistributionIntegrationTest` | Full lifecycle, stock timing, insufficient/expired stock, duplicates, override permissions, double completion, multi-item rollback |
| `VolunteerIntegrationTest` | Volunteers, tasks, status transitions, activity |
| `ReportingIntegrationTest` | Report figures, filters, CSV export, dashboard totals |
| `AuditAndUserAdminIntegrationTest` | Audit entries, filters, user role/status administration |

The tests assert real behaviour — that approval leaves stock untouched, that a failed line rolls the
whole distribution back, that a non-admin cannot override a duplicate — not just HTTP status codes.

---

## 📁 Project structure

```text
Charity-Inventory-Management-System-Backend/
├── postman/
│   └── charity-inventory-management.postman_collection.json
├── src/
│   ├── main/
│   │   ├── java/com/charitymanagement/api/charitymanagementback/
│   │   │   ├── auth/            # pre-existing authentication + user administration
│   │   │   ├── config/          # SecurityConfig, JwtAuthenticationFilter
│   │   │   ├── common/          # envelope, exceptions, base entity, references, security, config
│   │   │   ├── inventory/       # categories, items, ledger
│   │   │   ├── donation/        # donors, donations, bulk import
│   │   │   ├── beneficiary/     # beneficiaries
│   │   │   ├── distribution/    # requests, allocation, overrides
│   │   │   ├── volunteer/       # volunteers, tasks
│   │   │   ├── reporting/       # reports, exports, dashboard
│   │   │   ├── audit/           # audit logging
│   │   │   └── CharityManagementBackApplication.java
│   │   └── resources/application.properties
│   └── test/
│       ├── java/...             # integration suites + shared support
│       └── resources/application-test.properties
├── pom.xml
└── README.md
```

Each business module follows the same internal layout:

```text
<module>/
├── controller/
├── dto/request/
├── dto/response/
├── entity/
├── enums/
├── mapper/
├── repository/
└── service/
```

Conventions: constructor injection via Lombok's `@RequiredArgsConstructor`, `final` dependencies,
thin controllers, business logic in services, and JPA entities never returned from controllers —
always a DTO.

---

## 🔧 Troubleshooting

**`Access denied for user 'charity_user'@'localhost'`**
The MySQL user does not exist or the password differs. Run the [database setup](#-database-setup)
SQL, or point the app at your own account with `DB_USERNAME` / `DB_PASSWORD`.

**`Communications link failure` / `Connection refused`**
MySQL is not running, or is not on port 3306. Start the service and check with
`mysqladmin -u root -p status`, or set `DB_HOST` / `DB_PORT`.

**`Port 8080 was already in use`**
Another process holds the port. Free it, or start with `--server.port=8081`:

```bash
java -jar target/charity-management-back-0.0.1-SNAPSHOT.jar --server.port=8081
```

**`401 Unauthorized` on every call**
Missing or stale token. Log in again and send `Authorization: Bearer <token>`. Tokens last 24 hours
by default. A changed `JWT_SECRET` invalidates all previously issued tokens.

**`403 Forbidden` on an endpoint you expect to reach**
Your role lacks the permission — see [roles](#roles). Approvals, rejections, duplicate overrides,
volunteer management and audit logs are ADMIN-only.

**`409 INSUFFICIENT_STOCK` when stock looks sufficient**
Availability is checked against live quantity at that moment. Another distribution may have consumed
it, or the item may be archived. Check `GET /api/inventory/{id}` and its transaction history.

**`409 DUPLICATE_DISTRIBUTION`**
Working as intended — the beneficiary received that item within the last
`charity.distribution.duplicate-days` days. Inspect with
`GET /api/distributions/check-duplicate?beneficiaryId=&inventoryItemId=`, then either wait, choose a
different item, or have an ADMIN complete it with `overrideDuplicates` and a reason.

**A table or column is missing after adding code**
`ddl-auto=update` only applies changes at startup. Restart the application. Note that `update` never
drops or renames existing columns — remove those by hand if you need to.

**Tests fail but the app runs fine (or vice versa)**
Tests use H2 under the `test` profile, the app uses MySQL. A failure in only one usually means a
dialect-specific query or a schema drift in `charity_db` left over from an earlier run.

**Lombok errors in the IDE ("cannot find symbol getX()")**
Install the Lombok plugin and enable annotation processing. The Maven build already handles it.

---

## 📄 Notes

- `spring.jpa.open-in-view=false` — lazy associations are resolved inside services, so no queries
  fire during JSON serialisation.
- Business dates use `LocalDate`, timestamps use `LocalDateTime`, and everything is ISO-8601 in JSON.
- Logs carry a correlation id per request (`X-Correlation-Id`); credentials and tokens are never
  logged.
- CSRF is disabled deliberately: this is a stateless token-authenticated API with no session cookie,
  so there is no cookie for an attacker to ride on.
