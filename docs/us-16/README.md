# US-16 — Exportación interoperable de reportes (RNF-24)

Como administrador o vendedor, quiero descargar los reportes de ventas, demanda, inventario y
búsquedas en un formato que pueda abrir y procesar en herramientas externas (Excel, LibreOffice,
visores de PDF o scripts), para analizar la operación fuera de la plataforma sin depender de la
interfaz web.

La historia cubre los formatos **PDF, XLSX y CSV**, el reporte de **búsquedas** del catálogo, el
límite de filas, la plantilla propia de cada reporte y la **medición auditable del RNF-24**: más del
98 % de las solicitudes de exportación deben finalizar correctamente.

## 1. Descripción general

### Alcance funcional

- Exportación de cuatro reportes: `ventas`, `demanda`, `inventario` y `busquedas`.
- Tres formatos por reporte: `PDF` (presentación), `XLSX` (análisis con tipos numéricos) y `CSV`
  (intercambio universal).
- Filtros de período (`startDate`, `endDate`) y categoría (`categoria`); el inventario es una foto
  actual e ignora las fechas.
- El alcance de los datos sale del JWT, nunca de un parámetro: el administrador ve el consolidado y
  el vendedor solo su tienda.
- Registro anónimo de búsquedas del catálogo (incluidas las que no arrojan resultados) para
  alimentar el reporte de búsquedas.
- Límite de **5 000 filas** por reporte, validado antes de cargar los datos en memoria.
- El inventario se exporta **por variante**: una fila por combinación `prenda + talla + color`, con
  el color visible en PDF, XLSX y CSV. La demanda, en cambio, se agrupa por talla porque mide
  unidades vendidas por tamaño.
- Medición del RNF-24 con contadores del servidor, telemetría del cliente, endpoint de consulta y
  exposición en Actuator.

### Restricción por rol

| Rol | Acceso a exportar | Alcance | Observación |
|---|---|---|---|
| `CLIENTE` | Denegado | — | No ve el panel de reportes ni consume la API |
| `VENDEDOR` | Permitido | Su tienda | Inventario/ventas/demanda propios; en búsquedas solo ve términos sin resultados de categorías que vende |
| `ADMINISTRADOR` | Permitido | Global | Consolidado de toda la plataforma y consulta de la métrica RNF-24 |

### Endpoints API REST

- `GET /api/v1/reportes/{tipo}`
  - `tipo`: `ventas`, `inventario`, `demanda` o `busquedas`.
  - Query: `formato` (`PDF` por defecto, `XLSX`, `CSV`; también acepta la extensión en minúsculas),
    `startDate`, `endDate` (`yyyy-MM-dd`) y `categoria` (nombre del enum `CategoriaPrenda`).
  - Respuesta: archivo binario como adjunto, con `Content-Type` del formato,
    `Content-Disposition`, `Content-Length` para la barra de progreso,
    `X-Content-Type-Options: nosniff` y `Cache-Control: no-store`. CORS expone
    `Content-Disposition` y `Content-Length` al frontend, y Caddy no comprime `/api/v1/reportes/*`
    para que `Content-Length` sobreviva.
  - Errores: `400` por tipo, formato, fecha o categoría inválidos; `401/403` por rol;
    `422` cuando el reporte supera 5 000 filas; `500` si falla la generación.
- `POST /api/v1/reportes/telemetria`
  - Cuerpo: `{ "tipo": "...", "formato": "...", "resultado": "ok|error_red|blob_vacio|error_cliente|cancelado", "motivo": "..." }`.
  - Responde `202 Accepted`; es *fire and forget* desde el navegador.
  - Límite de 30 eventos por usuario y minuto (`429` al excederlo).
- `GET /api/v1/reportes/metricas/exito`
  - Solo `ADMINISTRADOR`. Devuelve `ok`, `error`, `rechazado`, `tasaExito` (0–1 o `null`),
    `estado` (`CUMPLE`, `NO_CUMPLE`, `SIN_DATOS`), `cumpleRnf24` y `desde` (arranque del proceso).
    El umbral es estrictamente `> 0.98`. Sin solicitudes el estado es `SIN_DATOS` y no se declara
    cumplimiento.
- `GET /actuator/metrics`
  - Solo `ADMINISTRADOR` (salvo `/actuator/health`, que es público). Las series se consultan por
    nombre: `reporte.exportaciones` (servidor), `reporte.exportaciones.cliente` (navegador) y
    `reporte.exportacion.duracion`. Los valores de `resultado` son `ok`, `rechazado`, `error`,
    `error_red`, `blob_vacio`, `error_cliente` y `cancelado` según el contador.

## 2. Documentación técnica

