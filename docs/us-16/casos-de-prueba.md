# Casos de prueba — US-16

## 1. Objetivo

Validar que la exportación de reportes cumple cuatro condiciones: (a) el archivo se genera en todos
los formatos y tipos, (b) los datos conservan tipos y codificación al abrirlo en herramientas
externas, (c) el acceso y el alcance respetan el rol, y (d) la tasa de éxito del RNF-24 es medible y
no se distorsiona con errores del usuario.

## 2. Casos de prueba principales

| ID | Precondición | Pasos | Resultado esperado                                                                                                                                                                                 | Tipo | Estado |
|---|---|---|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---|---|
| CP-01 | Backend arrancado con las cuatro plantillas JRXML compiladas | Exportar cada tipo en `PDF` con 0, 3 y 500 filas | PDF válido, texto extraíble, fuentes embebidas, varias páginas y pie «Página 1 de N» con N real                                                                                                    | Automatizada | Aprobado |
| CP-02 | Reporte con datos y una celda que empieza por `=` | Exportar en `XLSX` y abrir con Apache POI | Hojas `Datos` y `Resumen`, tipos numéricos/fecha, panel congelado, autofiltro y fórmulas neutralizadas                                                                                             | Automatizada | Aprobado |
| CP-03 | Textos con tildes, eñes, `;`, comillas y saltos | Exportar en `CSV` | BOM UTF-8, separador `;`, CRLF y escape RFC 4180. Para inventario, el CSV conserva la columna 'Color' y dos colores de una misma talla aparecen en filas diferentes, cada uno con su propio stock. | Automatizada | Aprobado |
| CP-04 | Fábrica con los tres formatos | Pedir cada formato y uno inexistente | Un creador y un generador por formato; sin `switch` en controller/service; `400` si no existe                                                                                                      | Automatizada | Aprobado |
| CP-05 | Sesiones anónima, `CLIENTE`, `VENDEDOR` y `ADMINISTRADOR` | Descargar, pedir tipo/formato inválido, enviar telemetría inválida y consultar métricas | Cabeceras de adjunto y `nosniff`/`no-store`; `401`, `403`, `400`; extensión por formato                                                                                                            | Automatizada | Aprobado |
| CP-06 | Vendedor con datos propios y administrador | Exportar los cuatro reportes con cada rol | Vendedor solo su tienda; administrador consolidado; sin alcance por query param                                                                                                                    | Automatizada | Aprobado |
| CP-07 | Más de 5 000 filas o crecimiento entre conteo y lectura | Exportar inventario/búsquedas | `422` antes de cargar los datos en memoria                                                                                                                                                         | Automatizada | Aprobado |
| CP-08 | Búsquedas con resultados, sin resultados y fallo del repositorio | Buscar en el catálogo | Se registra término normalizado/original, categoría, conteo y usuario; el fallo no rompe la búsqueda                                                                                               | Automatizada | Aprobado |
| CP-09 | Sin exportaciones, luego OK, error, rechazo y cancelación | Consultar `/metricas/exito` y Actuator | Dos contadores separados; rechazos y cancelaciones fuera de la tasa; `49/50 = 0,98` no cumple; `429` al exceder telemetría                                                                         | Automatizada | Aprobado |
| CP-10 | Panel de reportes en Angular | Descargar, imprimir, cancelar, simular error de red y popup bloqueado | Progreso, mensajes, impresión solo PDF, iframe primero y telemetría clasificada                                                                                                                    | Automatizada | Aprobado |
| CP-11 | Base H2 con búsquedas sembradas | Ejecutar la consulta JPQL global, por categoría, por fecha y por vendedor sin prendas | Agrupación por término, conteo de sin-resultados, filtros y alcance correctos                                                                                                                      | Automatizada | Aprobado |

## 3. Casos detallados

### CP-01 — PDF válido para los cuatro reportes

