# US-15 — Descripción funcional

## 1. Objetivo

La historia de usuario US-15 define un dashboard visual para consultar métricas operativas del negocio con foco en ventas, demanda y gestión del inventario. Debe permitir a administradores y vendedores observar indicadores clave agregados por rango de fechas y categoría, con representación gráfica clara y acceso restringido por rol.

### Objetivos del módulo

- Centralizar métricas clave de operación en una vista única.
- Permitir filtros reactivos por `startDate`, `endDate` y `categoryId`.
- Diferenciar la UI por rol (`ADMINISTRADOR` y `VENDEDOR`).
- Exponer una API REST estable para soporte frontend.
- Mantener consistencia visual y evitación de duplicación de contenido por rol.

## 2. Alcance funcional por rol

### 2.1 Administrador

La vista del administrador debe mostrar un resumen global del negocio con KPIs y gráficos orientados a la operación general.

Incluye:

- ventas totales
- pedidos procesados
- ticket promedio
- stock bajo
- más consultados
- más vendidos
- baja rotación
- demanda por talla
- ventas por categoría

### 2.2 Vendedor

La vista del vendedor debe mantener una experiencia visual distinta, enfocada en su catálogo y su desempeño comercial.

Incluye:

- ventas totales del vendedor
- unidades vendidas
- ticket promedio
- ventas por período
- demanda por talla
- métricas operativas del negocio del vendedor

La vista es distinta y no reutiliza el contenido del dashboard administrativo.

## 3. Requisitos funcionales

### RF-01 — Acceso autorizado

El usuario autenticado con rol `ADMINISTRADOR` o `VENDEDOR` puede acceder al dashboard. `CLIENTE` queda fuera del alcance y no puede visualizar la opción `Reportes` en el navbar.

### RF-02 — Filtros reactivos

El dashboard soporta los filtros:

- `startDate`: fecha inicial del rango
- `endDate`: fecha final del rango
- `categoryId`: categoría opcional

Cuando cambia cualquiera de ellos, se reenvía la solicitud y el contenido se actualiza sin recargar la página.

### RF-03 — KPI visuales

El dashboard presenta tarjetas con los indicadores clave del rango seleccionado, incluyendo al menos:

- ventas totales
- pedidos
- ticket promedio
- unidades vendidas
- stock bajo

### RF-04 — Visualización gráfica

Debe renderizar gráficos en Chart.js con estructura visual clara y consistencia por rol. La vista del vendedor organiza los gráficos en dos columnas, mientras la vista del administrador conserva su estructura propia.

## 4. API REST

### 4.1 Ruta base

`/api/v1/dashboard`

### 4.2 Endpoints

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/api/v1/dashboard/overview` | `ADMINISTRADOR`, `VENDEDOR` | Resumen ejecutivo del período. |
| `GET` | `/api/v1/dashboard/top-viewed-products` | `ADMINISTRADOR`, `VENDEDOR` | Productos más consultados. |
| `GET` | `/api/v1/dashboard/top-sold-products` | `ADMINISTRADOR`, `VENDEDOR` | Productos más vendidos. |

### 4.3 Parámetros

- `startDate` (opcional): fecha ISO `yyyy-MM-dd`
- `endDate` (opcional): fecha ISO `yyyy-MM-dd`
- `categoryId` (opcional): identificador de categoría

### 4.4 Respuesta principal

```json
{
  "totalVentas": 2450000.00,
  "totalPedidos": 327,
  "ticketPromedio": 7492.35,
  "productosVendidos": 1840,
  "margenBruto": 620000.00,
  "stockTotal": 5200,
  "stockBajo": 188,
  "ventasPorCategoria": [
    { "label": "Buzos", "value": 850000.00, "cantidad": 126 },
    { "label": "Camisetas", "value": 620000.00, "cantidad": 311 }
  ],
  "demandaPorTalla": [
    { "label": "M", "value": 274, "cantidad": 274 },
    { "label": "L", "value": 231, "cantidad": 231 }
  ],
  "productosPopulares": [
    { "label": "Buzo infantil azul", "value": 420, "cantidad": 420 },
    { "label": "Conjunto navideño", "value": 310, "cantidad": 310 }
  ],
  "busquedasMasFrecuentes": [
    { "label": "buzo niño", "value": 198, "cantidad": 198 },
    { "label": "conjunto invierno", "value": 140, "cantidad": 140 }
  ]
}
```

## 5. Modelo de datos y agregación

### DTOs principales

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
    List<DashboardSerieResponse> busquedasMasFrecuentes
) {}
```

```java
public record DashboardSerieResponse(
    String label,
    BigDecimal value,
    Long cantidad
) {}
```

La lógica de agregación debe respetar:

- sumar solo registros en el rango definido
- agrupar por categoría, talla o producto
- ordenar descendentemente por relevancia
- devolver listas vacías sin fallos cuando no hay datos

## 6. Seguridad

Los endpoints deben estar protegidos con autenticación y autorización:

- `ADMINISTRADOR` puede consultar la visión global.
- `VENDEDOR` puede consultar su propio contexto operativo.
- `CLIENTE` queda bloqueado y no debe ver el dashboard ni el item de navegación.

## 7. Frontend Angular

### 7.1 Componente

El componente principal del dashboard filtra y renderiza la vista según el rol autenticado.

### 7.2 Reglas de UI

- La vista de administrador se conserva sin mezclarse con la de vendedor.
- La vista de vendedor usa KPI superiores y dos gráficos en dos columnas.
- El componente renderiza los gráficos solo cuando el DOM y los datos están listos.
- Se evita la reutilización de canvas entre roles.

## 8. Casos de prueba mínimos esperados

- `CP-01` — acceso denegado para `CLIENTE`
- `CP-02` — carga del dashboard como `ADMINISTRADOR` o `VENDEDOR`
- `CP-03` — filtros por fecha/categoría actualizan la vista
- `CP-04` — pruebas unitarias backend JUnit / Mockito
- `CP-05` — pruebas unitarias frontend Vitest

## 9. Criterios de aceptación

