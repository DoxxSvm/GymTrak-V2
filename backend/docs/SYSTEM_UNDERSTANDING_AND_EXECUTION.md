# GymTrak Backend — System Understanding & Execution

**For Cursor:** the rule the agent loads automatically is **`.cursor/rules/gymtrak-backend-context.mdc`** (`alwaysApply: true`). Prefer updating that file when architecture changes; this doc is an optional longer copy.

This document explains **what the backend is**, **how it is structured**, and **how to run and operate it**. It is a Phase 1 reference before changing behavior or adding features.

---

## 1. Understanding — System summary

**GymTrak Backend** is a **modular NestJS monolith**: one deployable Node process, one **PostgreSQL** database, many feature modules. Tenancy is **per gym** (`Gym`): most data is scoped by `gymId` (header `X-Gym-Id`, query, or body — see interceptors and `getGymIdFromRequest`).

### Tech stack

| Layer | Choice |
|--------|--------|
| Framework | NestJS 10, TypeScript |
| HTTP API | REST, global prefix `api/v1` |
| API docs | Swagger UI from `docs/gymtrak-api.openapi.json` (path `/docs` with global prefix behavior) |
| Database | PostgreSQL |
| ORM | Prisma 5 (`PrismaService` = `PrismaClient`) |
| Auth | JWT (Passport), refresh tokens (hashed in DB), bcrypt for staff passwords |
| Validation | `class-validator` + `class-transformer`, global `ValidationPipe` |
| Realtime | Socket.IO (notifications gateway) |
| Jobs | BullMQ + Redis (WhatsApp/messaging queue; can be disabled) |
| Events | `@nestjs/event-emitter` (e.g. owner notifications) |
| Rate limits | `@nestjs/throttler` on selected routes |

### Architecture type

- **Monolith**, not microservices.
- **Modular**: `src/modules/*` per domain; `src/common/*` for guards, decorators, shared services; `src/platform/*` for platform-operator APIs.

### Repository layout (high level)

| Path | Purpose |
|------|---------|
| `src/main.ts`, `src/app.module.ts` | Bootstrap and root module |
| `src/common/` | Guards, decorators, interceptors, DTOs, `GymAccessService`, permission codes |
| `src/config/` | Env validation (`env.validation.ts`), startup tasks |
| `src/modules/*` | Auth, members, subscriptions, plans, attendance, trainers, etc. |
| `src/platform/` | Super-admin platform APIs |
| `prisma/` | `schema.prisma` and migrations |
| `docs/` | OpenAPI, Postman collections |
| `.cursor/rules/` | Cursor project rules (e.g. `gymtrak-backend-context.mdc`) |
| `scripts/` | Bootstrap / dev user helpers |
| `test/` | E2E / Jest config |

---

## 2. Understanding — Modules breakdown

### Auth

- **JWT** access token + **refresh** token (stored hashed).
- **OTP** challenges for phone flows (`OtpChallenge`).
- **Staff login**: username/password; must have active **TRAINER** or **STAFF** `GymUser` (or `SUPER_ADMIN`).
- **Global** `JwtAuthGuard`; **`@Public()`** skips auth.
- **Temporary signup JWT** (`isTemp`) is only valid for specific owner signup POSTs (see `JwtAuthGuard`).
- **Development / opt-out**: without Bearer, a user may be inferred (`AUTH_DEV_USER_ID` or first ACTIVE user when `NODE_ENV=development`, or `DISABLE_JWT_AUTH=true`). **Never enable relaxed auth in production by mistake.**

### Roles (Owner / “Admin” / Trainer / Member)

- **Global:** `USER` | `SUPER_ADMIN` (platform).
- **Per gym (`GymRole`):** `OWNER` | `TRAINER` | `STAFF` | `MEMBER`.

There is **no** separate `ADMIN` enum. **Staff-style admin** is **`STAFF` or `TRAINER`** with **`GymUserPermission`** rows for codes such as `dashboard:access`, `payments:access`, `members:manage`, `admin:access`. **Owners** and **`SUPER_ADMIN`** bypass RBAC in `PermissionEngineService`; **gym feature flags** still gate dashboard/payments for staff.

