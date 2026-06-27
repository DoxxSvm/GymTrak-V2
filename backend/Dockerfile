# Production image for GymTrak API (Coolify: use Dockerfile build pack, context = this folder).
# syntax=docker/dockerfile:1

FROM node:22-bookworm-slim AS build
WORKDIR /app

RUN apt-get update -y \
  && apt-get install -y --no-install-recommends openssl ca-certificates \
  && rm -rf /var/lib/apt/lists/*

COPY package.json package-lock.json ./
COPY prisma ./prisma
RUN npm ci

COPY . .
RUN npx prisma generate && npm run build

ENV NODE_ENV=production
RUN npm prune --omit=dev

EXPOSE 3000
CMD ["node", "dist/main"]
