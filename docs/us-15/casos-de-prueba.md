# Casos de prueba — US-15

## 1. Objetivo

Validar que el dashboard cumple las reglas de acceso por rol, la presentación de métricas, la
integridad de las agregaciones y el diseño responsive, tanto en backend como en frontend.

## 2. Casos de prueba principales

| ID | Regla / funcionalidad | Entrada | Resultado esperado |
|---|---|---|---|
| CP-01 | Restricción de acceso por rol | `CLIENTE` intenta abrir `/dashboard` o consumir la API | Acceso denegado (`403`), sin contenido analítico y sin enlace `Reportes` en el navbar |
| CP-02 | Visualización por rol | Sesión `VENDEDOR` o `ADMINISTRADOR` | Carga el panel con KPIs reales y la composición que corresponde a cada rol |
| CP-03 | Filtrado dinámico | Cambia período, fechas o categoría | Se lanza una petición con los parámetros nuevos y la vista se actualiza sin recargar la página |
| CP-04 | Alcance de datos | Vendedor con datos propios y administrador | El vendedor solo ve su tienda; el administrador ve el consolidado |
| CP-05 | Inventario por variante | Prenda con la misma talla en dos colores y distinto stock | El stock total suma ambas variantes; el stock bajo cuenta variantes, no tallas |
| CP-06 | Sin datos | Base sin ventas, vistas ni búsquedas | Ceros y listas vacías; nunca cifras de ejemplo |
| CP-07 | Fechas inválidas y categoría inexistente | `startDate > endDate`; categoría desconocida | `400` con mensaje claro; el frontend muestra el error sin romper el panel |
| CP-08 | Diseño responsive | Ancho de ventana ≤ 900 px | Cabecera, KPIs, resumen y gráficos en una columna; filtros en varias filas sin desbordar |
| CP-09 | Pruebas unitarias backend | JUnit/Mockito | Agregaciones, estados de venta, alcance y listas vacías |
| CP-10 | Pruebas unitarias frontend | Vitest | Componente, servicio, estados y etiquetas de KPI |

## 3. Casos detallados

### CP-01 — Restricción de acceso por rol

- Entrada: usuario autenticado con rol `CLIENTE` intenta abrir `/dashboard`.
- Resultado esperado:
  - el guard redirige o deniega el acceso;
  - no se renderiza el enlace `Reportes` en el navbar;
  - la API responde `403` si se invoca directamente;
  - no se muestra ningún contenido analítico.

### CP-02 — Visualización del dashboard por rol

- Entrada: iniciar sesión como `VENDEDOR` o `ADMINISTRADOR` y navegar a `/dashboard`.
- Resultado esperado:
  - seis tarjetas KPI: ventas totales, unidades vendidas, ticket promedio, pedidos, inventario
    disponible y variantes con stock bajo;
  - el administrador ve las tres listas de resumen (más consultados, más vendidos, palabras más
    buscadas) y los dos gráficos;
  - el vendedor ve los dos gráficos de su tienda y no ve las listas del administrador;
  - los títulos de los gráficos coinciden con lo graficado: “Demanda por talla (unidades)” y
    “Ventas por categoría (COP)”;
  - la tarjeta de margen no aparece y se explica que falta el costo de la prenda.

### CP-03 — Filtrado dinámico de métricas

- Entrada: seleccionar `30_DIAS`, `MES_ACTUAL`, `6_MESES`, fechas manuales o una categoría.
- Resultado esperado:
  - se llama a `/overview` con `startDate`, `endDate` y `categoria` correctos;
  - la petición anterior se cancela y no pisa la respuesta nueva;
  - la vista se re-renderiza sin recargar la página;
  - los gráficos se destruyen y recrean sin quedar vacíos.

### CP-04 — Alcance por rol

- Entrada: vendedor con pedidos propios y administrador con datos de varias tiendas.
- Resultado esperado:
  - el vendedor recibe métricas filtradas por `prendas.vendedor_id` de su token;
  - el administrador recibe el consolidado;
  - ningún parámetro de la URL permite pedir otro alcance;
  - las búsquedas no se filtran por vendedor porque son una señal de plataforma.