### Plans and subscriptions (two meanings)

1. **Member plans:** `GymPlan` (per-gym catalog) + `MemberSubscription` (member’s active/past period, `paidCents` / `priceCents`, freeze, etc.). Legacy link to global `SubscriptionPlan` may exist.
2. **Gym → GymTrak SaaS:** `GymSubscription` + `SubscriptionPlan` with `saasTier` (`BASIC` | `PLUS` | `PREMIUM`). Tier drives entitlements (`SaasEntitlementsService`). Platform APIs under `src/platform/` manage gym SaaS subscription and status.

### Payments

- **`Payment`** records amounts, status, method (`CASH` | `UPI` | `CARD`), optional links to member and `MemberSubscription`.
- Core write path: **`SubscriptionsService.receiveMemberPayment`** (transaction, updates `paidCents` when applicable). HTTP often goes through members/subscriptions controllers with **`members:manage`** (and payment feature flags for staff).
- **No** built-in Stripe-style PSP in the reviewed model; operations are **recorded** payments.

### Attendance

- **`AttendanceRecord`**: one row per gym + member user + calendar day; sources `QR_TOKEN`, `BIOMETRIC`, `MANUAL`.
- **QR:** HMAC-signed payloads with per-gym `qrSigningSecret` (`AttendanceQrService`).
- **Biometric-style:** `MemberBiometricCredential`.
- **Trainers:** separate `TrainerAttendanceRecord`.

### Notifications

- **`Notification`** rows per gym; **`NotificationsService.createForGymOwner`** targets the gym owner and emits over **WebSocket**.
- **Domain events** (`@OnEvent`) fan in to create notifications (member added, payment, plan assigned, expiry alerts).

---

## 3. Understanding — Data flow

1. Request → `api/v1/...`
2. **`GymContextInterceptor`**: merges `X-Gym-Id` into `query.gymId`; errors if query/body `gymId` disagrees with header.
3. **`ValidationPipe`**: DTO validation.
4. **`JwtAuthGuard`**: JWT or allowed public/signup/dev path → `request.user` as `JwtUser`.
5. **Route guards**: e.g. **`PermissionsGuard`** + `@RequirePermissions` → `PermissionEngineService`; or **`GymAccessService`** checks on controllers/services.
6. **Controller** → **Service** → **Prisma** → **PostgreSQL**.
7. Side paths: **EventEmitter** (notifications), **BullMQ** (WhatsApp jobs).

**Heavy business logic** often lives in **`SubscriptionsService`**, **`PlansService`**, **`PermissionEngineService`**, **`GymAccessService`**.

---

## 4. Understanding — Database overview

- **Tenant root:** `Gym` (`ownerId`, `status`, `timezone`, `qrSigningSecret`, …).
- **Membership:** `GymUser` (`userId`, `gymId`, `role`, permissions, member fields).
- **Commercial:** `GymPlan`, `MemberSubscription`, `Payment`, `Invoice`, `SubscriptionPlan`, `GymSubscription` (SaaS).
- **Attendance / CRM / ops:** `AttendanceRecord`, `Enquiry`, `Expense`, `AuditLog`, trainer-related tables.
- **Notifications:** `Notification`, `GymMessageTemplate`.

See `prisma/schema.prisma` for full models, enums, and indexes.

---

## 5. Understanding — Issues and risks

- **Relaxed JWT in dev** (`DISABLE_JWT_AUTH`, or development default): catastrophic if mis-set in production.
- **Two super-admin HTTP surfaces:** `/admin/*` and `/platform/*` — keep behavior and auth expectations aligned when changing platform features.
- **Subscriptions service** is large; changes need focused tests and clear transaction boundaries.
- **Scaling:** multiple app instances need Redis/BullMQ and WebSocket strategy (adapter, stickiness) considered.
- **Payments** are operational recordings; external gateways would need new integration and idempotency.

