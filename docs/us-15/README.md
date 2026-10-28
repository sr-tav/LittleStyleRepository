# US-15 — Reportes y analíticas (Dashboard Visual)

Como administrador o como vendedor, se requiere un panel visual que consolide indicadores clave de operación: ventas, demanda, inventario y comportamiento del catálogo. La historia define el acceso restringido por rol, la capa analítica, la presentación visual en Angular y la validación técnica en backend/frontend.

## 1. Descripción general

### Alcance funcional

- Consulta de métricas operativas para `ADMINISTRADOR` y `VENDEDOR`.
- Acceso restringido por rol y visibilidad del menú `Reportes`.
- Filtros dinámicos por rango de fechas y categoría.
- Presentación gráfica con tarjetas KPI y gráficos Chart.js.
- Integración entre frontend Angular y backend REST.

### Restricción por rol

| Rol | Acceso | Observación |
|---|---|---|
| `CLIENTE` | Denegado | No ve la opción `Reportes` ni accede a `/dashboard` |
| `VENDEDOR` | Permitido | Sólo ve métricas asociadas a su negocio |
| `ADMINISTRADOR` | Permitido | Vista global y operativa del negocio |

### Endpoints API REST consumidos / expuestos

- `GET /api/dashboard/overview`
  - Parámetros query: `startDate` (LocalDate), `endDate` (LocalDate), `categoryId` (Long)
  - Respuesta: objeto `DashboardOverview` con KPIs y colecciones de agregación.
  - Datos esperados:
    - `ventasTotales`
    - `pedidos`
    - `ticketPromedio`
    - `stockBajo`
    - `masConsultados`
    - `masVendidos`
    - `bajaRotacion`
    - `demandaPorTalla`
    - `ventasPorCategoria`

> Nota: en la implementación del proyecto actual la ruta técnica del backend queda en `/api/v1/dashboard`, manteniendo la misma semántica funcional del contrato de la historia.

## 2. Documentación técnica

| Archivo | Contenido |
|---|---|
| `descripcion-funcional.md` | Alcance funcional, API, DTOs, reglas, decisiones y validaciones. |
| `casos-de-prueba.md` | Casos de prueba CP-01 a CP-05 con resultado esperado. |
| `consultas-analiticas-dashboard.sql` | Consultas SQL analíticas para ventas, tallas y stock. |
| `guia-usuario-dashboard.md` | Instrucciones operativas para administrador y vendedor. |

## 3. Resumen de implementación

### Frontend

- `frontend/src/app/features/dashboard/pages/dashboard-analytics/dashboard-analytics.ts`
  - componente principal del dashboard.
  - render visual por rol.
  - filtros por fecha/categoría.
  - gráficos Chart.js por bloques visuales.

- `frontend/src/app/shared/components/navbar/navbar.ts`
  - ocultar o renderizar la opción `Reportes` según el rol.
  - control de navegación del panel administrativo/vendedor.

- `frontend/src/app/features/home/pages/vendedor-inicio/vendedor-inicio.ts`
  - pantalla de inicio del vendedor, con enlaces y contexto de acceso al dashboard.

### Backend

- `src/main/java/.../controller/DashboardController.java`
  - exposición de endpoints de dashboard.

- DTOs y servicios asociados
  - `DashboardOverviewResponse`
  - `DashboardSerieResponse`
  - `DashboardAnalyticsService`
  - `DashboardAnalyticsRepository`

### Pruebas

- `src/test/java/.../DashboardAnalyticsServiceTest.java`
- `frontend/src/app/features/dashboard/pages/dashboard-analytics/dashboard-analytics.spec.ts`
- `frontend/src/app/features/dashboard/services/dashboard-analytics.service.spec.ts`
