# Imagen web (US-13): compila Angular y lo sirve con Caddy, que además hace de proxy
# inverso hacia el backend (y Jenkins) y obtiene el certificado HTTPS automáticamente en producción.
#   docker build frontend                                        -> imagen web
#   docker build --target reportes --output reportes frontend    -> ejecuta las pruebas y la cobertura (CI)

# --- Dependencias npm ---
FROM node:24-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .

# --- Pruebas: los fallos no cortan el build para poder exportar el reporte; CI decide con él ---
FROM deps AS test
RUN mkdir -p reportes coverage/frontend && (npx ng test --watch=false --reporters=junit --reporters=default \
      --output-file=reportes/frontend.xml --coverage --coverage-reporters=cobertura \
      --coverage-reporters=html --coverage-reporters=text-summary || echo "PRUEBAS FALLIDAS")

FROM scratch AS reportes
COPY --from=test /app/reportes/ /frontend/
COPY --from=test /app/coverage/frontend/ /cobertura/frontend/

# --- Build de producción de Angular ---
FROM deps AS build
RUN npx ng build --configuration production

# --- Servidor web ---
FROM caddy:2.10-alpine
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist/frontend/browser /srv
