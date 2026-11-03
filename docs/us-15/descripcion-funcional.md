# US-15 — Descripción funcional

## 1. Objetivo

La historia US-15 define un panel analítico para que administradores y vendedores consulten, en una
sola vista, las métricas operativas del negocio: ventas, pedidos, ticket promedio, inventario,
demanda por talla, ventas por categoría y comportamiento del catálogo.

El panel debe responder tres preguntas concretas:

1. **Qué se vendió** en el período y con qué categoría.
2. **Qué tallas se demandan** para orientar reposición.
3. **Qué busca y consulta** el cliente, como señal de intención.

La vista se adapta al rol sin duplicar componentes: el backend limita los datos con el alcance del
JWT y el frontend decide qué bloques mostrar.

### Objetivos del módulo

- Centralizar los indicadores en una sola petición (`/overview`).
- Permitir filtros por período, fechas manuales y categoría.
- Distinguir la vista de `ADMINISTRADOR` de la de `VENDEDOR` sin mezclar contenidos.
- Devolver ceros y listas vacías cuando no hay actividad, nunca datos de ejemplo.
- Mantener el inventario como foto actual, independiente del rango de fechas.
- Ofrecer una lectura visual clara y accesible en escritorio y en pantallas angostas.

## 2. Alcance funcional por rol

### 2.1 Administrador

Ve el consolidado de la plataforma:

- ventas totales, pedidos, ticket promedio y unidades vendidas;
- inventario disponible y variantes con stock bajo;
- tres listas de resumen: **más consultados**, **más vendidos** y **palabras más buscadas**;
- gráfico de demanda por talla y gráfico de ventas por categoría;
- bloque de exportación de reportes (US-16) con los filtros activos.

### 2.2 Vendedor

Ve únicamente su tienda, con el `vendedorId` resuelto desde el token:

- los mismos seis KPI, calculados solo sobre sus prendas y pedidos;
- gráfico de demanda por talla y gráfico de ventas por categoría de su catálogo;
- bloque de exportación con su alcance.

No se muestran las listas de “más consultados / más vendidos / más buscadas” en la vista del
vendedor: su panel prioriza la evolución de su catálogo y evita repetir información que ya puede
exportar. La separación es deliberada y no reutiliza el contenido del panel administrativo.

### 2.3 Rol no autorizado

`CLIENTE` no accede a `/dashboard` ni ve el enlace **Reportes** en el navbar. La API responde
`403`; el guard de rutas y el backend aplican la misma regla.

## 3. Requisitos funcionales

### RF-01 — Acceso autorizado

Solo `ADMINISTRADOR` y `VENDEDOR` consumen `/api/v1/dashboard/*`. Las rutas usan
`@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VENDEDOR')")` y el alcance de datos se resuelve con
`AlcanceDatos.vendedorId(usuario)`.

### RF-02 — Filtros reactivos

El panel acepta:

| Filtro | Valores | Comportamiento |
|---|---|---|
| `periodo` | `30_DIAS`, `MES_ACTUAL`, `6_MESES` | Calcula `startDate` y `endDate` y recarga |
| `startDate` / `endDate` | Fecha ISO `yyyy-MM-dd` | Se aplican si el rango es válido |
| `categoria` | `TODAS` o código de `CategoriaPrenda` | `TODAS` viaja como ausencia de parámetro |

Reglas:

- Sin fechas, el backend usa **últimos 30 días**.
- Si `startDate > endDate`, responde `400` con “La fecha inicial no puede ser posterior a la final”.
- Una categoría inexistente responde `400` con “La categoría no es válida”.
- Cambiar cualquier filtro cancela la petición anterior y lanza una nueva, sin recargar la página.

### RF-03 — Indicadores clave

Seis tarjetas KPI para ambos roles:

| KPI | Origen | Unidad |
|---|---|---|
| Ventas totales | `SUM(linea.subtotal)` de pedidos que cuentan como venta | COP |
| Unidades vendidas | `SUM(linea.cantidad)` | unidades |
| Ticket promedio | `totalVentas / totalPedidos` (2 decimales) | COP |
| Pedidos | `COUNT(DISTINCT pedido.id)` | pedidos |
| Inventario disponible | `SUM(prenda_tallas.stock)` de prendas activas | unidades |
| Variantes con stock bajo | variantes activas con `stock <= stock_minimo` | variantes |

`margenBruto` se envía como `null` porque el catálogo no guarda el costo de la prenda. El frontend
no muestra la tarjeta y explica el motivo en una nota; no inventa un 0.

### RF-04 — Inventario por variante

Cada fila de `prenda_tallas` es una variante `prenda + talla + color`, con su propio `stock` y
`stock_minimo`. Por eso:

