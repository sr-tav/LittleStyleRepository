# Imagen web (US-13): compila Angular y lo sirve con Caddy, que además hace de proxy
# inverso hacia el backend y obtiene el certificado HTTPS automáticamente en producción.

# --- Etapa 1: build de producción de Angular ---
FROM node:24-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npx ng build --configuration production

# --- Etapa 2: servidor web ---
FROM caddy:2.10-alpine
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist/frontend/browser /srv
