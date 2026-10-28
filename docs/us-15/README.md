# US-15 — Reportes y analíticas (Dashboard Visual)

Panel visual para `ADMINISTRADOR` y `VENDEDOR` con ventas, demanda, inventario y comportamiento del catálogo.

## 1. Alcance y reglas

| Rol | Acceso | Alcance de los datos |
|---|---|---|
| `CLIENTE` | Denegado (403) | — |
| `VENDEDOR` | Permitido | Solo su negocio: el `vendedorId` sale del token (JWT), nunca de un parámetro (RNF-15) |
| `ADMINISTRADOR` | Permitido | Toda la plataforma |

- Un pedido puede mezclar prendas de varios vendedores y no tiene vendedor propio: las métricas se filtran por el **vendedor de cada prenda**.
- Cuentan como venta los pedidos `PAGADO`, `REGISTRADO`, `EN_PREPARACION`, `PARCIALMENTE_ENVIADO` y `ENVIADO`. Quedan fuera `PENDIENTE_PAGO` y `CANCELADO`.
- **Sin datos de ejemplo:** sin actividad el backend devuelve ceros y listas vacías; si falla la base devuelve error (HTTP 5xx) y el frontend muestra "Error al cargar datos" con botón *Reintentar*.
- Las categorías son un enum (`CategoriaPrenda`), no una tabla: el filtro usa su código (`VESTIDOS`, `BUZOS`...). El selector del frontend usa `OPCIONES_CATEGORIAS`, la misma lista del catálogo.

## 2. API (`/api/v1/dashboard`)

Todos requieren rol `ADMINISTRADOR` o `VENDEDOR` y aceptan `startDate`, `endDate` (ISO, por defecto últimos 30 días) y `categoria` (opcional).

| Endpoint | Respuesta |
|---|---|
| `GET /overview` | `DashboardOverviewResponse` (única petición que usa el frontend) |
| `GET /top-viewed-products` | Top 10 de prendas por **vistas** de su ficha |
| `GET /top-sold-products` | Top 10 de prendas por unidades vendidas |

Campos de `DashboardOverviewResponse`: `totalVentas`, `totalPedidos`, `ticketPromedio`, `productosVendidos`, `margenBruto`, `stockTotal`, `stockBajo`, `ventasPorCategoria`, `demandaPorTalla`, `productosPopulares`, `productosMasConsultados`, `busquedasMasFrecuentes`.

`DashboardSerieResponse`: `label`, `value` (monto en COP; en conteos igual a `cantidad`), `cantidad` (unidades, vistas o búsquedas).

### Decisiones y límites conocidos

- **`margenBruto` es `null`:** las prendas no guardan su costo, así que no se puede calcular. El frontend lo explica en vez de mostrar 0.
- **Stock bajo:** tallas activas con `stock <= stockMinimo`. Una talla sin mínimo configurado no alerta (regla del modelo `TallaPrenda`).
- **Inventario** es la foto actual; no depende del rango de fechas.
- **Palabras más buscadas** es una señal de toda la plataforma: una búsqueda no pertenece a ningún vendedor, por eso no se filtra por vendedor ni categoría. Solo se guarda el término normalizado (sin identificar al cliente).
- **Baja rotación** no existe en el backend y se retiró del frontend (antes mostraba una lista fija).

## 3. Métricas de comportamiento (tracking)

- `TrackingAnalyticsService` (`@Async`, nunca propaga errores) guarda en `busquedas` cada búsqueda de texto de la primera página del catálogo y en `producto_vistas` cada consulta de la ficha de una prenda publicada.
- Se invoca desde `CatalogoClienteController`.

## 4. Base de datos

- En dev/local Hibernate (`ddl-auto=update`) crea las tablas `busquedas` y `producto_vistas` y los índices declarados en las entidades.
- En **prod** (`ddl-auto=validate`) hay que ejecutar antes del despliegue `migracion-analiticas.sql`. Incluye los índices `idx_pedidos_fecha_estado` y `idx_pedido_items_prenda`, que usan todas las consultas del dashboard (RNF-04).

## 5. Documentación técnica

| Archivo | Contenido |
|---|---|
| `descripcion-funcional.md` | Alcance funcional, API, DTOs, reglas, decisiones y validaciones. |
| `casos-de-prueba.md` | Casos de prueba CP-01 a CP-05 con resultado esperado. |
| `consultas-analiticas-dashboard.sql` | Consultas SQL de referencia, **desactualizadas** (usan `categoria_id`, que no existe). La implementación vigente es el JPQL de `DashboardAnalyticsRepository`. |
| `migracion-analiticas.sql` | Tablas e índices nuevos para producción. |
| `guia-usuario-dashboard.md` | Instrucciones operativas para administrador y vendedor. |

## 6. Código y pruebas

- Backend: `modules/dashboard/` (`controller`, `service`, `repository`, `model`, `dto`) y `CatalogoClienteController` (tracking).
- Frontend: `features/dashboard/pages/dashboard-analytics/` y `features/dashboard/services/`.
- Pruebas backend: `DashboardIntegrationTest` (JWT real + H2: aislamiento entre vendedores, admin global, categoría, roles, sin demo), `DashboardAnalyticsServiceTest`, `DashboardControllerTest`, `TrackingAnalyticsServiceTest`.
- Pruebas frontend: `dashboard-analytics.spec.ts`, `dashboard-analytics.service.spec.ts`.
- Pendiente (RNF-04): medir el tiempo de carga del dashboard con una prueba de carga en `pruebas-carga/` (hoy solo cubre recomendaciones).
