# GymTrak Backend Production Runbook

This runbook is a practical checklist for deploying `gymtrak-backend` safely.

## 1) Required environment variables

Use `.env.example` as base, then set production values.

Minimum required for server boot:

- `DATABASE_URL`
- `JWT_ACCESS_SECRET`
- `JWT_REFRESH_SECRET`

Recommended production values:

```env
NODE_ENV=production
PORT=3000

DATABASE_URL=postgresql://USER:PASSWORD@HOST:5432/DB?schema=public

JWT_ACCESS_SECRET=long-random-secret-at-least-32-chars
JWT_REFRESH_SECRET=another-long-random-secret-at-least-32-chars
JWT_ACCESS_EXPIRES_IN=15m
JWT_REFRESH_EXPIRES_IN=7d

# If Redis is not available, set true
DISABLE_BULLMQ=true
# If Redis is available, set REDIS_URL and keep DISABLE_BULLMQ false/unset
# REDIS_URL=redis://HOST:6379

# App config endpoint response
APP_NAME=GymTrak
APP_VERSION=1.0.0
APP_FORCE_UPDATE=false
APP_MAINTENANCE_MODE=false
APP_SUPPORT_CONTACT=+919999999999
APP_DEFAULT_COUNTRY_CODE=+91
APP_FEATURE_FLAGS={"owner_auth_v2":true}
APP_LOGIN_METHODS_ENABLED={"phone":true,"email":false,"google":false,"apple":false}
APP_SPLASH_ASSETS={"logo_url":"","background_url":""}
```

## 2) Migrations strategy

The app runs `prisma migrate deploy` at startup by default.

- **Default**: keep `SKIP_MIGRATIONS_ON_STARTUP` unset/false.
- **CI-managed migrations**: set `SKIP_MIGRATIONS_ON_STARTUP=true` only if your pipeline runs migrations before app start.

Manual migration command:

```bash
npx prisma migrate deploy
```

If Prisma reports failed migration (for example P3009), resolve with:

```bash
npx prisma migrate resolve --applied <migration_folder_name>
# or
npx prisma migrate resolve --rolled-back <migration_folder_name>
```

## 3) Build and start checks

Run these on server (or CI artifact stage):

```bash
npm ci
npm run build
npm run start:prod
```

Expected API base prefix is:

- `/api/v1`

## 4) Health checks

Primary health endpoint:

- `GET /api/v1/health`

App config endpoints (both supported):

- `GET /api/v1/app/config`
- `GET /api/v1/config`

Quick probe examples:

```bash
curl -i http://127.0.0.1:3000/api/v1/health
curl -i http://127.0.0.1:3000/api/v1/config
```

## 5) Gateway / reverse proxy mapping

If clients call `/api/config`, map it to backend `/api/v1/config`.

### Nginx example

```nginx
server {
  listen 80;
  server_name api.yourdomain.com;

  location /api/config {
    proxy_pass http://127.0.0.1:3000/api/v1/config;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }

  location /api/ {
    proxy_pass http://127.0.0.1:3000/;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}
```

### Load balancer health check

Configure target health check path to:

- `/api/v1/health`

Use HTTP 200 as healthy.

### Cloudflare notes

- SSL mode should be `Full` or `Full (strict)` with valid origin certs.
- Disable aggressive caching for API paths (`/api/*`), especially auth/config endpoints.

## 6) Post-deploy smoke test

Run after deployment:

```bash
curl -i https://api.yourdomain.com/api/v1/health
curl -i https://api.yourdomain.com/api/v1/config
curl -i https://api.yourdomain.com/api/config
```

Expected:

- `200` for `/api/v1/health`
- `200` for `/api/v1/config`
- `200` for `/api/config` (if gateway rewrite is configured)

## 7) Common causes of 502

- Gateway points to wrong upstream host/port.
- Gateway path does not include `/api/v1` and no rewrite exists.
- App crashed on startup due to missing required env variables.
- Database connection refused / invalid `DATABASE_URL`.
- Startup migration failed and process exited.

## 8) Recommended rollout order

1. Set env vars.
2. Verify DB connectivity from server.
3. Run migrations.
4. Start app.
5. Confirm `/api/v1/health`.
6. Apply gateway rewrite for `/api/config`.
7. Run external smoke tests.