- `stockTotal` suma el stock de todas las variantes activas;
- `stockBajo` cuenta **variantes**, no tallas distintas;
- una talla con dos colores puede aportar dos variantes y dos alertas;
- una variante sin `stock_minimo` configurado no alerta.

La etiqueta de la tarjeta dice **“Variantes con stock bajo”** para que el número coincida con lo
que realmente se cuenta. Confundirlo con “tallas” haría que el KPI pareciera menor de lo que es.

### RF-05 — Demanda por talla

La demanda se agrupa por `pedido_items.talla`, sumando cantidades y valor. El color se conserva en
la línea del pedido para auditoría, pero **no se separa en este gráfico**: la pregunta que responde
es “qué tamaño se vende más”, y un mismo tamaño repartido en varios colores debe leerse junto.

Es una limitación conocida y documentada, no un error: si se necesita el detalle por color, se
consulta el inventario o el reporte de inventario de US-16.

### RF-06 — Comportamiento del catálogo

- **Más consultados:** vistas reales de la ficha (`producto_vistas`), top 10; el frontend muestra 5.
- **Más vendidos:** unidades vendidas por prenda, top 10; el frontend muestra 5.
- **Palabras más buscadas:** términos de `busquedas`, top 10; el frontend muestra 5.

Las búsquedas son una señal de toda la plataforma: no pertenecen a un vendedor y no se filtran por
vendedor ni por categoría. Solo se guarda el término normalizado, sin identificar al cliente.

## 4. API REST

### 4.1 Ruta base

`/api/v1/dashboard`

### 4.2 Endpoints

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/overview` | `ADMINISTRADOR`, `VENDEDOR` | Resumen completo del período; única petición del frontend |
| `GET` | `/top-viewed-products` | `ADMINISTRADOR`, `VENDEDOR` | Top 10 por vistas de la ficha |
| `GET` | `/top-sold-products` | `ADMINISTRADOR`, `VENDEDOR` | Top 10 por unidades vendidas |

### 4.3 Parámetros

- `startDate` (opcional): fecha ISO `yyyy-MM-dd`.
- `endDate` (opcional): fecha ISO `yyyy-MM-dd`.
- `categoria` (opcional): código de `CategoriaPrenda` (`VESTIDOS`, `BUZOS`, …).

### 4.4 Respuesta principal

```json
{
  "totalVentas": 2450000.00,
  "totalPedidos": 327,
  "ticketPromedio": 7492.35,
  "productosVendidos": 1840,
  "margenBruto": null,
  "stockTotal": 5200,
  "stockBajo": 188,
  "ventasPorCategoria": [
    { "label": "BUZOS", "value": 850000.00, "cantidad": 126 },
    { "label": "CAMISETAS", "value": 620000.00, "cantidad": 311 }
  ],
  "demandaPorTalla": [
    { "label": "2", "value": 274, "cantidad": 274 },
    { "label": "4", "value": 231, "cantidad": 231 }
  ],
  "productosPopulares": [
    { "label": "Buzo infantil azul", "value": 420, "cantidad": 420 }
  ],
  "productosMasConsultados": [
    { "label": "Vestido rosa", "value": 198, "cantidad": 198 }
  ],
  "busquedasMasFrecuentes": [
    { "label": "buzo niño", "value": 140, "cantidad": 140 }
  ]
}
```

`DashboardSerieResponse` usa `label`, `value` y `cantidad`:

- en ventas por categoría, `value` es el monto y `cantidad` las unidades;
- en demanda, rankings y búsquedas, `value` y `cantidad` coinciden porque la métrica es un conteo.

## 5. Modelo de datos y agregación

### 5.1 Fuentes

| Tabla | Uso |
|---|---|
| `pedidos` | Rango por `fecha_creacion` y estado que cuenta como venta |
| `pedido_items` | Monto, unidades, prenda y talla de cada línea |
| `prendas` | Filtro por vendedor y categoría; estado comercial |
| `prenda_tallas` | Stock y stock mínimo **por variante** (`talla + color`) |
| `producto_vistas` | Vistas reales de la ficha |
| `busquedas` | Términos buscados en el catálogo |

### 5.2 Estados que cuentan como venta

`PAGADO`, `REGISTRADO`, `EN_PREPARACION`, `PARCIALMENTE_ENVIADO` y `ENVIADO`.

Quedan fuera `PENDIENTE_PAGO` (todavía no es venta) y `CANCELADO` (no ocurrió). Un pedido pasa a
contar desde que el pago se confirma y sigue contando mientras avanza el despacho.

### 5.3 Reglas de agregación

- Rango semiabierto `[inicio, fin)`: `fecha >= inicio` y `fecha < fin`.
- Filtro por vendedor a través de `prendas.vendedor_id`, nunca del pedido: un pedido puede mezclar
  prendas de varios vendedores.
- Categoría como enum (`CategoriaPrenda`), no como tabla.
- Orden descendente por la métrica; desempate estable por nombre o talla.
- Listas vacías y ceros cuando no hay datos; la UI muestra un estado vacío explicativo.
- Top 10 en el backend; el frontend recorta a 5 ítems para la lectura rápida.

### 5.4 DTOs

```java
public record DashboardOverviewResponse(
    BigDecimal totalVentas,
    long totalPedidos,
    BigDecimal ticketPromedio,
    long productosVendidos,
    BigDecimal margenBruto,
    long stockTotal,
    long stockBajo,
    List<DashboardSerieResponse> ventasPorCategoria,
    List<DashboardSerieResponse> demandaPorTalla,
    List<DashboardSerieResponse> productosPopulares,
    List<DashboardSerieResponse> productosMasConsultados,
    List<DashboardSerieResponse> busquedasMasFrecuentes
) {}

