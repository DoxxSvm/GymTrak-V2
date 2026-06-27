# GymTrak API structure

**Base URL:** `/api/v1` (set in `src/main.ts` via `setGlobalPrefix('api/v1')`).

**Swagger UI:** `http://localhost:<PORT>/api/v1/docs` (serves `docs/gymtrak-api.openapi.json`). **OpenAPI JSON:** `http://localhost:<PORT>/api/v1/docs/json`. The spec **server URL** is dynamic: relative `/api/v1` in the file; at runtime, if **`APP_PUBLIC_URL`** is set (e.g. `https://api.example.com`), Swagger uses that origin + `/api/v1`. If unset, each `/docs/json` response uses **`X-Forwarded-Proto` / `X-Forwarded-Host`** (or the direct request host) so Try-it-out matches the URL you opened.

**Postman (latest app):** download from [https://www.postman.com/downloads/](https://www.postman.com/downloads/) (Windows / macOS / Linux). **Collection:** import the repo file `docs/gymtrak-api.postman_collection.json`, or in Postman use **Import → Link** with **`GET /api/v1/reference/postman-collection.json`** on your API host. Set collection variable **`baseUrl`** to `https://<your-host>/api/v1` (or `http://localhost:3000/api/v1` locally).

**Auth:** Most routes use the global `JwtAuthGuard`. Send `Authorization: Bearer <access_token>` unless the route is `@Public()` or treated as owner signup (see below).

**Owner signup (no Bearer required for user context):** The guard attaches `AUTH_DEV_USER_ID` or the first `ACTIVE` user for:

- `POST /user/select-role`
- `POST /gym`
- `POST /gyms`

See `src/common/guards/jwt-auth.guard.ts` (`isSignupFlowPost`).

**Phone signup flow:** `POST /auth/login` with `phone` (+ optional `country_code`) sends OTP to any number and returns `{ success, isRegistered, phone }`. `POST /auth/verify-otp`: registered users get full tokens; new numbers get a short-lived `tempToken` (`JWT_TEMP_SIGNUP_EXPIRES_IN`, default 12m). Bearer `tempToken` is accepted only on the three signup routes above; complete signup with `POST /user/select-role` (gym_owner) then `POST /gym` or `POST /gyms` to receive `access_token` + `refresh_token`.

**Legacy `POST /auth/login`:** Still accepts `username` + `password` (trainer/owner app password login).

**Public routes:** Marked with `@Public()` in code (e.g. parts of `auth`, `app`, `attendance-qr`, `attendance`). Those skip JWT entirely.

---

## App & health

| Method | Path | Source |
|--------|------|--------|
| GET | `/health` | `src/app.controller.ts` |
| GET | `/reference/postman-collection.json` | `src/app.controller.ts` |
| GET | `/app/config`, `/config` | `src/app.controller.ts` |

---

## Auth (`/auth`)

| Method | Path | Source |
|--------|------|--------|
| POST | `/auth/otp/send` | `src/modules/auth/auth.controller.ts` |
| POST | `/auth/otp/verify` | same |
| POST | `/auth/staff/login` | same |
| POST | `/auth/refresh` | same |
| POST | `/auth/send-otp` | same |
| POST | `/auth/resend-otp` | same |
| POST | `/auth/verify-otp` | same — body: `phone`, `otp`, optional `country_code`. Response includes **`registration_state`**: `registered` (tokens + **`app_role`**: `gym_owner` \| `trainer` \| `super_admin`) or `new_signup_required` (`tempToken`, `isRegistered: false`). |
| POST | `/auth/login` | same |
| POST | `/auth/logout` | same |
| GET | `/auth/me` | same |

---

## Owner app (root — no `@Controller` prefix)

| Method | Path | Source |
|--------|------|--------|
| POST | `/user/select-role` | `src/modules/owner-app/owner-app.controller.ts` |
| POST | `/gym` | same → `owner-app.service.ts` (`setGym`) |

---

## Onboarding (`/onboarding`)

| Method | Path | Source |
|--------|------|--------|
| POST | `/onboarding/choose-role` | `src/modules/onboarding/onboarding.controller.ts` |
| POST | `/onboarding/owner` | same |
| POST | `/onboarding/member` | same |

---

## Gyms (`/gyms`)

| Method | Path | Source |
|--------|------|--------|
| GET | `/gyms` | `src/modules/gyms/gyms.controller.ts` |
| POST | `/gyms` | same |
| GET | `/gyms/:gymId/config` | `src/modules/gym-config/gym-config.controller.ts` |
| PATCH | `/gyms/:gymId/config` | same |
| POST | `/gyms/:gymId/config` | same |

---

## Gym profile & features

| Method | Path | Source |
|--------|------|--------|
| GET | `/gym-profile` | `src/modules/gym-profile/gym-profile.controller.ts` |
| PATCH | `/gym-profile` | same |
| PUT | `/gym-profile` | same |
| PUT | `/gym-profile/compat` | same |
| POST | `/gym-profile/location` | same |
| POST | `/gym-profile/location/confirm` | same |
| GET | `/gym-features` | `src/modules/gym-features/gym-features.controller.ts` |
| PATCH | `/gym-features` | same |

---

## Dashboard & analytics

| Method | Path | Source |
|--------|------|--------|
| GET | `/dashboard/owner` | `src/modules/dashboard/dashboard.controller.ts` |
| GET | `/dashboard` | same — `?mobileView=owner&gymId=` returns owner home with **`owner_name`**, **`owner_image`** (User `avatarUrl`), **`total_enquiry`**, **`converted`**, **`pending`** (enquiries not CONVERTED/CLOSED/LOST). Full dashboard metrics also include the same top-level keys plus **`enquiries.pending`**. |
| GET | `/analytics/overview` | `src/modules/analytics/analytics.controller.ts` |
| GET | `/analytics/payments/growth` | same |
| GET | `/analytics/revenue/monthly` | same |
| GET | `/analytics/members/growth` | same |
| GET | `/analytics/attendance/trends` | same |

---

## Members (`/members`)

| Method | Path | Source |
|--------|------|--------|
| GET | `/members` | `src/modules/members/members.controller.ts` |
| POST | `/members` | same |

**`GET /members` query:** `gymId` (or header `X-Gym-Id`), `page`, `limit`, `offset`, **`search`** or **`q`** (name / phone / email), **`active=true`** or **`expired=true`** (mutually exclusive; same buckets as `status=active` / `status=expired`), or `status=all|active|expired|inactive`, or `filter` enum (`all`, `expiring`, `leads`, …). Example: `/members?gymId=…&page=1&limit=20&active=true&search=ali`.
| GET | `/members/:memberId` | same |
| GET | `/members/:memberId/profile` | same |
| PATCH | `/members/:memberId` | same |
| PUT | `/members/:memberId` | same |
| DELETE | `/members/:memberId` | same |
| GET | `/members/:memberId/subscriptions` | same |
| POST | `/members/:memberId/subscriptions` | same |
| GET | `/members/:memberId/attendance` | same |
| GET | `/members/:memberId/payments` | same |
| GET | `/members/:memberId/payment-summary` | same |
| POST | `/members/:memberId/payments` | same |
| GET | `/members/:memberId/diet` | same |
| POST | `/members/diet` | same |

**Compat (root):** `POST /diet` → `src/modules/members/member-detail-compat.controller.ts`

---

## Trainers (`/trainers`)

| Method | Path | Source |
|--------|------|--------|
| POST | `/uploads/images` | `src/modules/uploads/uploads.controller.ts` |
| GET | `/trainers/me/permissions` | `src/modules/trainers/trainers.controller.ts` |
| GET | `/trainers` | same |
| POST | `/trainers` | same |
| POST | `/trainers/compat` | same |
| GET | `/trainers/:trainerId/plans` | same |
| GET | `/trainers/:trainerId/clients` | same |
| GET | `/trainers/:trainerId/revenue` | same |
| GET | `/trainers/:trainerId/salary` | same |
| POST | `/trainers/:trainerId/salary/pay` | same |
| GET | `/trainers/:trainerId/attendance` | same |
| GET | `/trainers/:trainerId/salary-payments` | same |
| POST | `/trainers/:trainerId/attendance/punch` | same |
| POST | `/trainers/:trainerId/attendance/check-in` | same (legacy alias of `/trainers/:trainerId/attendance/punch`) |
| POST | `/trainers/:trainerId/salary-payments` | same |
| POST | `/trainers/:trainerId/credentials` | same |
| PATCH | `/trainers/:trainerId` | same |
| GET | `/trainers/:trainerId` | same |
| GET | `/trainers/:trainerId/members` | same |
| PUT | `/trainers/:trainerId` | same |
| DELETE | `/trainers/:trainerId` | same |
| PUT | `/trainers/:trainerId/password` | same |
| PUT | `/trainers/:trainerId/permissions` | same |

---

## Trainer leaves (`/trainer-leaves`)

| Method | Path | Source |
|--------|------|--------|
| POST | `/trainer-leaves` | `src/modules/trainer-leaves/trainer-leaves.controller.ts` |
| GET | `/trainer-leaves/me` | same |
| GET | `/trainer-leaves/pending` | same |
| PATCH | `/trainer-leaves/:leaveId/decision` | same |
| PATCH | `/trainer-leaves/:leaveId/cancel` | same |

---

## Plans (`/plans`)

| Method | Path | Source |
|--------|------|--------|
| GET | `/plans` | `src/modules/plans/plans.controller.ts` |
| POST | `/plans` | same |
| POST | `/plans/validate` | same |
| POST | `/plans/compat` | same |
| GET | `/plans/:planId/enrolled` | same |
| GET | `/plans/:planId` | same |
| PATCH | `/plans/:planId` | same |
| PUT | `/plans/:planId` | same |
| DELETE | `/plans/:planId` | same |
| POST | `/plans/member-plans` | same |

**Compat (root):** `POST /member-plans` → `src/modules/plans/plan-compat.controller.ts`

---

## Subscriptions & payments

| Method | Path | Source |
|--------|------|--------|
| GET | `/subscriptions` | `src/modules/subscriptions/subscriptions.controller.ts` |
| GET | `/subscriptions/:subscriptionId` | same |
| POST | `/subscriptions` | same |
| POST | `/subscriptions/:subscriptionId/renew` | same |
| POST | `/subscriptions/:subscriptionId/extend` | same |
| POST | `/subscriptions/:subscriptionId/upgrade` | same |
| POST | `/subscriptions/:subscriptionId/freeze` | same |
| POST | `/subscriptions/:subscriptionId/unfreeze` | same |
| POST | `/subscriptions/:subscriptionId/cancel` | same |
| POST | `/subscriptions/:subscriptionId/invoice` | same |
| GET | `/payments/analytics` | `src/modules/subscriptions/payments.controller.ts` |
| GET | `/payments` | same |
| POST | `/payments` | same |
| GET | `/invoices/:invoiceId` | `src/modules/subscriptions/invoices.controller.ts` |

---

## Finance & expenses

| Method | Path | Source |
|--------|------|--------|
| GET | `/finance/summary` | `src/modules/finance/finance.controller.ts` |
| GET | `/expenses/monthly-summary` | `src/modules/expenses/expenses.controller.ts` |
| GET | `/expenses` | same |
| POST | `/expenses` | same |
| GET | `/expenses/:expenseId` | same |
| PATCH | `/expenses/:expenseId` | same |
| DELETE | `/expenses/:expenseId` | same |

---

## Enquiries

| Method | Path | Source |
|--------|------|--------|
| GET | `/enquiries` | `src/modules/enquiries/enquiries.controller.ts` |
| GET | `/enquiries/stats` | same |
| POST | `/enquiries` | same |
| GET | `/enquiries/:enquiryId` | same |
| PATCH | `/enquiries/:enquiryId` | same |
| PUT | `/enquiries/:enquiryId` | same |
| POST | `/enquiries/:enquiryId/convert` | same |
| POST | `/inquiries` | `src/modules/enquiries/inquiries-compat.controller.ts` |
| GET | `/inquiries` | same |
| GET | `/inquiries/stats` | same |
| PUT | `/inquiries/:id` | same |

---

## Products (root paths)

| Method | Path | Source |
|--------|------|--------|
| POST | `/products` | `src/modules/products/products.controller.ts` |
| GET | `/products` | same |
| GET | `/products/:productId` | same |
| PUT | `/products/:productId` | same |
| DELETE | `/products/:productId` | same |
| PUT | `/products/:productId/stock` | same |
| GET | `/product-categories` | same |

---

## Meals (root paths)

| Method | Path | Source |
|--------|------|--------|
| POST | `/meals` | `src/modules/meals/meals.controller.ts` |
| GET | `/members/:memberId/meals` | same |
| GET | `/meals/:mealId` | same |
| POST | `/meals/:mealId/foods` | same |
| PUT | `/foods/:foodId` | same |
| DELETE | `/foods/:foodId` | same |
| PUT | `/meals/:mealId` | same |
| DELETE | `/meals/:mealId` | same |
| GET | `/foods/search` | same |

---

## Workouts (root paths)

| Method | Path | Source |
|--------|------|--------|
| POST | `/exercises` | `src/modules/workouts/workouts.controller.ts` |
| GET | `/exercises` | same |
| POST | `/workouts` | same |
| GET | `/members/:memberId/workouts` | same |
| GET | `/workouts/:workoutId` | same |
| POST | `/workouts/:workoutId/exercises` | same |
| DELETE | `/workouts/:workoutId/exercises/:exerciseId` | same |
| PUT | `/sets/:setId` | same |
| POST | `/exercise-sets` | same |
| POST | `/workouts/:workoutId/complete` | same |

---

## Attendance (`/attendance`)

| Method | Path | Source |
|--------|------|--------|
| POST | `/attendance` | `src/modules/attendance/attendance.controller.ts` |
| POST | `/attendance/check-in` | same |
| POST | `/attendance/check-out` | same |
| POST | `/attendance/biometric/check-in` | same |
| POST | `/attendance/biometric/register` | same |
| GET | `/attendance/biometric/credentials` | same |
| DELETE | `/attendance/biometric/credentials/:credentialId` | same |
| GET | `/attendance/logs/daily` | same |
| GET | `/attendance/logs/monthly` | same |
| GET | `/attendance/members/:memberUserId/lifetime` | same |
| GET | `/attendance/summary` | same |
| PATCH | `/attendance/members/:gymUserId/block` | same |

---

## Attendance QR (`/attendance-qr`)

| Method | Path | Source |
|--------|------|--------|
| GET | `/attendance-qr/my-qr` | `src/modules/attendance-qr/attendance-qr.controller.ts` |
| GET | `/attendance-qr/member-token` | same (legacy alias of `/attendance-qr/my-qr`) |
| POST | `/attendance-qr/punch` | same |
| POST | `/attendance-qr/punch/me` | same |

---

## Notifications, messaging, search, profile, RBAC, system

| Method | Path | Source |
|--------|------|--------|
| GET | `/notifications` | `src/modules/notifications/notifications.controller.ts` |
| PATCH | `/notifications/:id/read` | same |
| PUT | `/notifications/:id/read` | same |
| GET | `/message-templates` | `src/modules/messaging/message-templates.controller.ts` |
| PATCH | `/message-templates` | same |
| GET | `/message-templates/automation/templates` | same |
| PUT | `/message-templates/automation/templates` | same |
| POST | `/message-templates/automation/send` | same |
| GET | `/search` | `src/modules/search/search.controller.ts` |
| GET | `/profile` | `src/modules/profile/profile.controller.ts` |
| PUT | `/profile` | same |
| DELETE | `/profile` | same |
| GET | `/rbac/effective` | `src/modules/rbac/rbac.controller.ts` |
| GET | `/rbac/role-defaults/:role` | same |
| GET | `/system-config` | `src/modules/system-config/system-config.controller.ts` |
| PATCH | `/system-config` | same |

---

## SaaS (`/gym-saas`)

| Method | Path | Source |
|--------|------|--------|
| GET | `/gym-saas/entitlements` | `src/modules/saas/gym-saas.controller.ts` |
| GET | `/gym-saas/plans` | same |
| POST | `/gym-saas/custom-subscription-plans` | same |

---

## Audit (`/audit-logs`)

| Method | Path | Source |
|--------|------|--------|
| GET | `/audit-logs` | `src/modules/audit/audit.controller.ts` |

---

## Admin (`/admin`)

| Method | Path | Source |
|--------|------|--------|
| GET | `/admin/me` | `src/modules/admin/admin.controller.ts` |
| GET | `/admin/gyms` | same |
| GET | `/admin/gyms/:gymId` | same |
| PATCH | `/admin/gyms/:gymId/block` | same |
| PATCH | `/admin/gyms/:gymId/unblock` | same |
| GET | `/admin/users` | same |
| GET | `/admin/users/:userId` | same |
| GET | `/admin/analytics` | same |
| GET | `/admin/subscriptions` | same |
| PATCH | `/admin/subscriptions/:gymId` | same |
| GET | `/admin/activity` | same |
| GET | `/admin/saas-plans` | same |

---

## Platform (`/platform`) — super-admin

| Method | Path | Source |
|--------|------|--------|
| GET | `/platform/gyms` | `src/platform/platform-admin.controller.ts` |
| GET | `/platform/gyms/:gymId` | same |
| PATCH | `/platform/gyms/:gymId/status` | same |
| GET | `/platform/gyms/:gymId/subscription` | same |
| PUT | `/platform/gyms/:gymId/subscription` | same |
| GET | `/platform/revenue/overview` | same |
| GET | `/platform/saas-plans` | same |

---

## OpenAPI / Postman in repo

- `docs/owner-core.openapi.yaml` — subset (owner onboarding + members, etc.)
- `docs/mobile-auth.openapi.yaml` — mobile auth flows
- Other `docs/*.postman_collection.json` / `*.openapi.yaml` files for feature areas

---

## Regenerating this list

Routes are defined on Nest `@Controller` classes under `src/**/*.controller.ts`. After adding controllers, search for `@Get`, `@Post`, `@Put`, `@Patch`, `@Delete` and merge with the controller path prefix.
