# syntax=docker/dockerfile:1
# ---------------------------------------------------------------------------
# Sethos Store frontend image.
#
#   docker build -t sethos-store-frontend .
#
# Builds the Angular app, then serves it from an unprivileged nginx that also
# reverse-proxies /api to the backend. Browser and API share one origin, so no
# CORS configuration is involved in the containerised setup.
# ---------------------------------------------------------------------------

# --- build ------------------------------------------------------------------
# Angular 22 needs Node >= 24.15; pinned to the current 24 LTS patch.
FROM node:24.21.0-alpine3.24 AS build
WORKDIR /workspace

ENV NG_CLI_ANALYTICS=false

# Lockfile first: `npm ci` is only re-run when dependencies change.
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

COPY . .
# The production configuration swaps in environment.production.ts (apiBaseUrl: '/api').
RUN npm run build -- --configuration production

# --- runtime ----------------------------------------------------------------
FROM nginxinc/nginx-unprivileged:1.31.6-alpine3.24 AS runtime

LABEL org.opencontainers.image.title="sethos-store-frontend" \
      org.opencontainers.image.description="Sethos Store point of sale web app"

# The image's entrypoint renders files in templates/ with envsubst, which is how the
# backend address stays configurable without rebuilding.
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /workspace/dist/sethos-store-frontend/browser/ /usr/share/nginx/html/

# Where nginx forwards /api. Matches the service name in docker-compose.yml.
ENV BACKEND_URL=http://backend:8080

# Unprivileged image: nginx runs as uid 101 and listens on 8080, not 80.
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