- Entrada: exportar cada tipo con `formato=PDF`, sin filas, con 3 filas y con 500 filas.
- Resultado esperado:
  - el archivo empieza con la firma `%PDF` y no está vacío;
  - las tildes y eñes se conservan;
  - el texto contiene el título del reporte, «Camisa niña ñandú» y «Categoría»;
  - con 500 filas hay más de una página y el pie dice «Página 1 de N» con el N real;
  - la barra de participación se dibuja repitiendo el bloque `█` según `nivelBarra` (0–10);
  - el inventario trae una fila por `prenda + talla + color` y una columna **Color** legible;
    dos colores de la misma talla no se colapsan en una sola fila;
  - todas las fuentes que usa el PDF están embebidas.
- Evidencia: `GeneradorReportePdfTest` (`@EnumSource(TipoReporte.class)`,
  `elTextoEsExtraibleYLasFuentesEstanEmbebidas` y
  `unReporteDe500FilasOcupaVariasPaginasYNumeraLaPrimera` con PDFBox).

### CP-02 — XLSX interoperable

- Entrada: exportar ventas, inventario o búsquedas con `formato=XLSX`.
- Resultado esperado:
  - el archivo es un ZIP OOXML válido con las hojas `Datos` y `Resumen`;
  - los encabezados provienen de `TablaReporte.columnas()`;
  - enteros y moneda son celdas numéricas (se pueden sumar), las fechas son fechas y solo los
    textos son texto;
  - la hoja `Resumen` replica título, subtítulo, alcance, autor, fecha y totales;
  - la hoja `Datos` tiene panel congelado en la fila de encabezados y autofiltro sobre el rango;
  - todo texto, encabezado o título que empieza por `=`, `+`, `-`, `@`, tabulador o retorno de carro
    se escribe neutralizado con apóstrofo;
  - los enteros y montos negativos siguen siendo celdas numéricas, sin apóstrofo;
  - el inventario conserva la columna **Color** y una fila por variante.
- Evidencia: `GeneradorReporteXlsxTest` (`laHojaDatosTienePanelCongeladoYAutofiltro`).

### CP-03 — CSV para Excel en configuración regional colombiana

- Entrada: exportar un reporte con tildes, eñes y un texto que contenga `;`.
- Resultado esperado:
  - BOM `EF BB BF` al inicio para que Excel respete la codificación;
  - separador `;` y fin de línea CRLF;
  - valores crudos: decimales con punto, fechas ISO, sin “COP $”;
  - campos con separador, comilla o salto de línea entre comillas dobles y comillas duplicadas
    (RFC 4180);
  - fórmulas neutralizadas también cuando el texto empieza por tabulador o retorno de carro;
  - un monto o entero negativo viaja sin apóstrofo para no perder el tipo.
- Evidencia: `GeneradorReporteCsvTest`.

### CP-04 — Factory Method sin `switch` por formato

- Entrada: pedir cada formato y pedir uno inexistente.
- Resultado esperado:
  - cada formato tiene exactamente un creador y un generador;
  - dos creadores para el mismo formato fallan al arrancar;
  - un formato sin creador o nulo devuelve `400`;
  - el nombre del archivo usa la extensión del formato.
- Evidencia: `GeneradorReporteFactoryTest`.

### CP-05 — Contrato HTTP y seguridad

- Entrada: descarga correcta; tipo o formato inválido; telemetría con resultado desconocido;
  usuario `CLIENTE`, `VENDEDOR`, `ADMINISTRADOR` y anónimo.
- Resultado esperado:
  - `200` con `Content-Disposition: attachment`, `Content-Length`,
    `X-Content-Type-Options: nosniff` y `Cache-Control: no-store`;
  - CORS expone `Content-Disposition` y `Content-Length` al frontend y Caddy no comprime
    `/api/v1/reportes/*` (comprimir elimina `Content-Length`);
  - `TipoMedia` correcto para PDF, XLSX y CSV y nombre de archivo con la extensión del formato;
  - `400` sin invocar la generación cuando tipo, formato o resultado de telemetría no existen;
  - `401` sin autenticación, `403` para `CLIENTE` y para la métrica consultada por no administrador;
  - telemetría delegada en `ReporteMetricas` y métrica de éxito solo para administrador.
- Evidencia: `ReporteControllerTest` (contrato y extensión), `ReporteControllerSecurityTest`
  (cadena real de Spring Security con `@WebMvcTest`) y `SecurityConfigCorsTest` (cabeceras
  expuestas).

### CP-06 — Alcance por rol