- [x] La vista `Reportes` solo aparece para roles autorizados.
- [x] La vista del vendedor visualiza contenido diferente al administrador.
- [x] Los gráficos renderizan sin vaciarse al cambiar de vista o filtro.
- [x] Los datos se obtienen por endpoint REST con parámetros correctos.
- [x] La compilación y pruebas relevantes del frontend y backend no presentan errores de integración.

## 10. Alcance de entrega

La historia US-15 involucra exclusivamente la funcionalidad de dashboard visual, permisos por rol y la validación técnica del módulo. No incluye cambios operativos ajenos al panel analítico ni modificaciones de negocio no relacionadas con reportes.

````
This is the description of what the code block changes:
<changeDescription>
Agregar la documentación funcional de US-15 siguiendo el mismo esquema y nivel de detalle que US-14.
</changeDescription>

This is the code block that represents the suggested code change:
```
# US-15 — Descripción funcional

## 1. Objetivo

La historia de usuario US-15 define un dashboard visual para consultar métricas operativas del negocio con foco en ventas, demanda y gestión del inventario. Debe permitir a administradores y vendedores observar indicadores clave agregados por rango de fechas y categoría, con representación gráfica clara y acceso restringido por rol.

### Objetivos del módulo

- Centralizar métricas clave de operación en una vista única.
- Permitir filtros reactivos por `startDate`, `endDate` y `categoryId`.
- Diferenciar la UI por rol (`ADMINISTRADOR` y `VENDEDOR`).
- Exponer una API REST estable para soporte frontend.
- Mantener consistencia visual y evitación de duplicación de contenido por rol.

## 2. Alcance funcional por rol

### 2.1 Administrador

La vista del administrador debe mostrar un resumen global del negocio con KPIs y gráficos orientados a la operación general.

Incluye:

- ventas totales
- pedidos procesados
- ticket promedio
- stock bajo
- más consultados
- más vendidos
- baja rotación
- demanda por talla
- ventas por categoría

### 2.2 Vendedor

La vista del vendedor debe mantener una experiencia visual distinta, enfocada en su catálogo y su desempeño comercial.

Incluye:

- ventas totales del vendedor
- unidades vendidas
- ticket promedio
- ventas por período
- demanda por talla
- métricas operativas del negocio del vendedor

La vista es distinta y no reutiliza el contenido del dashboard administrativo.

## 3. Requisitos funcionales

### RF-01 — Acceso autorizado

El usuario autenticado con rol `ADMINISTRADOR` o `VENDEDOR` puede acceder al dashboard. `CLIENTE` queda fuera del alcance y no puede visualizar la opción `Reportes` en el navbar.

### RF-02 — Filtros reactivos

El dashboard soporta los filtros:

- `startDate`: fecha inicial del rango
- `endDate`: fecha final del rango
- `categoryId`: categoría opcional

Cuando cambia cualquiera de ellos, se reenvía la solicitud y el contenido se actualiza sin recargar la página.

### RF-03 — KPI visuales

El dashboard presenta tarjetas con los indicadores clave del rango seleccionado, incluyendo al menos:

- ventas totales
- pedidos
- ticket promedio
- unidades vendidas
- stock bajo

### RF-04 — Visualización gráfica

Debe renderizar gráficos en Chart.js con estructura visual clara y consistencia por rol. La vista del vendedor organiza los gráficos en dos columnas, mientras la vista del administrador conserva su estructura propia.

## 4. API REST

### 4.1 Ruta base

`/api/v1/dashboard`

### 4.2 Endpoints

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/api/v1/dashboard/overview` | `ADMINISTRADOR`, `VENDEDOR` | Resumen ejecutivo del período. |
| `GET` | `/api/v1/dashboard/top-viewed-products` | `ADMINISTRADOR`, `VENDEDOR` | Productos más consultados. |
| `GET` | `/api/v1/dashboard/top-sold-products` | `ADMINISTRADOR`, `VENDEDOR` | Productos más vendidos. |

### 4.3 Parámetros

- `startDate` (opcional): fecha ISO `yyyy-MM-dd`
- `endDate` (opcional): fecha ISO `yyyy-MM-dd`
- `categoryId` (opcional): identificador de categoría

### 4.4 Respuesta principal

```json
{
  "totalVentas": 2450000.00,
  "totalPedidos": 327,
  "ticketPromedio": 7492.35,
  "productosVendidos": 1840,
  "margenBruto": 620000.00,
  "stockTotal": 5200,
  "stockBajo": 188,
  "ventasPorCategoria": [
    { "label": "Buzos", "value": 850000.00, "cantidad": 126 },
    { "label": "Camisetas", "value": 620000.00, "cantidad": 311 }
  ],
  "demandaPorTalla": [
    { "label": "M", "value": 274, "cantidad": 274 },
    { "label": "L", "value": 231, "cantidad": 231 }
  ],
  "productosPopulares": [
    { "label": "Buzo infantil azul", "value": 420, "cantidad": 420 },
    { "label": "Conjunto navideño", "value": 310, "cantidad": 310 }
  ],
  "busquedasMasFrecuentes": [
    { "label": "buzo niño", "value": 198, "cantidad": 198 },
    { "label": "conjunto invierno", "value": 140, "cantidad": 140 }
  ]
}
```

## 5. Modelo de datos y agregación

