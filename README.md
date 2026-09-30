# Sethos Store

Point of sale for Sethos Store: an Angular single-page terminal talking to a Spring Boot
REST API over HTTP. The two halves are independent projects and can be built, run and
deployed on their own.

| Folder | Project | Stack | Port |
|---|---|---|---|
| `pos-frontend/` | `sethos-store-frontend` | Angular 22.2 · TypeScript 6 · standalone components, signals, lazy routes | 4200 |
| `pos-backend/` | `sethos-store-backend` | Spring Boot 4.1.1 · Java 17 · Spring Data JPA · Hibernate 7 | 8080 |
| `docker-compose.yml` | `sethos-store` | PostgreSQL 18.6 (optional; H2 is the default) | 5432 |

There is no authentication yet. Every endpoint is open, which is fine for local
development and deliberately not fine for anything shared. See
[Before this goes live](#before-this-goes-live).

---

## Prerequisites

| Tool | Required | Notes |
|---|---|---|
| Java | 17 or newer | Spring Boot 4 needs 17 as a minimum |
| Node.js | **22.22.3+ or 24.15.0+** | Angular 22's CLI refuses to run below this |
| Maven | not needed | use the bundled `mvnw` wrapper |
| Docker | only for PostgreSQL | the default H2 setup needs nothing |

> Anything older fails immediately with "The Angular CLI requires a minimum Node.js
> version". The `.tooling/` folder holds a portable Node used during initial setup; it is
> gitignored and safe to delete once your system Node meets the requirement.

---

## Running it

### 1. Backend

```bash
cd pos-backend
./mvnw spring-boot:run          # macOS / Linux
.\mvnw.cmd spring-boot:run      # Windows
```

Starts on <http://localhost:8080> using H2, creates the schema, and seeds a demo catalogue
(5 categories, 18 products, 3 customers) plus two weeks of sales so the dashboard has
something to show. Turn the seed off with `pos.seed-demo-data: false`.

- Health: <http://localhost:8080/actuator/health>
- H2 console: <http://localhost:8080/h2-console> — JDBC URL `jdbc:h2:file:./data/posdb`,
  user `sa`, empty password

### 2. Frontend

```bash
cd pos-frontend
npm install     # first time only
npm start
```

Open <http://localhost:4200>. It redirects to the terminal.

Run the backend first: the app fetches store settings (currency, tax, branding) at startup.
If the API is down the sidebar says so and the UI falls back to sensible defaults rather
than breaking.

---

## Which database to use

**Use PostgreSQL for anything real.** It is the right default for a POS:

- **Correct money.** `NUMERIC(12,2)` is exact decimal arithmetic. MySQL's older defaults and
  anything float-based will eventually be a cent out on a receipt, and that is the one bug a
  POS cannot have.
- **Real transactions under concurrency.** Checkout locks the product rows it is selling
  (`SELECT … FOR UPDATE`) so two terminals cannot both sell the last unit. PostgreSQL's MVCC
  handles this without readers blocking writers.
- **Reporting that keeps up.** Window functions, CTEs, partial and expression indexes, and
  `GENERATE_SERIES` for date spines. Sales reporting outgrows a simple engine quickly.
- **Good fit for the data.** `JSONB` if you later need per-item modifiers or promo rules,
  and time zone aware timestamps, which matter once "today's takings" has to mean the
  store's today.
- **Operationally boring.** Free, mature, excellent Docker images, logical replication and
  point-in-time recovery when the business starts caring about backups.

SQLite is tempting for a single till but falls over with a second terminal writing.
MongoDB is the wrong shape: sales are deeply relational and need multi-row atomicity.
MySQL/MariaDB would work; PostgreSQL is simply the stronger engine for the reporting and
concurrency this system does.

### H2 is the default on purpose

The default profile uses **H2**, the embedded database bundled with Spring Boot. Zero setup,
no Docker, and the URL is file-based (`./data/posdb`) so your data survives a restart. It
exists so you can clone and run. Both profiles are verified against the same test suite, and
the JPA mappings are portable between them.

### Switching to PostgreSQL

```bash
docker compose up -d                    # PostgreSQL 18.6 on :5432
docker compose --profile tools up -d    # …plus pgAdmin on :5050

cd pos-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Credentials live in `docker-compose.yml` and `application-postgres.yml` and match by
default (`posdb` / `posuser` / `pospassword`). Override in real environments with
`POS_DB_URL`, `POS_DB_USERNAME`, `POS_DB_PASSWORD`.

To stop, and to discard the data:

```bash
docker compose down        # keeps the volume
docker compose down -v     # deletes the volume too
```

---

## What it does

**Terminal** — the till. Product grid with category filter and search, barcode scanning
(scan, then Enter), quantity stepper, per-line discounts, manager price override,
order-level discount, five payment methods, quick-tender buttons, live change calculation,
and a printable receipt on completion.

**Dashboard** — today, last 7 days and month to date; a 14-day sales chart; best sellers;
takings split by payment method; and a low-stock list. One request fills the page, so every
figure is from the same instant.

**Products** — full CRUD with search, category/status/low-stock filters, sortable columns
and pagination. Stock is never edited through the product form: it moves only via an
adjustment, which writes a journal row, so an unexpected count can always be traced.
Products that appear in past sales are deactivated rather than deleted so receipts stay
intact.

**Categories** — CRUD with a colour and display order that drive the terminal's filter chips
and tile accents.

**Customers** — optional buyer records. Deleting one keeps their sales, which become
walk-in sales.

**Sales** — history with search and date filters, receipt view, reprint, and void. Voiding
keeps the row, records the reason and timestamp, and returns the stock.

---

## How the money works

Prices are stored **tax-exclusive**. Per line:

```
gross     = unitPrice × quantity
net       = gross − lineDiscount        (a discount can never exceed its line)
tax       = net × taxRate%
lineTotal = net + tax
```

Then for the sale:

```
subtotal      = Σ gross
taxTotal      = Σ tax
total         = Σ lineTotal − orderDiscount     (never negative)
discountTotal = Σ lineDiscount + orderDiscount
```

The order-level discount comes off the **taxed** total, which keeps each line's recorded tax
equal to the tax actually charged on it.

Decisions worth knowing about:

- **Everything rounds through one place.** `Money` on the server and `core/util/money.ts` in
  the browser both use scale 2, `HALF_UP`, in the same order of operations. The cart previews
  totals locally so they update instantly, but **the server recomputes on checkout and its
  numbers are what get stored.**
- **Receipts are immutable.** Product name, SKU, unit price, cost and tax rate are
  snapshotted onto the sale line. Repricing or renaming a product never rewrites history.
- **Duplicate scans merge.** Two lines for the same product are folded into one before the
  stock check, otherwise each copy would be checked in isolation and the basket could
  oversell.
- **Non-cash settles exactly.** Only `CASH` produces change; card, e-wallet and transfer
  record the exact amount.
- **Receipt numbers are gap-free.** `S-20260927-0001`, issued from a per-day counter row
  held under a pessimistic lock, in its own short transaction.
- **Reports use a half-open window.** `[startOfFrom, startOfTo + 1 day)` in the store's
  configured zone, so a sale at 23:59:59.500 is not silently dropped.

---

## Configuration

Store settings live under `pos.*` in `pos-backend/src/main/resources/application.yml` and
are served to the client at `GET /api/settings`, so currency and branding are a config edit
rather than a code change.

```yaml
pos:
  store-name: Sethos Store   # sidebar, browser tab and receipt header
  currency-code: USD          # any ISO 4217 code
  currency-symbol: $
  time-zone: UTC              # set to the store's zone, e.g. Asia/Manila
  default-tax-rate: 0.00
  seed-demo-data: true
  allowed-origins:
    - http://localhost:4200
```

`time-zone` is worth setting properly: it defines "today" for reports and for receipt
numbering.

The frontend's API URL is in `pos-frontend/src/environments/`. Development points at
`http://localhost:8080/api`; the production build swaps in a relative `/api`, so serving both
behind one reverse proxy needs no CORS at all.

---

## API

Base URL `http://localhost:8080/api`.

| Method | Path | Purpose |
|---|---|---|
| GET | `/settings` | Store branding, currency, tax defaults |
| GET | `/categories` · `/{id}` | List, read |
| POST · PUT · DELETE | `/categories` · `/{id}` | Create, update, delete |
| GET | `/products` | Paged search: `search`, `categoryId`, `active`, `lowStock`, `page`, `size`, `sort` |
| GET | `/products/{id}` · `/by-barcode/{code}` · `/by-sku/{sku}` · `/low-stock` | Lookups |
| POST · PUT · DELETE | `/products` · `/{id}` | Create, update, remove or deactivate |
| POST | `/products/{id}/stock-adjustments` | Signed stock correction |
| GET | `/products/{id}/stock-movements` | Stock journal |
| GET · POST · PUT · DELETE | `/customers` · `/{id}` | Customer CRUD |
| GET | `/sales` | Paged history: `search`, `status`, `from`, `to`, `page`, `size` |
| GET | `/sales/{id}` · `/by-reference/{ref}` | Full receipt |
| POST | `/sales` | **Checkout** |
| POST | `/sales/{id}/void` | Void and return stock |
| GET | `/reports/dashboard` · `/summary` · `/daily-sales` · `/top-products` · `/payment-methods` | Reporting |

Dates are inclusive `yyyy-MM-dd` in the store's time zone.

### Errors

One shape for every failure, so the client has one thing to parse:

```json
{
  "timestamp": "2026-09-27T15:16:22.163Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Not enough stock to complete this sale",
  "path": "/api/sales",
  "details": ["Baby Spinach 200g (PRD-004): asked for 999, only 0 on hand"]
}
```

`400` validation (with a `fieldErrors` map), `404` not found, `409` duplicate SKU/barcode/name
or a concurrent edit, `422` business rule (insufficient stock, short tender, voiding an
already-voided sale).

### Example checkout

```bash
curl -X POST http://localhost:8080/api/sales \
  -H "Content-Type: application/json" \
  -d '{
        "items": [
          { "productId": 1, "quantity": 2 },
          { "productId": 6, "quantity": 3, "discountAmount": 1.00 }
        ],
        "paymentMethod": "CASH",
        "amountTendered": 50.00,
        "orderDiscount": 2.00,
        "cashierName": "Jamie"
      }'
```

---

## Project layout

```
pos-backend/src/main/java/com/possystem/pos/
  domain/        JPA entities and enums; BaseEntity carries id, auditing, @Version
  repository/    Spring Data interfaces
    spec/        Criteria specifications for the dynamic filters
    projection/  Record projections for report aggregates
  service/       Business rules. SaleService owns checkout; StockService is the only
                 place stock changes; ReferenceGenerator issues receipt numbers
  web/           REST controllers plus GlobalExceptionHandler
  dto/           Request and response records, including report/
  config/        PosProperties, Clock, CORS
  bootstrap/     DemoDataSeeder
  support/       Money

pos-frontend/src/app/
  core/
    api/         API_BASE_URL token, query-param builder, error normaliser
    models/      TypeScript mirrors of the backend DTOs
    services/    One service per resource, plus CartStore and SettingsStore signal stores
    util/        money.ts, matching the server's rounding
  features/      terminal, dashboard, products, categories, customers, sales (lazy loaded)
  shared/        MoneyPipe, Receipt, ToastHost
```

Two design notes that explain most of the structure:

- **Dynamic filters use Criteria specifications, not JPQL with optional parameters.** The
  `(:param is null or …)` idiom leaves the bind parameter untyped when the value is null. H2
  tolerates it; PostgreSQL resolves `lower(?)` to `lower(bytea)` and throws. Omitting an
  absent filter from the predicate tree sidesteps the problem.
- **Pagination is wrapped in `PageResponse`.** Spring's `Page` JSON shape is not a stable
  contract and changes between versions, which would break the Angular client on an
  unrelated upgrade.

---

## Building

```bash
cd pos-backend  && ./mvnw clean package     # runs tests, produces target/sethos-store-backend-0.0.1-SNAPSHOT.jar
cd pos-frontend && npm run build            # production bundle in dist/sethos-store-frontend/
```

The frontend builds to ~310 kB initial (~85 kB transferred) with each feature in its own
lazy chunk.

---

## Before this goes live

Ordered by how much it will hurt to skip.

1. **Authentication and authorisation.** Nothing is protected today. Add Spring Security
   with a cashier/manager split — price overrides, voids and stock adjustments should be
   manager-only — and replace the free-text `cashierName` on a sale with the signed-in user.
2. **Own the schema with migrations.** `ddl-auto: update` is a development convenience and
   will not carry you through a production change safely. Move to Flyway or Liquibase and set
   `ddl-auto: validate`.
3. **Tighten CORS and serve over HTTPS.** Replace the localhost origins in
   `pos.allowed-origins`, or serve both halves behind one proxy and use the relative `/api`.
4. **Back the database up.** Point-in-time recovery on PostgreSQL, and a restore you have
   actually tested.
5. **Tests worth the name.** There is one context-load test. The checkout money maths,
   concurrent-sale stock locking, and the void/restore path are what deserve coverage first.
6. **Turn the demo seed off.** `pos.seed-demo-data: false`.

Likely next features: refunds and partial returns (the `REFUNDED` status and the stock
journal already anticipate them), shift and cash-drawer reconciliation, receipt printer and
cash drawer integration, and multi-store support.
