# Guía de usuario — Exportación de reportes

Esta guía explica cómo descargar e imprimir los reportes de LittleStyle y cómo interpretar los
mensajes más comunes. Está dirigida a administradores y vendedores.

## 1. Dónde se exportan los reportes

1. Inicia sesión como **administrador** o **vendedor**.
2. Abre **Reportes** en el menú (panel `/dashboard`).
3. En el bloque **Exportar reporte**, elige el tipo de reporte y el formato.
4. Si quieres acotar la información, ajusta el rango de fechas y la categoría en el panel antes de
   exportar.
5. Pulsa **Descargar** o **Imprimir**.

> El alcance no se elige en la pantalla: lo determina tu sesión. Un vendedor solo exporta su tienda;
> un administrador exporta el consolidado de la plataforma.

## 2. Tipos de reporte

| Reporte | Qué muestra | Usa fechas |
|---|---|---|
| **Ventas** | Ventas totales, pedidos, ticket promedio, serie diaria y ventas por prenda | Sí |
| **Demanda** | Unidades y valor vendidos por prenda y talla | Sí |
| **Inventario** | Foto actual de stock por prenda, tienda, categoría y talla, con nivel y stock mínimo | No |
| **Búsquedas** | Términos buscados, veces buscados, búsquedas sin resultados y última fecha | Sí |

El reporte de **búsquedas** mide qué busca la gente; el de **demanda** mide qué se vendió. No son
intercambiables: usa búsquedas para detectar productos que la gente busca y no encuentra, y demanda
para entender tallas y prendas más vendidas.

## 3. Formatos disponibles

| Formato | Para qué sirve | Qué esperar |
|---|---|---|
| **PDF** | Presentar, compartir o imprimir | Documento paginado con título, alcance, autor, fecha y pie “Página X de Y” |
| **Excel (.xlsx)** | Analizar, filtrar y sumar | Dos hojas: `Datos` (tabla con tipos reales) y `Resumen` (título, alcance, autor, fecha y totales) |
| **CSV (.csv)** | Importar en otra herramienta o script | Texto UTF-8 con BOM, separador `;`, fechas ISO y números sin símbolo de moneda |

Consejos de interoperabilidad:

- En Excel o LibreOffice, los montos del XLSX se pueden sumar directamente porque son números, no
  texto.
- El CSV está preparado para Excel en español: abre con doble clic y conserva tildes y eñes.
- Para scripts o bases de datos, usa el CSV: los decimales usan punto y las fechas el formato
  `AAAA-MM-DD`.
- El formato “COP $ 1.234” solo aparece en el PDF. En XLSX/CSV los valores van crudos para que no
  pierdan su tipo.

## 4. Imprimir

El botón **Imprimir** solo está disponible con formato **PDF**. Si eliges Excel o CSV, la aplicación
te lo indica y deshabilita el botón: esos formatos son para analizar, no para imprimir.

Al imprimir, LittleStyle intenta primero el diálogo desde un marco oculto con el PDF. Si el iframe no
carga en 3 segundos o el navegador no permite imprimirlo, usa la pestaña de respaldo que ya se abrió
al hacer clic. Si ninguna vía funciona, descarga el PDF y ábrelo manualmente. La pestaña y el marco se
cierran al terminar.

## 5. Filtros

- **Rango de fechas:** si no eliges fechas, se usan los últimos 30 días.
- **Categoría:** limita ventas, demanda y búsquedas a una categoría del catálogo. Inventario también
  la respeta.
- **Inventario:** ignora las fechas porque siempre muestra el estado actual del stock.

## 6. Mensajes y qué hacer

| Mensaje / situación | Causa | Qué hacer |
|---|---|---|
| “El reporte supera los 5.000 registros. Filtra por categoría o período.” | El resultado es demasiado grande | Acota las fechas o elige una categoría |
| “La fecha inicial no puede ser posterior a la final.” | Rango invertido | Corrige las fechas |
| “La categoría no es válida.” | Categoría inexistente | Elige una categoría de la lista |
| “No tienes permiso para exportar este reporte.” | Sesión sin rol autorizado o vencida | Inicia sesión como administrador o vendedor |
| “No pudimos generar el reporte. Inténtalo de nuevo en unos minutos.” | Fallo interno | Reintenta; si persiste, avisa al equipo técnico |
| “No hay conexión con el servidor.” | Red caída | Revisa la conexión y reintenta |
| “No pudimos guardar el archivo. Revisa los permisos de descarga.” | El navegador bloqueó la descarga | Permite descargas para el sitio o elige otra carpeta |
| “El archivo recibido no es un PDF válido.” (o Excel/CSV) | La respuesta llegó incompleta o una página de error ocupó el lugar del archivo | Reintenta; si persiste, avisa al equipo técnico |

## 7. Barra de progreso y cancelación

- La barra muestra el porcentaje real cuando el servidor informa el tamaño del archivo.
- Si el servidor o un proxy no lo informa, la barra se muestra indeterminada; la descarga continúa.
- **Cancelar** detiene la petición y cierra la pestaña de impresión si estaba abierta.

## 8. Para administradores: estado del RNF-24

La exportación de reportes debe completarse correctamente en más del 98 % de las solicitudes.
LittleStyle mide ese porcentaje con dos fuentes separadas:

- **Servidor:** `reporte_exportaciones_total` registra `ok`, `rechazado` y `error`.
- **Cliente:** `reporte_exportaciones_cliente_total` registra `ok`, `error_red`, `blob_vacio`,
  `error_cliente` y `cancelado`.

La tasa del requisito es `ok / (ok + error)` y solo cumple si es **estrictamente mayor que 0,98**.
Los rechazos por filtros inválidos y las cancelaciones del usuario quedan fuera del denominador.
Mientras no haya solicitudes registradas, el estado es `SIN_DATOS` y la tasa no se publica: no se
declara cumplimiento sin evidencia.

Consultas disponibles:

| Recurso | Acceso | Uso |
|---|---|---|
| `GET /api/v1/reportes/metricas/exito` | Administrador | `ok`, `error`, `rechazado`, `tasaExito`, `estado`, `cumpleRnf24` y `desde` |
| `GET /actuator/metrics` | Administrador | Series `reporte.exportaciones`, `reporte.exportaciones.cliente` y `reporte.exportacion.duracion` |
| Log `reporte.exportacion` | Equipo técnico | Detalle por intento: tipo, formato, filas, bytes, duración y usuario |

Detalle de fórmulas y verificación manual: [`metricas-rnf24.md`](metricas-rnf24.md).

> Los contadores viven en memoria del proceso. Si el servidor se reinicia, la medición vuelve a
> empezar; para histórico entre despliegues haría falta conectar un registro persistente.