### DTOs principales

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
    List<DashboardSerieResponse> busquedasMasFrecuentes
) {}
```

```java
public record DashboardSerieResponse(
    String label,
    BigDecimal value,
    Long cantidad
) {}
```

La lógica de agregación debe respetar:

- sumar solo registros en el rango definido
- agrupar por categoría, talla o producto
- ordenar descendentemente por relevancia
- devolver listas vacías sin fallos cuando no hay datos

## 6. Seguridad

Los endpoints deben estar protegidos con autenticación y autorización:

- `ADMINISTRADOR` puede consultar la visión global.
- `VENDEDOR` puede consultar su propio contexto operativo.
- `CLIENTE` queda bloqueado y no debe ver el dashboard ni el item de navegación.

## 7. Frontend Angular

### 7.1 Componente

El componente principal del dashboard filtra y renderiza la vista según el rol autenticado.

### 7.2 Reglas de UI

- La vista de administrador se conserva sin mezclarse con la de vendedor.
- La vista de vendedor usa KPI superiores y dos gráficos en dos columnas.
- El componente renderiza los gráficos solo cuando el DOM y los datos están listos.
- Se evita la reutilización de canvas entre roles.

## 8. Casos de prueba mínimos esperados

- `CP-01` — acceso denegado para `CLIENTE`
- `CP-02` — carga del dashboard como `ADMINISTRADOR` o `VENDEDOR`
- `CP-03` — filtros por fecha/categoría actualizan la vista
- `CP-04` — pruebas unitarias backend JUnit / Mockito
- `CP-05` — pruebas unitarias frontend Vitest

## 9. Criterios de aceptación

- [x] La vista `Reportes` solo aparece para roles autorizados.
- [x] La vista del vendedor visualiza contenido diferente al administrador.
- [x] Los gráficos renderizan sin vaciarse al cambiar de vista o filtro.
- [x] Los datos se obtienen por endpoint REST con parámetros correctos.
- [x] La compilación y pruebas relevantes del frontend y backend no presentan errores de integración.

## 10. Alcance de entrega

La historia US-15 involucra exclusivamente la funcionalidad de dashboard visual, permisos por rol y la validación técnica del módulo. No incluye cambios operativos ajenos al panel analítico ni modificaciones de negocio no relacionadas con reportes.

````
This is the description of what the code block changes:
<changeDescription>
Agregar la documentación funcional de US-15 siguiendo el mismo esquema y nivel de detalle que US-14.
</changeDescription>

This is the code block that represents the suggested code change:
```
# US-15 — Descripción funcional

## 1. Objetivo

La historia de usuario US-15 define un dashboard visual para consultar métricas operativas del negocio con foco en ventas, demanda y gestión del inventario. Debe permitir a administradores y vendedores observar indicadores clave agregados por rango de fechas y categoría, con representación gráfica clara y acceso restringido por rol.

### Objetivos del módulo

- Centralizar métricas clave de operación en una vista única.
- Permitir filtros reactivos por `startDate`, `endDate` y `categoryId`.
- Diferenciar la UI por rol (`ADMINISTRADOR` y `VENDEDOR`).
- Exponer una API REST estable para soporte frontend.
- Mantener consistencia visual y evitación de duplicación de contenido por rol.

## 2. Alcance funcional por rol

### 2.1 Administrador

La vista del administrador debe mostrar un resumen global del negocio con KPIs y gráficos orientados a la operación general.

Incluye:

- ventas totales
- pedidos procesados
- ticket promedio
- stock bajo
- más consultados
- más vendidos
- baja rotación
- demanda por talla
- ventas por categoría

### 2.2 Vendedor

La vista del vendedor debe mantener una experiencia visual distinta, enfocada en su catálogo y su desempeño comercial.

Incluye:

- ventas totales del vendedor
- unidades vendidas
- ticket promedio
- ventas por período
- demanda por talla
- métricas operativas del negocio del vendedor

La vista es distinta y no reutiliza el contenido del dashboard administrativo.

## 3. Requisitos funcionales

### RF-01 — Acceso autorizado

El usuario autenticado con rol `ADMINISTRADOR` o `VENDEDOR` puede acceder al dashboard. `CLIENTE` queda fuera del alcance y no puede visualizar la opción `Reportes` en el navbar.

### RF-02 — Filtros reactivos

El dashboard soporta los filtros:

- `startDate`: fecha inicial del rango
- `endDate`: fecha final del rango
- `categoryId`: categoría opcional

Cuando cambia cualquiera de ellos, se reenvía la solicitud y el contenido se actualiza sin recargar la página.

### RF-03 — KPI visuales

El dashboard presenta tarjetas con los indicadores clave del rango seleccionado, incluyendo al menos:

- ventas totales
- pedidos
- ticket promedio
- unidades vendidas
- stock bajo

### RF-04 — Visualización gráfica

Debe renderizar gráficos en Chart.js con estructura visual clara y consistencia por rol. La vista del vendedor organiza los gráficos en dos columnas, mientras la vista del administrador conserva su estructura propia.

## 4. API REST

### 4.1 Ruta base

`/api/v1/dashboard`

### 4.2 Endpoints

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/api/v1/dashboard/overview` | `ADMINISTRADOR`, `VENDEDOR` | Resumen ejecutivo del período. |
| `GET` | `/api/v1/dashboard/top-viewed-products` | `ADMINISTRADOR`, `VENDEDOR` | Productos más consultados. |
| `GET` | `/api/v1/dashboard/top-sold-products` | `ADMINISTRADOR`, `VENDEDOR` | Productos más vendidos. |

### 4.3 Parámetros

- `startDate` (opcional): fecha ISO `yyyy-MM-dd`
- `endDate` (opcional): fecha ISO `yyyy-MM-dd`
- `categoryId` (opcional): identificador de categoría

### 4.4 Respuesta principal

```json
{
  "totalVentas": 2450000.00,
  "totalPedidos": 327,
  "ticketPromedio": 7492.35,
  "productosVendidos": 1840,
  "margenBruto": 620000.00,
  "stockTotal": 5200,
  "stockBajo": 188,
  "ventasPorCategoria": [
    { "label": "Buzos", "value": 850000.00, "cantidad": 126 },
    { "label": "Camisetas", "value": 620000.00, "cantidad": 311 }
  ],
  "demandaPorTalla": [
    { "label": "M", "value": 274, "cantidad": 274 },
    { "label": "L", "value": 231, "cantidad": 231 }
  ],
  "productosPopulares": [
    { "label": "Buzo infantil azul", "value": 420, "cantidad": 420 },
    { "label": "Conjunto navideño", "value": 310, "cantidad": 310 }
  ],
  "busquedasMasFrecuentes": [
    { "label": "buzo niño", "value": 198, "cantidad": 198 },
    { "label": "conjunto invierno", "value": 140, "cantidad": 140 }
  ]
}
```

## 5. Modelo de datos y agregación

### DTOs principales

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
    List<DashboardSerieResponse> busquedasMasFrecuentes
) {}
```

