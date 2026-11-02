# Descripción funcional — US-16

## 1. Objetivo

Exportar los reportes administrativos de **ventas, demanda, inventario y búsquedas** en formatos
interoperables, de modo que administradores y vendedores puedan analizarlos fuera de LittleStyle.
La historia cierra además el requisito **RNF-24**: más del 98 % de las solicitudes de exportación
deben finalizar correctamente y ese porcentaje debe poder auditarse.

## 2. Flujo de una exportación

```text
Angular (ExportarReporte)
  → GET /api/v1/reportes/{tipo}?formato=...&startDate=...&endDate=...&categoria=...
    → ReporteService (alcance desde el JWT)
      → ConstructorDatosReporte (misma fuente para PDF y hojas de cálculo)
      → GeneradorReporteFactory → CreadorGeneradorX → GeneradorReporteX
    → ReporteController (adjunto, longitud, nosniff, no-store)
  ← Blob
Angular entrega el archivo (descarga o impresión) y envía POST /telemetria
```

El controlador y el servicio **no contienen `switch` por formato**. El formato se resuelve una sola
vez en la fábrica; agregar uno nuevo exige un valor de enum, un creador y un generador, sin tocar
el flujo existente.

## 3. Factory Method

| Pieza | Responsabilidad |
|---|---|
| `FormatoReporte` | Valores `PDF`, `XLSX`, `CSV`; extensión y `MediaType`. `desde(...)` acepta el nombre o la extensión, sin distinguir mayúsculas. |
| `CreadorGeneradorReporte` | Contrato del creador: `formato()` y `crear()`. |
| `CreadorGeneradorPdf` / `Xlsx` / `Csv` | Instancian su generador concreto. |
| `GeneradorReporte` | Contrato `formato()` y `generar(ReporteDatos)`. |
| `GeneradorReporteFactory` | Indexa los creadores en el arranque; falla si hay dos para el mismo formato y devuelve `400` si no existe. |

La fábrica vive en `src/main/java/co/edu/uniquindio/littlestyle/modules/reportes/factory/`.

## 4. Datos de cada reporte

`ConstructorDatosReporte` produce **dos vistas de la misma información**:

- `filas`: mapas con valores ya formateados (`"COP $ 1.234"`, `"dd/MM/yyyy"`) para las plantillas
  JRXML del PDF.
- `tabla`: columnas tipadas (`TEXTO`, `ENTERO`, `MONEDA`, `FECHA`) y valores crudos para XLSX/CSV.

| Reporte | Fuente | Contenido |
|---|---|---|
| `VENTAS` | `DashboardAnalyticsService.obtenerResumen` + `obtenerVentasPorDia` | Indicadores (ventas, pedidos, ticket), serie diaria con nivel de barra, ventas por prenda y totales como parámetros. |
| `DEMANDA` | `DashboardAnalyticsService.obtenerResumen` | Unidades y valor por prenda y talla. No confundir con búsquedas: aquí se mide lo vendido. |
| `INVENTARIO` | `ReporteInventarioRepository` | Foto actual de prendas activas: prenda, tienda, categoría, talla, stock, mínimo y nivel. Ignora las fechas. |
| `BUSQUEDAS` | `ReporteBusquedaRepository` | Término, número de búsquedas, búsquedas sin resultados y última fecha; totales como parámetros. |

Parámetros comunes del PDF: `TITULO`, `SUBTITULO`, `ALCANCE`, `GENERADO_EN`, `GENERADO_POR` y
`TOTAL_REGISTROS`. Ventas agrega `TOTAL_VENTAS`, `TOTAL_PEDIDOS` y `TICKET_PROMEDIO`; inventario
agrega `TOTAL_UNIDADES` y `TOTAL_BAJOS`; búsquedas agrega `TOTAL_BUSQUEDAS` y
`TOTAL_SIN_RESULTADOS`.

## 5. Contrato de la API

### `GET /api/v1/reportes/{tipo}`

| Parámetro | Obligatorio | Valores |
|---|---|---|
| `tipo` (path) | Sí | `ventas`, `inventario`, `demanda`, `busquedas` |
| `formato` | No (PDF) | `PDF`, `XLSX`, `CSV` o sus extensiones |
| `startDate` | No | `yyyy-MM-dd`; por defecto hace 30 días |
| `endDate` | No | `yyyy-MM-dd`; por defecto hoy |
| `categoria` | No | Nombre del enum `CategoriaPrenda` |

Respuesta correcta: `200` con el archivo como adjunto. Cabeceras:

