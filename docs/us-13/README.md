# US-13: Pruebas integrales E2E, corrección de errores y despliegue en AWS

## Fases

| # | Fase | Estado |
|---|------|--------|
| 1 | Contenedores Docker y `docker compose` local | Hecho |
| 2 | Despliegue manual en AWS (EC2 + RDS MySQL + S3, DuckDNS + HTTPS): [despliegue-aws.md](despliegue-aws.md) | Hecho |
| 3 | CI/CD con Jenkins: [cicd-jenkins.md](cicd-jenkins.md) | Probado en local, falta instalar en la EC2 |
| 4 | Pruebas E2E del flujo completo (Playwright) + registro en TestRail | Pendiente |
| 5 | Pruebas de rendimiento y carga de la API (Artillery) | Pendiente |
| 6 | Corrección de errores y cierre | Pendiente |

## Fase 1: contenedores

```
Navegador ──> web (Caddy :80/:443) ──/api/*, /media/*──> backend (Spring Boot :8080, perfil prod)
                 └── sirve el build de Angular (SPA)            └──> MySQL (local) / RDS (producción)
```

| Archivo | Propósito |
|---------|-----------|
| `Dockerfile` | Backend: compila con Maven Wrapper (Java 25) y ejecuta con JRE, usuario sin privilegios, perfil `prod`. |
| `frontend/Dockerfile` | Build de producción de Angular servido por Caddy. |
| `frontend/Caddyfile` | Proxy inverso a `/api` y `/media`, fallback SPA. Con `SITE_ADDRESS` = dominio activa HTTPS automático. |
| `docker-compose.yml` | Stack base (backend + web). Es el que se usa en EC2 contra RDS y S3. |
| `docker-compose.local.yml` | Agrega MySQL 8.4, `ddl-auto=update` y almacenamiento local de imágenes. |
| `.env.example` | Variables requeridas. Se copia a `.env`, que no se versiona. |

### Ejecutar en local

Requiere Docker Desktop.

```bash
cp .env.example .env
# Completar DB_PASSWORD, DB_ROOT_PASSWORD, JWT_SECRET, PERFIL_ENC_KEY, AWS_S3_BUCKET (cualquier valor en local)
#   openssl rand -base64 64 | tr -d '\r\n'   -> JWT_SECRET
#   openssl rand -base64 32 | tr -d '\r\n'   -> PERFIL_ENC_KEY
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
```

- App: http://localhost
- MySQL desde el host: `localhost:3307`
- Logs: `docker compose logs -f backend`
- Apagar: `docker compose -f docker-compose.yml -f docker-compose.local.yml down` (con `-v` también se borran los datos)

En Windows los secretos se generan con `tr -d '\r\n'`. Si queda un `\r`, el backend falla con
`Illegal base64 character`.

### Prueba de humo (fase 1)

| Verificación | Resultado esperado |
|--------------|--------------------|
| `GET /` | 200 (Angular) |
| `GET /auth/login` (ruta SPA) | 200 (fallback a `index.html`) |
| `GET /api/cliente/perfiles` sin token | 401 |
| `POST /api/auth/login` con el administrador inicial | 200 con `token` y `rol` |