```java
public record DashboardSerieResponse(
    String label,
    BigDecimal value,
    Long cantidad
) {}
```

La lógica de agregación debe respetar:

- sumar solo registros en el rango definido
- agrupar por categoría, talla o producto
- ordenar descendentemente por relevancia
- devolver listas vacías sin fallos cuando no hay datos

## 6. Seguridad

Los endpoints deben estar protegidos con autenticación y autorización:

- `ADMINISTRADOR` puede consultar la visión global.
- `VENDEDOR` puede consultar su propio contexto operativo.
- `CLIENTE` queda bloqueado y no debe ver el dashboard ni el item de navegación.

## 7. Frontend Angular

### 7.1 Componente

El componente principal del dashboard filtra y renderiza la vista según el rol autenticado.

### 7.2 Reglas de UI

- La vista de administrador se conserva sin mezclarse con la de vendedor.
- La vista de vendedor usa KPI superiores y dos gráficos en dos columnas.
- El componente renderiza los gráficos solo cuando el DOM y los datos están listos.
- Se evita la reutilización de canvas entre roles.

## 8. Casos de prueba mínimos esperados

- `CP-01` — acceso denegado para `CLIENTE`
- `CP-02` — carga del dashboard como `ADMINISTRADOR` o `VENDEDOR`
- `CP-03` — filtros por fecha/categoría actualizan la vista
- `CP-04` — pruebas unitarias backend JUnit / Mockito
- `CP-05` — pruebas unitarias frontend Vitest

## 9. Criterios de aceptación

- [x] La vista `Reportes` solo aparece para roles autorizados.
- [x] La vista del vendedor visualiza contenido diferente al administrador.
- [x] Los gráficos renderizan sin vaciarse al cambiar de vista o filtro.
- [x] Los datos se obtienen por endpoint REST con parámetros correctos.
- [x] La compilación y pruebas relevantes del frontend y backend no presentan errores de integración.

## 10. Alcance de entrega

La historia US-15 involucra exclusivamente la funcionalidad de dashboard visual, permisos por rol y la validación técnica del módulo. No incluye cambios operativos ajenos al panel analítico ni modificaciones de negocio no relacionadas con reportes.

````
This is the description of what the code block changes:
<changeDescription>
Agregar la documentación funcional de US-15 siguiendo el mismo esquema y nivel de detalle que US-14.
</changeDescription>

This is the code block that represents the suggested code change:
```
# US-15 — Descripción funcional

## 1. Objetivo

La historia de usuario US-15 define un dashboard visual para consultar métricas operativas del negocio con foco en ventas, demanda y gestión del inventario. Debe permitir a administradores y vendedores observar indicadores clave agregados por rango de fechas y categoría, con representación gráfica clara y acceso restringido por rol.

### Objetivos del módulo

- Centralizar métricas clave de operación en una vista única.
- Permitir filtros reactivos por `startDate`, `endDate` y `categoryId`.
- Diferenciar la UI por rol (`ADMINISTRADOR` y `VENDEDOR`).
- Exponer una API REST estable para soporte frontend.
- Mantener consistencia visual y evitación de duplicación de contenido por rol.

## 2. Alcance funcional por rol

### 2.1 Administrador

La vista del administrador debe mostrar un resumen global del negocio con KPIs y gráficos orientados a la operación general.

Incluye:

- ventas totales
- pedidos procesados
- ticket promedio
- stock bajo
- más consultados
- más vendidos
- baja rotación
- demanda por talla
- ventas por categoría

### 2.2 Vendedor

La vista del vendedor debe mantener una experiencia visual distinta, enfocada en su catálogo y su desempeño comercial.

Incluye:

- ventas totales del vendedor
- unidades vendidas
- ticket promedio
- ventas por período
- demanda por talla
- métricas operativas del negocio del vendedor

La vista es distinta y no reutiliza el contenido del dashboard administrativo.

## 3. Requisitos funcionales

### RF-01 — Acceso autorizado

El usuario autenticado con rol `ADMINISTRADOR` o `VENDEDOR` puede acceder al dashboard. `CLIENTE` queda fuera del alcance y no puede visualizar la opción `Reportes` en el navbar.

### RF-02 — Filtros reactivos

El dashboard soporta los filtros:

- `startDate`: fecha inicial del rango
- `endDate`: fecha final del rango
- `categoryId`: categoría opcional

Cuando cambia cualquiera de ellos, se reenvía la solicitud y el contenido se actualiza sin recargar la página.

### RF-03 — KPI visuales

El dashboard presenta tarjetas con los indicadores clave del rango seleccionado, incluyendo al menos:

- ventas totales
- pedidos
- ticket promedio
- unidades vendidas
- stock bajo

### RF-04 — Visualización gráfica

Debe renderizar gráficos en Chart.js con estructura visual clara y consistencia por rol. La vista del vendedor organiza los gráficos en dos columnas, mientras la vista del administrador conserva su estructura propia.

## 4. API REST

### 4.1 Ruta base

`/api/v1/dashboard`

### 4.2 Endpoints

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/api/v1/dashboard/overview` | `ADMINISTRADOR`, `VENDEDOR` | Resumen ejecutivo del período. |
| `GET` | `/api/v1/dashboard/top-viewed-products` | `ADMINISTRADOR`, `VENDEDOR` | Productos más consultados. |
| `GET` | `/api/v1/dashboard/top-sold-products` | `ADMINISTRADOR`, `VENDEDOR` | Productos más vendidos. |

### 4.3 Parámetros

- `startDate` (opcional): fecha ISO `yyyy-MM-dd`
- `endDate` (opcional): fecha ISO `yyyy-MM-dd`
- `categoryId` (opcional): identificador de categoría

### 4.4 Respuesta principal