public record DashboardSerieResponse(String label, BigDecimal value, Long cantidad) {}
```

## 6. Diseño de interfaz y decisiones UX

### 6.1 Estructura visual

El panel se organiza de arriba hacia abajo, de lo general a lo particular:

1. **Cabecera** con título, subtítulo y filtros en una tarjeta blanca.
2. **Rejilla de KPI** de tres columnas; seis tarjetas en total.
3. **Resumen del administrador** en tres tarjetas (consultados, vendidos, búsquedas) o, en el
   vendedor, paso directo a los gráficos.
4. **Gráficos** en una rejilla `1.2fr 0.8fr`: línea para demanda por talla y barras para ventas por
   categoría.
5. **Exportar reporte**, como acción secundaria al final del flujo.

La jerarquía sigue la **ley de proximidad** de Gestalt: los filtros están juntos porque afectan a
todo el panel; los KPI comparten tamaño y borde porque son la misma clase de dato; los gráficos se
separan de las listas porque responden a otra pregunta. La **ley de similitud** se aplica con una
sola tarjeta base (fondo blanco, borde suave, radio 16 px) y color solo como acento semántico.

### 6.2 Decisiones de interacción

- **Ley de Hick:** el período se elige entre tres presets (`30 días`, `mes actual`, `6 meses`) en
  lugar de obligar a construir un rango desde cero. Las fechas manuales quedan disponibles para
  quien las necesita.
- **Ley de Miller:** seis KPI y cinco ítems por lista. Más cifras en la primera pantalla obligan a
  comparar demasiado y diluyen la lectura.
- **Ley de Fitts:** controles de 42 px de alto y botones de reintento con área cómoda; los filtros
  tienen ancho mínimo de 140–190 px para no exigir precisión excesiva.
- **Ley de Jakob:** el panel vive dentro del mismo navbar y usa los mismos patrones de tarjeta,
  tipografía y estados vacíos que el resto de la aplicación.
- **Divulgación progresiva:** el detalle operativo (exportar, imprimir, elegir formato) se concentra
  al final; el panel primero responde “cómo va el negocio”.
- **Consistencia de etiquetas:** el título de cada gráfico nombra exactamente lo que se grafica
  (“Demanda por talla (unidades)”, “Ventas por categoría (COP)”). No se usan títulos genéricos que
  prometan una métrica distinta.

### 6.3 Accesibilidad (WCAG 2.1 AA)

- Filtros agrupados con `aria-label="Filtros del dashboard"`.
- Cargador con `role="status"` y `aria-live="polite"`; el spinner es `aria-hidden`.
- Foco visible con anillo de 3 px y contraste suficiente sobre fondo claro.
- Encabezados semánticos (`h2`, `h3`) y listas reales para los rankings.
- Texto alternativo implícito en gráficos mediante el título y los estados vacíos en texto; el
  canvas se oculta cuando no hay datos para no dejar un área vacía sin explicación.
- Colores de alerta acompañados siempre de una etiqueta textual (“Variantes con stock bajo”), nunca
  solo color.

### 6.4 Diseño responsive

- Escritorio: KPI en 3 columnas, resumen en 3 columnas, gráficos en `1.2fr 0.8fr`.
- `@media (max-width: 900px)`: la cabecera pasa a columna, los filtros se alinean a la izquierda y
  las tres rejillas (`kpi-grid`, `summary-grid`, `sales-grid`) colapsan a **una columna**.
- Los filtros usan `flex-wrap` y `flex: 1 1 130px`, de modo que se acomodan en varias filas sin
  desbordar en móvil.
- Las tarjetas conservan altura legible; los gráficos mantienen `maintainAspectRatio: false` dentro
  de un contenedor de 270 px para no deformarse.
- El panel no depende de `hover`: toda la información está visible sin interacción.

## 7. Frontend Angular

- Componente standalone `DashboardAnalyticsPage` con `signals` y `computed`.
- `ngOnInit` consulta el perfil y fija `esVendedor`; si el perfil falla, asume la vista de vendedor,
  que es la más restrictiva.
- Una sola petición a `/overview` alimenta KPIs, listas y gráficos. Cambiar un filtro cancela la
  suscripción anterior (`Subscription.unsubscribe`) para evitar respuestas fuera de orden.
- Chart.js se registra una vez; los gráficos se destruyen antes de recrearse y al salir del
  componente para no filtrar instancias ni reutilizar `canvas` entre roles.
- Estados: cargando, error con botón **Reintentar**, y vacío por bloque (“Sin ventas en el período”,
  “Sin consultas…”, “Sin búsquedas…”).
- Formato local: COP sin decimales y números con separador de miles `es-CO`.

## 8. Tracking y retención

`TrackingAnalyticsService` (`@Async`, tolerante a fallos) guarda en `busquedas` cada búsqueda de la
primera página del catálogo y en `producto_vistas` cada consulta de la ficha de una prenda publicada.

Reglas anti-ruido:

| Caso | Regla |
|---|---|
| Refrescos o repetir la misma ficha/término | Mismo cliente + misma prenda o término en 30 min cuenta una vez |
| Vendedor viendo su propia prenda | Se descarta |
| Bots y scripts | Se descarta por `User-Agent` automatizado o ausente |
| Sin cliente autenticado | No se cuenta |

`RetencionAnalyticsService` borra a las 03:30 los registros de `busquedas`, `busqueda_catalogo` y
`producto_vistas` con más de 12 meses. La ventana anti-repetición vive en memoria: se pierde al
reiniciar y no se comparte entre instancias.

> El dashboard lee `busquedas`; el reporte US-16 lee `busqueda_catalogo`. Son registros distintos y
> sus totales no se suman ni se comparan.

## 9. Limitaciones y decisiones conocidas

- `margenBruto` es `null`: no hay costo registrado por prenda.
- La demanda por talla no separa color; el color se conserva en la línea del pedido.
- El stock bajo se mide por variante; una talla con varios colores puede sumar varias alertas.
- Las búsquedas no se filtran por vendedor porque no pertenecen a una tienda.
- No existe “baja rotación” en el backend; se retiró del frontend para no mostrar una lista fija o
  inventada.
- Los períodos predefinidos llegan a 6 meses; con fechas manuales se puede pedir más, pero las
  métricas de búsquedas y vistas solo reflejan los últimos 12 meses por la retención.
- El panel no reemplaza la medición operacional de RNF: no hay alertas de vencimiento ni SLA.

## 10. Casos de prueba mínimos

- `CP-01` — `CLIENTE` no accede al panel ni al enlace del navbar.
- `CP-02` — `ADMINISTRADOR` y `VENDEDOR` cargan su vista con datos reales.
- `CP-03` — filtros de período, fechas y categoría actualizan la consulta y la vista.
- `CP-04` — aislamiento por vendedor, alcance global del administrador y categoría inválida.
- `CP-05` — stock bajo contado por variante y sin datos no se inventan cifras.
- `CP-06` — pruebas unitarias backend y frontend sin fallos.

## 11. Criterios de aceptación

- [x] Solo `ADMINISTRADOR` y `VENDEDOR` acceden al panel y a la API.
- [x] El vendedor ve únicamente su tienda; el administrador ve el consolidado.
- [x] Los filtros de período, fechas y categoría recargan sin recargar la página.
- [x] Los KPI distinguen inventario total de variantes con stock bajo.
- [x] Los gráficos se destruyen y recrean sin vaciarse al cambiar de rol o filtro.
- [x] Sin actividad se muestran ceros y estados vacíos, nunca datos de ejemplo.
- [x] La interfaz colapsa a una columna en pantallas de hasta 900 px.
- [x] Las pruebas backend y frontend del módulo ejecutan sin fallos.

## 12. Trazabilidad

| Capa | Archivos |
|---|---|
| Backend | `modules/dashboard/controller/DashboardController.java`, `service/DashboardAnalyticsService.java`, `repository/DashboardAnalyticsRepository.java`, `dto/DashboardOverviewResponse.java` |
| Tracking | `TrackingAnalyticsService`, `RetencionAnalyticsService`, `CatalogoClienteController` |
| Frontend | `features/dashboard/pages/dashboard-analytics/dashboard-analytics.ts`, `features/dashboard/services/dashboard-analytics.service.ts` |
| Pruebas | `DashboardIntegrationTest`, `DashboardAnalyticsServiceTest`, `DashboardControllerTest`, `TrackingAnalyticsServiceTest`, `VentanaAntiRepeticionTest`, `RetencionAnalyticsServiceTest`, `dashboard-analytics.spec.ts`, `dashboard-analytics.service.spec.ts` |