| Archivo | Contenido |
|---|---|
| `descripcion-funcional.md` | Arquitectura, Factory Method, datos de cada reporte, decisiones y validaciones. |
| `casos-de-prueba.md` | Casos CP-01 a CP-10 con evidencia automatizada y manual. |
| `metricas-rnf24.md` | Contadores, fórmulas, consultas de Actuator, contrato del endpoint y decisiones de medición. |
| `migracion-busquedas.sql` | DDL de `busqueda_catalogo` para MySQL/MariaDB en producción. |
| `consultas-reportes.sql` | Consultas SQL de verificación de los datos que alimentan cada reporte. |
| `guia-usuario-reportes.md` | Instrucciones operativas para administrador y vendedor. |

## 3. Resumen de implementación

### Backend — exportación

- `src/main/java/.../reportes/controller/ReporteController.java`
  - expone exportación, telemetría y métrica de éxito; no decide por formato.
- `src/main/java/.../reportes/service/ReporteService.java`
  - orquesta datos + fábrica, registra log `reporte.exportacion` y alimenta la métrica.
- `src/main/java/.../reportes/service/ConstructorDatosReporte.java`
  - construye las filas del PDF y la tabla tipada de XLSX/CSV; valida el límite de filas.
- `src/main/java/.../reportes/service/ReporteMetricas.java`
  - dos contadores separados (servidor y cliente), límite de eventos y resumen RNF-24 con umbral
    estricto.

### Backend — Factory Method

Cada formato es un valor de `FormatoReporte` con su `CreadorGeneradorX` y su `GeneradorReporteX`:

| Formato | Creador | Generador | Salida |
|---|---|---|---|
| `PDF` | `CreadorGeneradorPdf` | `GeneradorReportePdf` | JasperReports con plantilla por reporte |
| `XLSX` | `CreadorGeneradorXlsx` | `GeneradorReporteXlsx` | Apache POI, hojas `Datos` y `Resumen` |
| `CSV` | `CreadorGeneradorCsv` | `GeneradorReporteCsv` | UTF-8 con BOM, separador `;`, CRLF, RFC 4180 |

- `src/main/java/.../reportes/factory/PlantillasReporte.java`
  - compila una plantilla por tipo en el arranque: `ventas.jrxml`, `demanda.jrxml`,
    `busquedas.jrxml` e `inventario.jrxml`.

### Backend — datos y búsquedas

- `src/main/java/.../catalogocompras/catalogo/model/BusquedaCatalogo.java`
  - tabla `busqueda_catalogo`: término normalizado y original, categoría, número de resultados,
    usuario opcional y fecha.
- `RegistradorBusquedaCatalogo` + `CatalogoClienteService`
  - registran la búsqueda de forma asíncrona y tolerante a fallos, incluso con cero resultados.
- `ReporteBusquedaRepository` y `ReporteInventarioRepository`
  - agregaciones de solo lectura; el inventario cuenta y pagina por variante
    (`prenda + talla + color`) antes de leer, y el repositorio selecciona `t.color` para que el
    color llegue al PDF, al XLSX y al CSV.
- `DashboardAnalyticsService.obtenerVentasPorDia`
  - serie diaria reutilizada por el reporte de ventas.

### Frontend

- `frontend/src/app/features/reportes/components/exportar-reporte.ts`
  - selector de reporte y formato, descarga, progreso, cancelación, impresión solo para PDF y
    telemetría del resultado.
- `frontend/src/app/features/reportes/services/reportes.service.ts`
  - petición con `responseType: 'blob'`, validación de la firma real del archivo (`%PDF-` / `PK` /
    texto con BOM), nombre desde `Content-Disposition`, entrega del archivo y reporte *fire and
    forget*.
- `frontend/src/app/features/reportes/models/reporte.models.ts`
  - tipos y opciones de reporte/formato compartidos.
- `frontend/src/app/features/dashboard/pages/dashboard-analytics/dashboard-analytics.ts`
  - integra el componente de exportación en el panel de reportes.

### Pruebas

- Backend: `ReporteControllerTest`, `ReporteControllerSecurityTest`, `ReporteServiceTest`,
  `ReporteMetricasTest`, `ConstructorDatosReporteTest`, `GeneradorReporteFactoryTest`,
  `GeneradorReportePdfTest`, `GeneradorReporteXlsxTest`, `GeneradorReporteCsvTest`,
  `RegistradorBusquedaCatalogoTest`, `NormalizadorBusquedaTest`, `ReporteBusquedaRepositoryTest`
  y `CatalogoClienteServiceTest`.
- Frontend: `exportar-reporte.spec.ts` y `reportes.service.spec.ts`.

Comandos de verificación:

```bash
./mvnw test
cd frontend && npm test -- --watch=false
```

Resultado de la última ejecución: **521 pruebas backend sin fallos** y **54 archivos / 289 pruebas
frontend aprobadas**.