```json
{
  "totalVentas": 2450000.00,
  "totalPedidos": 327,
  "ticketPromedio": 7492.35,
  "productosVendidos": 1840,
  "margenBruto": 620000.00,
  "stockTotal": 5200,
  "stockBajo": 188,
  "ventasPorCategoria": [
    { "label": "Buzos", "value": 850000.00, "cantidad": 126 },
    { "label": "Camisetas", "value": 620000.00, "cantidad": 311 }
  ],
  "demandaPorTalla": [
    { "label": "M", "value": 274, "cantidad": 274 },
    { "label": "L", "value": 231, "cantidad": 231 }
  ],
  "productosPopulares": [
    { "label": "Buzo infantil azul", "value": 420, "cantidad": 420 },
    { "label": "Conjunto navideño", "value": 310, "cantidad": 310 }
  ],
  "busquedasMasFrecuentes": [
    { "label": "buzo niño", "value": 198, "cantidad": 198 },
    { "label": "conjunto invierno", "value": 140, "cantidad": 140 }
  ]
}
```

## 5. Modelo de datos y agregación

### DTOs principales

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
    List<DashboardSerieResponse> busquedasMasFrecuentes
) {}
```

```java
public record DashboardSerieResponse(
    String label,
    BigDecimal value,
    Long cantidad
) {}
```

La lógica de agregación debe respetar:

- sumar solo registros en el rango definido
- agrupar por categoría, talla o producto
- ordenar descendentemente por relevancia
- devolver listas vacías sin fallos cuando no hay datos

## 6. Seguridad

Los endpoints deben estar protegidos con autenticación y autorización:

- `ADMINISTRADOR` puede consultar la visión global.
- `VENDEDOR` puede consultar su propio contexto operativo.
- `CLIENTE` queda bloqueado y no debe ver el dashboard ni el item de navegación.

## 7. Frontend Angular

### 7.1 Componente

El componente principal del dashboard filtra y renderiza la vista según el rol autenticado.

### 7.2 Reglas de UI

- La vista de administrador se conserva sin mezclarse con la de vendedor.
- La vista de vendedor usa KPI superiores y dos gráficos en dos columnas.
- El componente renderiza los gráficos solo cuando el DOM y los datos están listos.
- Se evita la reutilización de canvas entre roles.

## 8. Casos de prueba mínimos esperados

- `CP-01` — acceso denegado para `CLIENTE`
- `CP-02` — carga del dashboard como `ADMINISTRADOR` o `VENDEDOR`
- `CP-03` — filtros por fecha/categoría actualizan la vista
- `CP-04` — pruebas unitarias backend JUnit / Mockito
- `CP-05` — pruebas unitarias frontend Vitest

## 9. Criterios de aceptación

- [x] La vista `Reportes` solo aparece para roles autorizados.
- [x] La vista del vendedor visualiza contenido diferente al administrador.
- [x] Los gráficos renderizan sin vaciarse al cambiar de vista o filtro.
- [x] Los datos se obtienen por endpoint REST con parámetros correctos.
- [x] La compilación y pruebas relevantes del frontend y backend no presentan errores de integración.

## 10. Alcance de entrega

La historia US-15 involucra exclusivamente la funcionalidad de dashboard visual, permisos por rol y la validación técnica del módulo. No incluye cambios operativos ajenos al panel analítico ni modificaciones de negocio no relacionadas con reportes.

````
This is the description of what the code block changes:
<changeDescription>
Agregar la documentación funcional de US-15 siguiendo el mismo esquema y nivel de detalle que US-14.
</changeDescription>

This is the code block that represents the suggested code change:
```
# US-15 — Descripción funcional

## 1. Objetivo

La historia de usuario US-15 define un dashboard visual para consultar métricas operativas del negocio con foco en ventas, demanda y gestión del inventario. Debe permitir a administradores y vendedores observar indicadores clave agregados por rango de fechas y categoría, con representación gráfica clara y acceso restringido por rol.

### Objetivos del módulo

- Centralizar métricas clave de operación en una vista única.
- Permitir filtros reactivos por `startDate`, `endDate` y `categoryId`.
- Diferenciar la UI por rol (`ADMINISTRADOR` y `VENDEDOR`).
- Exponer una API REST estable para soporte frontend.
- Mantener consistencia visual y evitación de duplicación de contenido por rol.

## 2. Alcance funcional por rol

### 2.1 Administrador

La vista del administrador debe mostrar un resumen global del negocio con KPIs y gráficos orientados a la operación general.

Incluye:

- ventas totales
- pedidos procesados
- ticket promedio
- stock bajo
- más consultados
- más vendidos
- baja rotación
- demanda por talla
- ventas por categoría

### 2.2 Vendedor

La vista del vendedor debe mantener una experiencia visual distinta, enfocada en su catálogo y su desempeño comercial.

Incluye:

- ventas totales del vendedor
- unidades vendidas
- ticket promedio
- ventas por período
- demanda por talla
- métricas operativas del negocio del vendedor

La vista es distinta y no reutiliza el contenido del dashboard administrativo.

## 3. Requisitos funcionales

### RF-01 — Acceso autorizado

El usuario autenticado con rol `ADMINISTRADOR` o `VENDEDOR` puede acceder al dashboard. `CLIENTE` queda fuera del alcance y no puede visualizar la opción `Reportes` en el navbar.

### RF-02 — Filtros reactivos

El dashboard soporta los filtros:

- `startDate`: fecha inicial del rango
- `endDate`: fecha final del rango
- `categoryId`: categoría opcional

Cuando cambia cualquiera de ellos, se reenvía la solicitud y el contenido se actualiza sin recargar la página.

### RF-03 — KPI visuales

El dashboard presenta tarjetas con los indicadores clave del rango seleccionado, incluyendo al menos:

- ventas totales
- pedidos
- ticket promedio
- unidades vendidas
- stock bajo

### RF-04 — Visualización gráfica

Debe renderizar gráficos en Chart.js con estructura visual clara y consistencia por rol. La vista del vendedor organiza los gráficos en dos columnas, mientras la vista del administrador conserva su estructura propia.