### CP-05 — Inventario por variante

- Entrada: prenda activa con talla `4` en `AZUL` (stock 2) y `ROJO` (stock 9), con el mismo
  `stock_minimo`.
- Resultado esperado:
  - `stockTotal` suma 11;
  - `stockBajo` cuenta dos variantes si ambas están en el umbral, no una talla;
  - el detalle de inventario y el reporte US-16 muestran dos filas con su color;
  - la etiqueta visible es “Variantes con stock bajo”.

### CP-06 — Sin datos

- Entrada: base sin actividad en el rango.
- Resultado esperado:
  - `totalVentas`, `totalPedidos`, `ticketPromedio`, `productosVendidos`, `stockTotal` y
    `stockBajo` en cero;
  - listas vacías;
  - estados “Sin ventas en el período seleccionado”, “Sin consultas…”, “Sin búsquedas…”;
  - el test verifica que no aparezcan cifras de ejemplo.

### CP-07 — Validaciones de filtros

- Entrada: `startDate` posterior a `endDate`; categoría que no existe en el enum.
- Resultado esperado:
  - `400` con “La fecha inicial no puede ser posterior a la final” o “La categoría no es válida”;
  - el frontend muestra un mensaje accionable y conserva los filtros para corregirlos.

### CP-08 — Diseño responsive y accesibilidad

- Entrada: ventana de 900 px o menos; navegación con teclado.
- Resultado esperado:
  - `kpi-grid`, `summary-grid` y `sales-grid` pasan a una columna;
  - los filtros se acomodan en varias filas con `flex-wrap`;
  - los controles conservan 42 px de alto y foco visible;
  - el cargador anuncia el estado con `role="status"` y `aria-live="polite"`;
  - los gráficos mantienen proporción y no desbordan su tarjeta.

### CP-09 — Pruebas unitarias del backend

- Resultado esperado:
  - `DashboardAnalyticsServiceTest` valida sumatorias, agrupaciones, estados que cuentan como venta
    y `margenBruto = null`;
  - `DashboardIntegrationTest` valida aislamiento entre vendedores, alcance global, categoría y
    roles con JWT real sobre H2;
  - `DashboardControllerTest` valida parámetros y serialización;
  - listas vacías no rompen la lógica ni la respuesta.

### CP-10 — Pruebas unitarias del frontend

- Resultado esperado:
  - `DashboardAnalyticsPage` se monta y distingue la vista por rol;
  - el servicio arma los parámetros y resuelve la respuesta;
  - se verifica la etiqueta “Variantes con stock bajo” y la ausencia de “Baja rotación”;
  - los estados de carga, error y vacío se renderizan correctamente.

## 4. Matriz de evidencia

| Capa | Técnica | Evidencia |
|---|---|---|
| Backend — agregación | JUnit 5 + Mockito + AssertJ | `DashboardAnalyticsServiceTest` |
| Backend — integración | JWT real + H2 + MockMvc | `DashboardIntegrationTest` |
| Backend — contrato | MockMvc | `DashboardControllerTest` |
| Tracking y retención | JUnit 5 | `TrackingAnalyticsServiceTest`, `VentanaAntiRepeticionTest`, `RetencionAnalyticsServiceTest`, `RetencionAnalyticsIntegrationTest` |
| Frontend | Vitest + Angular Testing | `dashboard-analytics.spec.ts`, `dashboard-analytics.service.spec.ts` |
| Diseño | Revisión visual y de teclado | Pruebas manuales de CP-08 |

## 5. Criterios de aceptación

- [x] El rol no autorizado no accede al dashboard ni al enlace del navbar.
- [x] El usuario autorizado visualiza métricas y gráficos reales.
- [x] Los filtros de período, fechas y categoría alteran la consulta y la vista.
- [x] La vista del vendedor y la del administrador están separadas.
- [x] El stock bajo se cuenta por variante (`talla + color`).
- [x] Sin datos se muestran ceros y estados vacíos.
- [x] La interfaz colapsa a una columna en pantallas de hasta 900 px.
- [x] Las pruebas backend y frontend del módulo ejecutan correctamente.