---

## 6. Execution — Prerequisites

- **Node.js** (compatible with Nest 10 / TypeScript 5 as in `package.json`).
- **PostgreSQL** and a valid `DATABASE_URL`.
- **Redis** (optional if `DISABLE_BULLMQ=true` for local dev without the WhatsApp queue).

---

## 7. Execution — Environment variables

### Required (startup validation)

| Variable | Purpose |
|----------|---------|
| `DATABASE_URL` | PostgreSQL connection string |
| `JWT_ACCESS_SECRET` | Sign access tokens |
| `JWT_REFRESH_SECRET` | Sign or derive refresh handling (see auth module) |

### Common optional variables

| Variable | Purpose |
|----------|---------|
| `PORT` | HTTP port (default 3000 in `main.ts` if unset/invalid) |
| `NODE_ENV` | `development` affects JWT relaxation unless overridden |
| `DISABLE_JWT_AUTH` | Explicit opt-out of Bearer requirement (dangerous in prod) |
| `AUTH_DEV_USER_ID` | User id used when JWT disabled and no Bearer |
| `REDIS_URL` | Redis for BullMQ |
| `DISABLE_BULLMQ` | Skip BullMQ/WhatsApp queue registration |
| `SKIP_MIGRATIONS_ON_STARTUP` | Skip `prisma migrate deploy` on boot if CI applies migrations |
| `APP_PUBLIC_URL` | Swagger server URL / public base |
| `OTP_*` | OTP TTL, cooldown, dev static OTP (dev only) |
| `WHATSAPP_*` / `TWILIO_*` | Messaging providers |

Full list and transforms: `src/config/env.validation.ts`.

---

## 8. Execution — NPM scripts

From the repository root (`backend/`):

| Script | Command | Purpose |
|--------|---------|---------|
| Install deps | `npm install` | Dependencies |
| Dev server | `npm run start:dev` | Nest watch mode |
| Production build | `npm run build` then `npm run start:prod` | Compile + run `dist` |
| Prisma client | `npm run prisma:generate` | Generate client after schema changes |
| Migrations (dev) | `npm run prisma:migrate` | `prisma migrate dev` |
| Prisma Studio | `npm run prisma:studio` | DB GUI |
| Tests | `npm test` | Unit tests |
| E2E | `npm run test:e2e` | E2E |
| Lint / format | `npm run lint`, `npm run format` | ESLint / Prettier |
| Seed helpers | `npm run db:default-user`, `npm run db:bootstrap-super-admin` | Scripts in `scripts/` |

---

## 9. Execution — API and docs

- **Base path:** `/api/v1`
- **Swagger UI:** `/docs` (OpenAPI from `docs/gymtrak-api.openapi.json` when file exists)
- **Gym scoping:** Prefer **`X-Gym-Id`** header; must stay consistent with `gymId` in query/body when both are sent.

---

## 10. Execution — Typical local flow

1. Create a PostgreSQL database and set `DATABASE_URL`.
2. Set `JWT_ACCESS_SECRET` and `JWT_REFRESH_SECRET`.
3. `npm install`
4. `npm run prisma:generate`
5. `npm run prisma:migrate` (or apply migrations your way)
6. Optional: `npm run db:default-user` or `db:bootstrap-super-admin` per `scripts/` README if present
7. `npm run start:dev`
8. Open `http://localhost:<PORT>/docs` for API exploration

If Redis is not running locally, set **`DISABLE_BULLMQ=true`** so the app starts without the queue.

---

## 11. Phase 2 — Feature work (after approval)

When implementing a feature:

1. Map requirements to **Prisma models** and **modules** above.
2. List **API** additions/changes and **DTOs** (`class-validator`).
3. Keep **controllers thin**; put logic in **services**; reuse **`GymAccessService`** / **`PermissionEngineService`**.
4. Use **`gymId`** scoping consistently; add tests where behavior is non-trivial.

Do not merge behavioral changes without aligning this document if the architecture materially changes.