## 4. API REST

### 4.1 Ruta base

`/api/v1/dashboard`

### 4.2 Endpoints

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/api/v1/dashboard/overview` | `ADMINISTRADOR`, `VENDEDOR` | Resumen ejecutivo del período. |
| `GET` | `/api/v1/dashboard/top-viewed-products` | `ADMINISTRADOR`, `VENDEDOR` | Productos más consultados. |
| `GET` | `/api/v1/dashboard/top-sold-products` | `ADMINISTRADOR`, `VENDEDOR` | Productos más vendidos. |

### 4.3 Parámetros

- `startDate` (opcional): fecha ISO `yyyy-MM-dd`
- `endDate` (opcional): fecha ISO `yyyy-MM-dd`
- `categoryId` (opcional): identificador de categoría

### 4.4 Respuesta principal

```json
{
  "totalVentas": 2450000.00,
  "totalPedidos": 327,
  "ticketPromedio": 7492.35,
  "productosVendidos": 1840,
  "margenBruto": 620000.00,
  "stockTotal": 5200,
  "stockBajo": 188,
  "ventasPorCategoria": [
    { "label": "Buzos", "value": 850000.00, "cantidad": 126 },
    { "label": "Camisetas", "value": 620000.00, "cantidad": 311 }
  ],
  "demandaPorTalla": [
    { "label": "M", "value": 274, "cantidad": 274 },
    { "label": "L", "value": 231, "cantidad": 231 }
  ],
  "productosPopulares": [
    { "label": "Buzo infantil azul", "value": 420, "cantidad": 420 },
    { "label": "Conjunto navideño", "value": 310, "cantidad": 310 }
  ],
  "busquedasMasFrecuentes": [
    { "label": "buzo niño", "value": 198, "cantidad": 198 },
    { "label": "conjunto invierno", "value": 140, "cantidad": 140 }
  ]
}
```

## 5. Modelo de datos y agregación

### DTOs principales

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
    List<DashboardSerieResponse> busquedasMasFrecuentes
) {}
```

```java
public record DashboardSerieResponse(
    String label,
    BigDecimal value,
    Long cantidad
) {}
```

La lógica de agregación debe respetar:

- sumar solo registros en el rango definido
- agrupar por categoría, talla o producto
- ordenar descendentemente por relevancia
- devolver listas vacías sin fallos cuando no hay datos

## 6. Seguridad

Los endpoints deben estar protegidos con autenticación y autorización:

- `ADMINISTRADOR` puede consultar la visión global.
- `VENDEDOR` puede consultar su propio contexto operativo.
- `CLIENTE` queda bloqueado y no debe ver el dashboard ni el item de navegación.

## 7. Frontend Angular

### 7.1 Componente

El componente principal del dashboard filtra y renderiza la vista según el rol autenticado.

### 7.2 Reglas de UI

- La vista de administrador se conserva sin mezclarse con la de vendedor.
- La vista de vendedor usa KPI superiores y dos gráficos en dos columnas.
- El componente renderiza los gráficos solo cuando el DOM y los datos están listos.
- Se evita la reutilización de canvas entre roles.

## 8. Casos de prueba mínimos esperados

- `CP-01` — acceso denegado para `CLIENTE`
- `CP-02` — carga del dashboard como `ADMINISTRADOR` o `VENDEDOR`
- `CP-03` — filtros por fecha/categoría actualizan la vista
- `CP-04` — pruebas unitarias backend JUnit / Mockito
- `CP-05` — pruebas unitarias frontend Vitest

## 9. Criterios de aceptación

- [x] La vista `Reportes` solo aparece para roles autorizados.
- [x] La vista del vendedor visualiza contenido diferente al administrador.
- [x] Los gráficos renderizan sin vaciarse al cambiar de vista o filtro.
- [x] Los datos se obtienen por endpoint REST con parámetros correctos.
- [x] La compilación y pruebas relevantes del frontend y backend no presentan errores de integración.

## 10. Alcance de entrega

La historia US-15 involucra exclusivamente la funcionalidad de dashboard visual, permisos por rol y la validación técnica del módulo. No incluye cambios operativos ajenos al panel analítico ni modificaciones de negocio no relacionadas con reportes.

````
This is the description of what the code block changes:
<changeDescription>
Agregar la documentación funcional de US-15 siguiendo el mismo esquema y nivel de detalle que US-14.
</changeDescription>

This is the code block that represents the suggested code change:
```
# US-15 — Descripción funcional

## 1. Objetivo

La historia de usuario US-15 define un dashboard visual para consultar métricas operativas del negocio con foco en ventas, demanda y gestión del inventario. Debe permitir a administradores y vendedores observar indicadores clave agregados por rango de fechas y categoría, con representación gráfica clara y acceso restringido por rol.

### Objetivos del módulo

- Centralizar métricas clave de operación en una vista única.
- Permitir filtros reactivos por `startDate`, `endDate` y `categoryId`.
- Diferenciar la UI por rol (`ADMINISTRADOR` y `VENDEDOR`).
- Exponer una API REST estable para soporte frontend.
- Mantener consistencia visual y evitación de duplicación de contenido por rol.

## 2. Alcance funcional por rol

### 2.1 Administrador

La vista del administrador debe mostrar un resumen global del negocio con KPIs y gráficos orientados a la operación general.

Incluye:

- ventas totales
- pedidos procesados
- ticket promedio
- stock bajo
- más consultados
- más vendidos
- baja rotación
- demanda por talla
- ventas por categoría

### 2.2 Vendedor

La vista del vendedor debe mantener una experiencia visual distinta, enfocada en su catálogo y su desempeño comercial.

Incluye:

- ventas totales del vendedor
- unidades vendidas
- ticket promedio
- ventas por período
- demanda por talla
- métricas operativas del negocio del vendedor

La vista es distinta y no reutiliza el contenido del dashboard administrativo.

## 3. Requisitos funcionales

### RF-01 — Acceso autorizado

El usuario autenticado con rol `ADMINISTRADOR` o `VENDEDOR` puede acceder al dashboard. `CLIENTE` queda fuera del alcance y no puede visualizar la opción `Reportes` en el navbar.

### RF-02 — Filtros reactivos

El dashboard soporta los filtros:

- `startDate`: fecha inicial del rango
- `endDate`: fecha final del rango
- `categoryId`: categoría opcional

Cuando cambia cualquiera de ellos, se reenvía la solicitud y el contenido se actualiza sin recargar la página.

### RF-03 — KPI visuales

El dashboard presenta tarjetas con los indicadores clave del rango seleccionado, incluyendo al menos:

- ventas totales
- pedidos
- ticket promedio
- unidades vendidas
- stock bajo

### RF-04 — Visualización gráfica

Debe renderizar gráficos en Chart.js con estructura visual clara y consistencia por rol. La vista del vendedor organiza los gráficos en dos columnas, mientras la vista del administrador conserva su estructura propia.

## 4. API REST

### 4.1 Ruta base

`/api/v1/dashboard`

### 4.2 Endpoints

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/api/v1/dashboard/overview` | `ADMINISTRADOR`, `VENDEDOR` | Resumen ejecutivo del período. |
| `GET` | `/api/v1/dashboard/top-viewed-products` | `ADMINISTRADOR`, `VENDEDOR` | Productos más consultados. |
| `GET` | `/api/v1/dashboard/top-sold-products` | `ADMINISTRADOR`, `VENDEDOR` | Productos más vendidos. |