- Entrada: exportar con un vendedor autenticado y con un administrador.
- Resultado esperado:
  - el vendedor recibe datos filtrados por su `vendedorId`, sin poder pedir otro alcance por query
    param;
  - el administrador recibe el consolidado (`vendedorId == null`);
  - un cliente o anónimo se rechaza sin tocar datos;
  - un fallo de renderizado se convierte en `500` sin exponer la causa interna.
- Evidencia: `ReporteServiceTest`.

### CP-07 — Límite de filas y rechazo temprano

- Entrada: inventario o búsquedas con más de 5 000 filas; crecimiento entre el conteo y la lectura.
- Resultado esperado:
  - `422` con mensaje que invita a filtrar por categoría o período;
  - la consulta de datos no se ejecuta cuando el conteo ya supera el límite;
  - si el conjunto crece después del conteo, la lectura de `MAX_FILAS + 1` también lo detecta.
- Evidencia: `ConstructorDatosReporteTest`
  (`inventarioMuyGrandeSeRechazaAntesDeTraerLasFilas`,
  `inventarioQueCreceEntreElConteoYLaLecturaTambienSeRechaza`,
  `busquedasMuyGrandesSeRechazanAntesDeTraerLasFilas`).

### CP-08 — Registro de búsquedas del catálogo

- Entrada: búsqueda con resultados, búsqueda sin resultados y fallo del repositorio.
- Resultado esperado:
  - se guarda el término original y el normalizado (minúsculas, sin tildes, espacios colapsados);
  - se guarda la categoría, el número de resultados y el usuario si está autenticado;
  - las búsquedas sin resultados también se registran;
  - si el registro falla, `explorar` sigue respondiendo normalmente.
- Evidencia: `NormalizadorBusquedaTest`, `RegistradorBusquedaCatalogoTest`,
  `CatalogoClienteServiceTest`.

### CP-09 — Métrica de éxito RNF-24

- Entrada: combinaciones de exportaciones del servidor, telemetría del cliente, rechazos,
  cancelaciones y ráfagas de eventos.
- Resultado esperado:
  - sin evidencia, `estado = SIN_DATOS`, `tasaExito = null` y `cumpleRnf24 = false`;
  - `50 OK + 0 error` y `99 OK + 1 error` dan `estado = CUMPLE`;
  - los `rechazado` (`4xx`) no entran en el denominador; los `error` (`5xx`) sí;
  - `49/50 = 0,98` da `estado = NO_CUMPLE` y `cumpleRnf24 = false`; un éxito adicional los pone en
    `CUMPLE` y `true`;
  - la telemetría usa `reporte_exportaciones_cliente_total` y no altera la tasa del servidor;
  - la cancelación se cuenta pero queda fuera del denominador;
  - más de 30 eventos por usuario y minuto devuelve `429`;
  - el endpoint devuelve `ok`, `error`, `rechazado`, `tasaExito` (o `null`), `estado`,
    `cumpleRnf24` y `desde`.
- Evidencia: `ReporteMetricasTest`, `ReporteServiceTest`
  (`unErrorDeNegocioConEstado5xxSiRestaEnLaTasa`) y `ReporteControllerSecurityTest`.
- Detalle: [`metricas-rnf24.md`](metricas-rnf24.md).

### CP-10 — Experiencia de exportación en Angular

- Entrada: elegir tipo y formato, descargar, imprimir, cancelar, destruir el componente, simular
  error de red, archivo vacío y popup bloqueado.
- Resultado esperado:
  - se ofrecen los cuatro reportes y los tres formatos;
  - “Imprimir” se deshabilita fuera de PDF y se explica por qué;
  - los botones se deshabilitan mientras hay una exportación en curso;
  - se envía el formato elegido en el query param y se usan las fechas/categoría del dashboard;
  - el progreso es real con `Content-Length` e indeterminado sin él;
  - el nombre por defecto usa la extensión del formato;
  - antes de entregar el archivo se valida la firma del blob: `%PDF-` para PDF, `PK` para XLSX y
    texto no vacío que no empiece por `<`, `{` o `[` para CSV (con BOM opcional); un HTML o JSON de
    error disfrazado no se descarga y se reporta `error_cliente`;
  - `ok` se reporta solo tras validar y entregar el archivo, con motivo
    `archivo_validado_entregado` (o `archivo_validado_impreso` al imprimir); no afirma que se guardó
    en disco;
  - un `400` muestra el mensaje del backend y un `500` un mensaje genérico;
  - la telemetría se envía en éxito, error, red, archivo vacío y cancelación, con minúsculas en el
    cuerpo; un fallo al enviarla no rompe la descarga;
  - la impresión intenta primero el iframe, cae en la pestaña de respaldo y limpia la ventana;
  - cancelar y destruir liberan la suscripción y cierran la ventana de impresión.