- `Content-Type` del formato (`application/pdf`,
  `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `text/csv;charset=UTF-8`).
- `Content-Disposition: attachment; filename*=UTF-8''...`.
- `Content-Length` para que Angular muestre progreso real. CORS lo expone junto con
  `Content-Disposition` (`Access-Control-Expose-Headers`), y Caddy no comprime
  `/api/v1/reportes/*`: al comprimir se pierde `Content-Length` y la barra quedaría indeterminada.
- `X-Content-Type-Options: nosniff` y `Cache-Control: no-store`.

Errores:

| Código | Causa |
|---|---|
| `400` | Tipo, formato, fecha o categoría inválidos; rango de fechas invertido |
| `401` / `403` | Sin autenticación o rol distinto de administrador/vendedor |
| `422` | El reporte supera `MAX_FILAS` (5 000) |
| `429` | Demasiados eventos de telemetría del mismo usuario en un minuto |
| `500` | Fallo interno de generación (sin exponer detalles) |

### `POST /api/v1/reportes/telemetria`

Cuerpo validado:

```json
{ "tipo": "VENTAS", "formato": "XLSX", "resultado": "ok", "motivo": null }
```

`resultado` admite `ok`, `error_red`, `blob_vacio`, `error_cliente` y `cancelado` (minúsculas). Un
valor desconocido produce `400`. Responde `202 Accepted`; `motivo` admite hasta 300 caracteres. El
cliente lo envía sin esperar la respuesta: un fallo de red al reportar nunca cambia el resultado de
la descarga.

### `GET /api/v1/reportes/metricas/exito`

Solo `ADMINISTRADOR`:

```json
{
  "ok": 120,
  "error": 1,
  "rechazado": 4,
  "tasaExito": 0.9917,
  "estado": "CUMPLE",
  "cumpleRnf24": true,
  "desde": "2026-10-31T10:00:00Z"
}
```

`cumpleRnf24` es `tasaExito > 0.98` (estricto) y siempre coincide con `estado == "CUMPLE"`. Sin
solicitudes, `estado` es `SIN_DATOS`, `tasaExito` es `null` y `cumpleRnf24` es `false`. `desde`
permite distinguir «no hubo solicitudes» de «el proceso acaba de arrancar». El detalle de contadores,
fórmulas y consultas de Actuator está en [`metricas-rnf24.md`](metricas-rnf24.md).

## 6. Registro de búsquedas

El reporte de búsquedas necesita una fuente que antes no existía. Se creó la tabla
`busqueda_catalogo`:

| Columna | Tipo | Nota |
|---|---|---|
| `id` | BIGINT | PK |
| `termino_normalizado` | VARCHAR(200) | minúsculas, sin tildes, espacios colapsados; clave de agrupación |
| `termino_original` | VARCHAR(200) | texto tal como lo escribió el usuario |
| `categoria` | VARCHAR(30) | enum `CategoriaPrenda` o `NULL` |
| `resultados_count` | INT | 0 identifica una búsqueda sin resultados |
| `usuario_id` | BIGINT | opcional; nunca se guarda nombre ni correo |
| `creado_en` | DATETIME | índice por fecha |

Índices: `termino_normalizado`, `creado_en` y `categoria`.

`RegistradorBusquedaCatalogo` escribe de forma **asíncrona** y captura cualquier fallo: un problema
al registrar no puede romper la búsqueda del cliente. `CatalogoClienteService.explorar` lo invoca
también cuando el resultado es vacío, porque esa es la señal de demanda insatisfecha más valiosa.

## 7. Límite de filas

`MAX_FILAS = 5 000`.

- Ventas/demanda/búsquedas: se valida al construir las filas.
- Búsquedas e inventario: se cuenta primero y se lee con `Pageable` de `MAX_FILAS + 1`, de modo que
  un crecimiento concurrente entre el conteo y la lectura también se rechaza.
- Al superar el límite se responde `422` con un mensaje que invita a filtrar por categoría o
  período. Así se protege la memoria del servidor y el tiempo de respuesta.

## 8. Medición del RNF-24

Se usan **dos contadores independientes** porque responden preguntas distintas y no deben sumarse
por accidente. El detalle completo está en [`metricas-rnf24.md`](metricas-rnf24.md).

| Contador | Origen | Valores de `resultado` | Pregunta |
|---|---|---|---|
| `reporte_exportaciones_total` | Servidor | `ok`, `rechazado`, `error` | ¿El backend generó y entregó el archivo? |
| `reporte_exportaciones_cliente_total` | Navegador | `ok`, `error_red`, `blob_vacio`, `error_cliente`, `cancelado` | ¿El usuario pudo abrir, guardar o imprimir lo recibido? |

Cálculo del servidor (el que se compara con el requisito):

```text
totalServidor = ok + error
tasa          = totalServidor == 0 ? 1.0 : ok / totalServidor
cumple        = totalServidor == 0 || tasa > 0.98
```

- Los `rechazado` (filtros inválidos `4xx`) se cuentan pero **no** entran en la tasa: son errores del
  usuario, no fallos del sistema. Los `error` (`5xx` o excepción) sí restan.
- La tasa de punta a punta usa el contador del cliente:
  `ok / (ok + error_red + blob_vacio + error_cliente)`. La cancelación queda fuera.
- El temporizador `reporte.exportacion.duracion` mide el tiempo de generación por formato.
- La telemetría está limitada a **30 eventos por usuario y minuto** para que un cliente con un
  error de red no pueda inflar el contador.

Evidencia operativa:

- `GET /api/v1/reportes/metricas/exito` (solo administrador) con `desde`.
- `GET /actuator/metrics` (solo administrador) con las series `reporte.exportaciones`,
  `reporte.exportaciones.cliente` y `reporte.exportacion.duracion`.
- Log `reporte.exportacion resultado=OK|RECHAZADO|ERROR tipo=... formato=... filas=... bytes=... ms=... usuarioId=...`.

## 9. Decisiones y supuestos

1. **Atributos de JasperReports 7.** El proyecto usa JR 7.0.6. Los atributos XML son *bean names*
   de Jackson, no las constantes `isXxx`: `<style name="base" default="true">` y
   `<group ... reprintHeaderOnEachPage="true">`. Con `isDefault` o `isReprintHeaderOnEachPage` la
   compilación falla con `UnrecognizedPropertyException`. Se verificó compilando y llenando las
   cuatro plantillas con un programa de prueba.
   `style` como atributo de referencia sí existe; `<printWhenExpression>` debe seguir siendo un
   elemento hijo.
2. **Barras de ventas sin módulo de gráficos.** Para no depender de JFreeChart/Java 25 se repite el
   carácter de bloque `U+2588` tantas veces como indique `nivelBarra` (0–10), calculado en
   `ConstructorDatosReporte`. Un único `textField` reemplaza los diez rectángulos con
   `printWhenExpression`: el resultado sigue siendo proporcional, liviano y mucho más fácil de
   mantener. Se comprobó que el glifo existe en la DejaVu Sans embebida y que el texto se extrae
   del PDF.
3. **Pie “Página X de Y”.** Se implementó con dos campos de texto adyacentes: uno con
   `$V{PAGE_NUMBER}` y otro con `evaluationTime="Report"` para el total. JasperReports no ofrece una
   variable única “actual de total” en esta versión.
4. **Alcance del vendedor en búsquedas.** No se persisten los ids de las prendas devueltas por cada
   búsqueda (hasta 5 000 por evento). Por eso el vendedor solo ve búsquedas **sin resultados** cuya
   categoría él vende: es la señal accionable de demanda insatisfecha. El administrador ve el
   consolidado completo.
5. **Métrica de punta a punta.** El servidor no puede saber si un PDF con `200` se abrió mal en el
   equipo del usuario; por eso el navegador valida la firma del blob y reporta el desenlace real en
   su propio contador. El umbral del requisito es estrictamente `> 0.98`: un `49/50 = 0,98` no
   cumple. Sin evidencia (`ok + error == 0`) el estado es `SIN_DATOS`, la tasa es `null` y no se
   declara cumplimiento.
6. **CSV para Excel en español.** BOM UTF-8 (tildes y eñes), separador `;`, fin de línea CRLF y
   escape RFC 4180. Los valores viajan crudos (decimales con punto, fechas ISO, sin símbolo de
   moneda) para que sean interoperables; el formato “COP $” queda solo en el PDF.
7. **Inyección de fórmulas.** En XLSX y CSV, todo texto (valores, encabezados y títulos) que empiece
   por `=`, `+`, `-`, `@`, tabulador (`\t`) o retorno de carro (`\r`) se neutraliza con un apóstrofo
   antes de escribirlo, siguiendo la guía OWASP «CSV Injection». Las columnas numéricas
   (`ENTERO`, `MONEDA`, `FECHA`) se escriben crudas y tipadas para no romper las sumas.
8. **Inventario sin fechas.** Es una foto del estado actual; los parámetros de fecha se ignoran a
   propósito y el nombre del archivo usa la fecha de generación.
9. **Límite de 5 000 filas.** No viene de la historia: se fijó para proteger memoria y tiempos de
   respuesta. Está documentado aquí y en el mensaje de error para que el usuario sepa cómo
   reducir el reporte.
10. **Tabla de búsquedas nueva.** El repositorio no tenía histórico de búsquedas. Se agregó
    `busqueda_catalogo` con datos anónimos; la escritura es asíncrona y tolerante a fallos.

## 10. Riesgos y pendientes

- El filtro por vendedor de `ReporteBusquedaRepository` usa JPQL con `exists` sobre `Prenda`.
  `ReporteBusquedaRepositoryTest` valida la consulta global, el filtro por categoría, el rango de
  fechas y el caso de un vendedor sin categorías sobre H2. Falta una prueba con prendas reales de un
  vendedor y con MySQL en producción.
- La telemetría y la métrica viven en memoria del proceso: se reinician con cada despliegue. Para
  histórico entre despliegues hay que persistir los contadores o conectar un registro externo.
- El límite de 30 telemetrías por minuto es por instancia; con varias réplicas el límite efectivo se
  multiplica.
- La impresión usa primero un iframe oculto; si no carga en 3 segundos o el navegador no permite
  imprimirlo, cae en la pestaña de respaldo abierta dentro del clic. Algunos navegadores todavía
  pueden exigir confirmación manual para imprimir el iframe.
