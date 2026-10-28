# LittleStyle

Plataforma e-commerce de ropa infantil. Backend Spring Boot 4 (Java 25) + frontend Angular 21 (`frontend/`).

## Requisitos

- JDK 25 (`JAVA_HOME` debe apuntar al JDK 25, no a uno anterior)
- Node.js 24 y Angular CLI 21

## Ejecutar en local

**1. Backend** (perfil `local`: base H2 en `./data`, sin MySQL; crea un administrador inicial)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Queda en `http://localhost:8080`. Consola H2: `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:file:./data/littlestyle-local`, usuario `sa`, sin contraseña).

Administrador inicial: `admin@littlestyle.com` / `Admin12345` (definido en `application-local.properties`).
Clientes y vendedores se crean desde la pantalla de registro.

Con el perfil `dev` (por defecto) se usa MySQL y se requieren `DB_HOST`, `DB_USER` y `DB_PASSWORD`.

**2. Frontend**

```bash
cd frontend
npm install
ng serve
```

Abrir `http://localhost:4200`. Las llamadas a `/api` se redirigen al backend mediante `proxy.conf.json`.

## Pruebas

```bash
./mvnw test              # backend: JUnit 5 + Mockito + MockMvc
cd frontend && ng test   # frontend: Vitest
```

## Módulo de autenticación (US05)

| Endpoint | Acceso | Descripción |
|---|---|---|
| `POST /api/auth/register` | público | Registro de cliente o vendedor; devuelve JWT |
| `POST /api/auth/login` | público | Login; devuelve JWT |
| `GET /api/auth/me` | autenticado | Perfil del usuario del token |
| `/api/cliente/**`, `/api/vendedor/**`, `/api/admin/**` | por rol | Reservados para los módulos de cada rol |

- JWT HS512 sin estado (ADR-04): el filtro `JwtAuthenticationFilter` arma la autenticación desde los claims, sin consultar la base de datos.
- Variables: `JWT_SECRET` (Base64, obligatoria en `prod`), `JWT_EXPIRATION_MS` (por defecto 8 h), `CORS_ALLOWED_ORIGINS`.
- Frontend: `authInterceptor` adjunta el token y maneja 401/403; `roleGuard` (`data.roles`), `authGuard` y `guestGuard` protegen las rutas.

## Administración y moderación (US-14)

El Panel Administrativo está disponible en `/admin` para el rol `ADMINISTRADOR`.
La revisión de prendas es **posterior** a su publicación: vendedor publica en estado
`ACTIVA` y la prenda permanece visible mientras `SIN_REVISAR`; el administrador puede
aprobar, desactivar con motivo o restaurar. Las cuentas administradas pueden habilitarse,
suspenderse o bloquearse. Los cambios administrativos se registran en una auditoría
append-only con cadena SHA-256. El flujo de incidencias permite reportes de productos,
apelaciones de vendedor y solicitudes de soporte.

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/admin/resumen` | Administrador | KPIs de cuentas, moderación e incidencias. |
| `GET` | `/api/admin/usuarios` | Administrador | Listado paginado/filtros por rol, estado y texto. |
| `GET` | `/api/admin/usuarios/{id}` | Administrador | Detalle de cuenta. |
| `PUT` | `/api/admin/usuarios/{id}/estado` | Administrador | Habilitar/suspender/bloquear con motivo. |
| `GET` | `/api/admin/moderacion/prendas` | Administrador | Cola post-publicación con filtros. |
| `GET` | `/api/admin/moderacion/prendas/{id}` | Administrador | Detalle de prenda. |
| `POST` | `/api/admin/moderacion/prendas/{id}/aprobar` | Administrador | Aprobar publicación. |
| `POST` | `/api/admin/moderacion/prendas/{id}/desactivar` | Administrador | Ocultar del catálogo con motivo. |
| `POST` | `/api/admin/moderacion/prendas/{id}/restaurar` | Administrador | Restaurar prenda desactivada. |
| `GET` | `/api/admin/auditoria` | Administrador | Auditoría paginada de solo lectura. |
| `GET` | `/api/admin/auditoria/integridad` | Administrador | Verificar cadena SHA-256. |
| `GET` | `/api/admin/incidencias` | Administrador | Bandeja de incidencias y filtros. |
| `POST` | `/api/admin/incidencias/{id}/tomar` | Administrador | Asignar incidencia para revisión. |
| `POST` | `/api/admin/incidencias/{id}/resolver` | Administrador | Resolver con respuesta. |
| `POST` | `/api/admin/incidencias/{id}/rechazar` | Administrador | Rechazar con respuesta. |
| `GET`, `POST` | `/api/cliente/incidencias` | Cliente | Consultar propias y crear soporte/reporte. |
| `GET`, `POST` | `/api/vendedor/incidencias` | Vendedor | Consultar propias y crear soporte/apelación. |

**Documentación y guía de uso:** [US-14](docs/us-14/README.md), [descripción funcional y
API](docs/us-14/descripcion-funcional.md), [casos de prueba](docs/us-14/casos-de-prueba.md)
y [Guía del Panel Administrativo](docs/us-14/guia-usuario-panel-administrativo.md).

## Dashboard operativo (US-15)

La vista `/dashboard` está habilitada para `ADMINISTRADOR` y `VENDEDOR` con filtros por rango de fechas y categoría.

- Backend: `GET /api/v1/dashboard/overview`
- Autorización: la ruta y el endpoint usan `@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")`
- Payload principal: `totalVentas`, `totalPedidos`, `ticketPromedio`, `ventasPorCategoria`, `demandaPorTalla`, `productosPopulares`
- Frontend: `frontend/src/app/features/dashboard/pages/dashboard-analytics/dashboard-analytics.ts` renderiza gráficos con Chart.js

Documentación específica:
- `docs/us-15/README.md`
- `docs/us-15/descripcion-funcional.md`
- `docs/us-15/casos-de-prueba.md`
- `docs/us-15/guia-usuario-dashboard.md`

**Nota:** el dashboard usa la API analítica real y mantiene cálculos con rangos temporales y segmentación por categoría para evitar datos estáticos.
