# Casos de prueba — US-15

## 1. Objetivo

Validar que el dashboard visual de reportes cumple las reglas de acceso por rol, la presentación de métricas y la integridad de agregación y renderizado en backend y frontend.

## 2. Casos de prueba principales

| ID | Regla / Funcionalidad | Entrada | Resultado esperado |
|---|---|---|---|
| CP-01 | Restricción de acceso por rol | Usuario con rol `CLIENTE` intenta acceder a `/dashboard` o consumir la API | Acceso denegado y la opción `Reportes` no se renderiza en el navbar. |
| CP-02 | Visualización del dashboard | Sesión autenticada con `VENDEDOR` o `ADMINISTRADOR` | Carga del panel con diseño en rejilla y KPIs vigentes. |
| CP-03 | Filtrado dinámico | Cambia `startDate`, `endDate` y/o `categoryId` | Se ejecuta la petición con nuevos parámetros y la vista se actualiza sin flickering. |
| CP-04 | Pruebas unitarias backend | Ejecuta servicio JUnit/Mockito | Validación de `GROUP BY`, sumatorias `SUM` y manejo de listas vacías. |
| CP-05 | Pruebas unitarias frontend | Ejecuta specs Vitest | Validación del componente y del servicio del dashboard. |

## 3. Casos detallados

### CP-01 — Restricción de acceso por rol (guarda y navbar)

- Entrada: usuario autenticado con rol `CLIENTE` intenta abrir `/dashboard`.
- Resultado esperado:
  - acceso denegado o redirección
  - no se renderiza el enlace `Reportes` en el navbar
  - no se despliega contenido analítico

### CP-02 — Visualización del dashboard (ADMINISTRADOR / VENDEDOR)

- Entrada: iniciar sesión como `VENDEDOR` o `ADMINISTRADOR` y navegar a `/dashboard`.
- Resultado esperado:
  - carga del dashboard con KPI principal
  - render visual en Chart.js
  - interfaz diferenciada por rol
  - para vendedor: KPI y gráficos en dos columnas
  - para administrador: contenido global y no mezclado con la vista del vendedor

### CP-03 — Filtrado dinámico de métricas

- Entrada: selección de fechas y categoría.
- Resultado esperado:
  - se llama a la API con los parámetros actualizados
  - se re-renderiza automáticamente la vista
  - no aparecen gráficos vacíos ni flickering visual

### CP-04 — Pruebas unitarias del backend (JUnit / Mockito)

- Resultado esperado:
  - `DashboardAnalyticsServiceTest` valida `GROUP BY` y sumatorias
  - rutas con fechas no válidas devuelven error
  - listas vacías no rompen la lógica ni la serialización

### CP-05 — Pruebas unitarias del frontend (Vitest)

- Resultado esperado:
  - `DashboardAnalyticsComponent` se monta correctamente
  - `DashboardAnalyticsService` formatea parámetros y resuelve respuestas
  - los tests de renderizado y permisos no fallan

## 4. Matriz de evidencia recomendada

| Capa | Técnica | Evidencia |
|---|---|---|
| Backend | JUnit 5 + Mockito | `DashboardAnalyticsServiceTest` |
| Controller | MockMvc / Security Test | validación de permisos y serialización JSON |
| Frontend | Vitest + Angular Testing | `dashboard-analytics.spec.ts` y servicio del dashboard |
| Render | Chart.js | cargas con datos reales y estados vacíos |

## 5. Criterios de aceptación

- [x] El usuario con rol no autorizado no accede al dashboard.
- [x] El usuario autorizado visualiza métricas y gráficos.
- [x] Los filtros de fechas y categoría alteran la consulta y la vista.
- [x] La vista de vendedor y la de administrador están separadas.
- [x] Las pruebas unitarias backend y frontend ejecutan correctamente.
