# --- Build ------------------------------------------------------------------
FROM node:24-alpine AS build
WORKDIR /build

# npm ci, not npm install: it installs exactly what package-lock.json pins, so the
# image is reproducible. That is also why the lockfile is committed.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Runtime ------------------------------------------------------------------
FROM nginx:alpine AS runtime

# The Angular application builder emits the browser bundle into dist/frontend/browser.
COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# 127.0.0.1 rather than localhost: busybox wget resolves localhost to ::1 and does not
# fall back to IPv4, so the probe has to name the address it means.
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=5 \
    CMD wget -qO- http://127.0.0.1/ >/dev/null 2>&1 || exit 1