- Evidencia: `exportar-reporte.spec.ts` (17 casos) y `reportes.service.spec.ts`.

### CP-11 — Consulta JPQL del reporte de búsquedas

- Entrada: base H2 con búsquedas de «Camisa» y «Vestido», una fuera del rango y una categoría
  distinta; consulta global, por categoría, por fecha y por un vendedor sin prendas.
- Resultado esperado:
  - agrupa por término normalizado y ordena por volumen;
  - cuenta las búsquedas sin resultados por término;
  - respeta `creadoEn >= inicio` y `creadoEn < fin`;
  - filtra por categoría sin perder la agrupación;
  - un vendedor sin prendas de la categoría no ve búsquedas.
- Evidencia: `ReporteBusquedaRepositoryTest` (`@DataJpaTest` + H2).

## 4. Pruebas manuales de interoperabilidad

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Exportar ventas en XLSX y abrirlo en Excel/LibreOffice | Dos hojas; montos sumables; filtros y panel congelado en `Datos` |
| 2 | Exportar búsquedas en CSV y abrirlo en Excel | Tildes y eñes correctas; una columna por campo; fechas reconocidas |
| 3 | Exportar inventario en PDF e imprimirlo | Una fila por prenda, talla y color, con la columna **Color**; encabezado repetido por página y pie “Página X de Y” |
| 4 | Exportar con un vendedor y con un administrador | El vendedor no ve datos de otras tiendas |
| 5 | Exportar sin resultados | El archivo se genera con una fila explicativa, sin error |
| 6 | Forzar más de 5 000 filas | Mensaje `422` que invita a filtrar |

## 5. Matriz de evidencia

| Capa | Técnica | Evidencia |
|---|---|---|
| Backend — generación | JUnit 5 + Apache POI + PDFBox | `GeneradorReporteXlsxTest`, `GeneradorReporteCsvTest`, `GeneradorReportePdfTest` |
| Backend — datos | JUnit 5 + Mockito + AssertJ | `ConstructorDatosReporteTest` |
| Backend — contrato | MockMvc / Security Test | `ReporteControllerTest`, `ReporteControllerSecurityTest` |
| Backend — métrica | Micrometer `SimpleMeterRegistry` | `ReporteMetricasTest`, `ReporteServiceTest` |
| Backend — repositorio | `@DataJpaTest` + H2 | `ReporteBusquedaRepositoryTest` |
| Frontend | Vitest + Angular Testing | `exportar-reporte.spec.ts`, `reportes.service.spec.ts` (firmas de blob) |
| Interoperabilidad | Excel, LibreOffice y visor de PDF | Pruebas manuales de la sección 4 |

## 6. Criterios de aceptación

- [x] Se exportan ventas, demanda, inventario y búsquedas en PDF, XLSX y CSV.
- [x] El PDF conserva el comportamiento anterior y numera todas las páginas.
- [x] XLSX y CSV conservan tipos, codificación y escape para herramientas externas.
- [x] El acceso y el alcance se resuelven por rol desde el JWT.
- [x] Los reportes que superan 5 000 filas se rechazan antes de cargarse en memoria.
- [x] Las búsquedas del catálogo se registran, incluidas las que no tienen resultados.
- [x] La tasa de éxito del RNF-24 es consultable y se expone en Actuator con umbral estricto
      `> 0.98` y contadores separados para servidor y cliente.
- [x] La impresión intenta primero el iframe y cae en una pestaña de respaldo sin depender del popup.
- [x] Las pruebas backend y frontend ejecutan sin fallos.