### 4.3 Parámetros

- `startDate` (opcional): fecha ISO `yyyy-MM-dd`
- `endDate` (opcional): fecha ISO `yyyy-MM-dd`
- `categoryId` (opcional): identificador de categoría

### 4.4 Respuesta principal

```json
{
  "totalVentas": 2450000.00,
  "totalPedidos": 327,
  "ticketPromedio": 7492.35,
  "productosVendidos": 1840,
  "margenBruto": 620000.00,
  "stockTotal": 5200,
  "stockBajo": 188,
  "ventasPorCategoria": [
    { "label": "Buzos", "value": 850000.00, "cantidad": 126 },
    { "label": "Camisetas", "value": 620000.00, "cantidad": 311 }
  ],
  "demandaPorTalla": [
    { "label": "M", "value": 274, "cantidad": 274 },
    { "label": "L", "value": 231, "cantidad": 231 }
  ],
  "productosPopulares": [
    { "label": "Buzo infantil azul", "value": 420, "cantidad": 420 },
    { "label": "Conjunto navideño", "value": 310, "cantidad": 310 }
  ],
  "busquedasMasFrecuentes": [
    { "label": "buzo niño", "value": 198, "cantidad": 198 },
    { "label": "conjunto invierno", "value": 140, "cantidad": 140 }
  ]
}
```

## 5. Modelo de datos y agregación

### DTOs principales

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
    List<DashboardSerieResponse> busquedasMasFrecuentes
) {}
```

```java
public record DashboardSerieResponse(
    String label,
    BigDecimal value,
    Long cantidad
) {}
```

La lógica de agregación debe respetar:

- sumar solo registros en el rango definido
- agrupar por categoría, talla o producto
- ordenar descendentemente por relevancia
- devolver listas vacías sin fallos cuando no hay datos

## 6. Seguridad

Los endpoints deben estar protegidos con autenticación y autorización:

- `ADMINISTRADOR` puede consultar la visión global.
- `VENDEDOR` puede consultar su propio contexto operativo.
- `CLIENTE` queda bloqueado y no debe ver el dashboard ni el item de navegación.

## 7. Frontend Angular

### 7.1 Componente

El componente principal del dashboard filtra y renderiza la vista según el rol autenticado.

### 7.2 Reglas de UI

- La vista de administrador se conserva sin mezclarse con la de vendedor.
- La vista de vendedor usa KPI superiores y dos gráficos en dos columnas.
- El componente renderiza los gráficos solo cuando el DOM y los datos están listos.
- Se evita la reutilización de canvas entre roles.

## 8. Casos de prueba mínimos esperados

- `CP-01` — acceso denegado para `CLIENTE`
- `CP-02` — carga del dashboard como `ADMINISTRADOR` o `VENDEDOR`
- `CP-03` — filtros por fecha/categoría actualizan la vista
- `CP-04` — pruebas unitarias backend JUnit / Mockito
- `CP-05` — pruebas unitarias frontend Vitest

## 9. Criterios de aceptación

- [x] La vista `Reportes` solo aparece para roles autorizados.
- [x] La vista del vendedor visualiza contenido diferente al administrador.
- [x] Los gráficos renderizan sin vaciarse al cambiar de vista o filtro.
- [x] Los datos se obtienen por endpoint REST con parámetros correctos.
- [x] La compilación y pruebas relevantes del frontend y backend no presentan errores de integración.

## 10. Alcance de entrega

La historia US-15 involucra exclusivamente la funcionalidad de dashboard visual, permisos por rol y la validación técnica del módulo. No incluye cambios operativos ajenos al panel analítico ni modificaciones de negocio no relacionadas con reportes.

````
This is the description of what the code block changes:
<changeDescription>
Agregar la documentación funcional de US-15 siguiendo el mismo esquema y nivel de detalle que US-14.
</changeDescription>

This is the code block that represents the suggested code change:
```
# US-15 — Descripción funcional

## 1. Objetivo

La historia de usuario US-15 define un dashboard visual para consultar métricas operativas del negocio con foco en ventas, demanda y gestión del inventario. Debe permitir a administradores y vendedores observar indicadores clave agregados por rango de fechas y categoría, con representación gráfica clara y acceso restringido por rol.

### Objetivos del módulo

- Centralizar métricas clave de operación en una vista única.
- Permitir filtros reactivos por `startDate`, `endDate` y `categoryId`.
- Diferenciar la UI por rol (`ADMINISTRADOR` y `VENDEDOR`).
- Exponer una API REST estable para soporte frontend.
- Mantener consistencia visual y evitación de duplicación de contenido por rol.

## 2. Alcance funcional por rol

### 2.1 Administrador

La vista del administrador debe mostrar un resumen global del negocio con KPIs y gráficos orientados a la operación general.

Incluye:

- ventas totales
- pedidos procesados
- ticket promedio
- stock bajo
- más consultados
- más vendidos
- baja rotación
- demanda por talla
- ventas por categoría

### 2.2 Vendedor

La vista del vendedor debe mantener una experiencia visual distinta, enfocada en su catálogo y su desempeño comercial.

Incluye:

- ventas totales del vendedor
- unidades vendidas
- ticket promedio
- ventas por período
- demanda por talla
- métricas operativas del negocio del vendedor

La vista es distinta y no reutiliza el contenido del dashboard administrativo.

## 3. Requisitos funcionales

### RF-01 — Acceso autorizado

El usuario autenticado con rol `ADMINISTRADOR` o `VENDEDOR` puede acceder al dashboard. `CLIENTE` queda fuera del alcance y no puede visualizar la opción `Reportes` en el navbar.

### RF-02 — Filtros reactivos

El dashboard soporta los filtros:

- `startDate`: fecha inicial del rango
- `endDate`: fecha final del rango
- `categoryId`: categoría opcional

Cuando cambia cualquiera de ellos, se reenvía la solicitud y el contenido se actualiza sin recargar la página.

### RF-03 — KPI visuales

El dashboard presenta tarjetas con los indicadores clave del rango seleccionado, incluyendo al menos:

- ventas totales
- pedidos
- ticket promedio
- unidades vendidas
- stock bajo

### RF-04 — Visualización gráfica

Debe renderizar gráficos en Chart.js con estructura visual clara y consistencia por rol. La vista del vendedor organiza los gráficos en dos columnas, mientras la vista del administrador conserva su estructura propia.

## 4. API REST

### 4.1 Ruta base

`/api/v1/dashboard`

### 4.2 Endpoints

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/api/v1/dashboard/overview` | `ADMINISTRADOR`, `VENDEDOR` | Resumen ejecutivo del período. |
| `GET` | `/api/v1/dashboard/top-viewed-products` | `ADMINISTRADOR`, `VENDEDOR` | Productos más consultados. |
| `GET` | `/api/v1/dashboard/top-sold-products` | `ADMINISTRADOR`, `VENDEDOR` | Productos más vendidos. |

### 4.3 Parámetros

- `startDate` (opcional): fecha ISO `yyyy-MM-dd`
- `endDate` (opcional): fecha ISO `yyyy-MM-dd`
- `categoryId` (opcional): identificador de categoría

### 4.4 Respuesta principal

```json
{
  "totalVentas": 2450000.00,
  "totalPedidos": 327,
  "ticketPromedio": 7492.35,
  "productosVendidos": 1840,
  "margenBruto": 620000.00,
  "stockTotal": 5200,
  "stockBajo": 188,
  "ventasPorCategoria": [
    { "label": "Buzos", "value": 850000.00, "cantidad": 126 },
    { "label": "Camisetas", "value": 620000.00, "cantidad": 311 }
  ],
  "demandaPorTalla": [
    { "label": "M", "value": 274, "cantidad": 274 },
    { "label": "L", "value": 231, "cantidad": 231 }
  ],
  "productosPopulares": [
    { "label": "Buzo infantil azul", "value": 420, "cantidad": 420 },
    { "label": "Conjunto navideño", "value": 310, "cantidad": 310 }
  ],
  "busquedasMasFrecuentes": [
    { "label": "buzo niño", "value": 198, "cantidad": 198 },
    { "label": "conjunto invierno", "value": 140, "cantidad": 140 }
  ]
}
```

## 5. Modelo de datos y agregación

### DTOs principales

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
    List<DashboardSerieResponse> busquedasMasFrecuentes
) {}
```

```java
public record DashboardSerieResponse(
    String label,
    BigDecimal value,
    Long cantidad
) {}
```

La lógica de agregación debe respetar:

- sumar solo registros en el rango definido
- agrupar por categoría, talla o producto
- ordenar descendentemente por relevancia
- devolver listas vacías sin fallos cuando no hay datos

## 6. Seguridad

Los endpoints deben estar protegidos con autenticación y autorización:

- `ADMINISTRADOR` puede consultar la visión global.
- `VENDEDOR` puede consultar su propio contexto operativo.
- `CLIENTE` queda bloqueado y no debe ver el dashboard ni el item de navegación.

## 7. Frontend Angular

### 7.1 Componente

El componente principal del dashboard filtra y renderiza la vista según el rol autenticado.

### 7.2 Reglas de UI

- La vista de administrador se conserva sin mezclarse con la de vendedor.
- La vista de vendedor usa KPI superiores y dos gráficos en dos columnas.
- El componente renderiza los gráficos solo cuando el DOM y los datos están listos.
- Se evita la reutilización de canvas entre roles.

## 8. Casos de prueba mínimos esperados

- `CP-01` — acceso denegado para `CLIENTE`
- `CP-02` — carga del dashboard como `ADMINISTRADOR` o `VENDEDOR`
- `CP-03` — filtros por fecha/categoría actualizan la vista
- `CP-04` — pruebas unitarias backend JUnit / Mockito
- `CP-05` — pruebas unitarias frontend Vitest

## 9. Criterios de aceptación

- [x] La vista `Reportes` solo aparece para roles autorizados.
- [x] La vista del vendedor visualiza contenido diferente al administrador.
- [x] Los gráficos renderizan sin vaciarse al cambiar de vista o filtro.
- [x] Los datos se obtienen por endpoint REST con parámetros correctos.
- [x] La compilación y pruebas relevantes del frontend y backend no presentan errores de integración.

## 10. Alcance de entrega

La historia US-15 involucra exclusivamente la funcionalidad de dashboard visual, permisos por rol y la validación técnica del módulo. No incluye cambios operativos ajenos al panel analítico ni modificaciones de negocio no relacionadas con reportes.

```
