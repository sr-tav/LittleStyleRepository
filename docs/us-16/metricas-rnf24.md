# Métricas del RNF-24 — exportaciones de reportes (US-16)

Este documento fija **qué se mide**, **cómo se calcula** y **cómo se audita** el requisito
«más del 98 % de las solicitudes de exportación deben finalizar correctamente». Es la referencia
para el equipo y para la evidencia de calidad. La medición vive en el proceso y se consulta por
Actuator; no requiere infraestructura de monitoreo externa.

## 1. Regla y umbral

Una exportación es exitosa solo si el archivo se generó, viajó completo y el usuario pudo abrirlo o
guardarlo. El umbral es **estrictamente mayor que 0,98**:

```text
cumpleRnf24 = tasa > 0.98
```

Un `49 OK + 1 error = 0,98` **no cumple**. Hace falta un éxito adicional para superar el 98 %.

Sin solicitudes registradas no hay tasa que comparar: el resumen queda en estado `SIN_DATOS`,
`tasaExito` es `null` y `cumpleRnf24` es `false`. «No hay evidencia» nunca se reporta como «cumple».

## 2. Dos contadores, dos preguntas

| Contador (Actuator) | Etiquetas | Valores de `resultado` | Pregunta que responde |
|---|---|---|---|
| `reporte.exportaciones` | `tipo`, `formato`, `resultado` | `ok`, `rechazado`, `error` | ¿El servidor pudo generar y entregar el archivo? |
| `reporte.exportaciones.cliente` | `tipo`, `formato`, `resultado` | `ok`, `error_red`, `blob_vacio`, `error_cliente`, `cancelado` | ¿El navegador pudo abrir, guardar o imprimir lo que recibió? |
| `reporte.exportacion.duracion` | `tipo`, `formato` | — (temporizador) | ¿Cuánto tarda la generación en el servidor? |

En `/actuator/metrics` los nombres se consultan con puntos, tal como se registran en Micrometer:
`reporte.exportaciones`, `reporte.exportaciones.cliente` y `reporte.exportacion.duracion`.

### Semántica de cada resultado

| Valor | Quién lo emite | Significado | ¿Entra en la tasa? |
|---|---|---|---|
| `ok` (servidor) | Backend | Generó el archivo y respondió `200` | Sí, en el numerador |
| `error` | Backend | Excepción interna o `BusinessException` `5xx` | Sí, en el denominador |
| `rechazado` | Backend | Validación del solicitante (`4xx`: fechas, categoría, formato, demasiadas filas) | **No**: no es un fallo del sistema |
| `ok` (cliente) | Navegador | Validó la firma del archivo y lo entregó al navegador (o abrió la impresión) | Sí, en el numerador |
| `error_red` | Navegador | La petición nunca llegó al servidor (`status 0`) | Sí, en el denominador |
| `blob_vacio` | Navegador | El servidor respondió `200` pero con un archivo de 0 bytes (o un CSV que solo trae el BOM) | Sí, en el denominador |
| `error_cliente` | Navegador | `4xx/5xx`, archivo corrupto, permisos de descarga o fallo del navegador | Sí, en el denominador |
| `cancelado` | Navegador | El usuario pulsó «Cancelar» | **No**: decisión del usuario |

## 3. Fórmulas

Tasa del servidor (la que publica `GET /api/v1/reportes/metricas/exito` y la que se compara con el
umbral del requisito):

```text
tasaServidor = ok / (ok + error)
```

### Qué significa `ok` en el cliente

El navegador **no puede confirmar que el archivo se guardó en disco**: por seguridad no expone si el
usuario aceptó la descarga, eligió carpeta o canceló el diálogo del sistema. Antes de reportar `ok`,
el cliente comprueba que el blob recibido corresponde al formato pedido leyendo sus primeros bytes:

| Formato | Firma esperada |
|---|---|
| `PDF` | `%PDF-` (`25 50 44 46 2D`) |
| `XLSX` | `PK` (`50 4B`), porque todo OOXML es un ZIP |
| `CSV` | Texto no vacío que no empiece por `<`, `{` o `[`; se admite el BOM UTF-8 `EF BB BF` |

Si la firma no coincide, el archivo no se entrega y se reporta `error_cliente` (o `blob_vacio` si no
hay contenido). En CSV la comprobación es más laxa porque el formato es texto libre, pero se
descartan las páginas de error HTML/XML (`<`) y las respuestas JSON (`{`, `[`) que un proxy pudiera
servir con estado `200`. Por eso `ok` significa **«archivo íntegro validado y entregado al
navegador»**, no «guardado en disco». El campo `motivo` de la telemetría deja el detalle trazable
(`archivo_validado_entregado` para descarga, `archivo_validado_impreso` para impresión).

Tasa de punta a punta (la que responde «¿el usuario obtuvo su archivo?»):

```text
tasaCliente = ok / (ok + error_red + blob_vacio + error_cliente)
```

Si `ok + error == 0` la tasa del servidor no se calcula: se publica `tasaExito = null` con estado
`SIN_DATOS` y `cumpleRnf24 = false`. Es una foto sin evidencia, no un cumplimiento. En cuanto existe
al menos una solicitud, la tasa es `ok / (ok + error)` y el estado pasa a `CUMPLE` o `NO_CUMPLE`
según el umbral estricto. Los rechazos y las cancelaciones **nunca** se suman al denominador del
requisito.

## 4. Contrato del endpoint

`GET /api/v1/reportes/metricas/exito` — solo `ADMINISTRADOR`.

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

Sin solicitudes, la misma consulta responde:

```json
{
  "ok": 0,
  "error": 0,
  "rechazado": 0,
  "tasaExito": null,
  "estado": "SIN_DATOS",
  "cumpleRnf24": false,
  "desde": "2026-10-31T10:00:00Z"
}
```

- `estado` vale `CUMPLE`, `NO_CUMPLE` o `SIN_DATOS` y es la lectura directa para auditoría.
- `tasaExito` está redondeada a 4 decimales, vive entre `0` y `1` y es `null` cuando no hay
  evidencia (`SIN_DATOS`).
- `desde` es el instante en que arrancó el proceso que tiene los contadores.
- `cumpleRnf24` aplica `> 0.98` sobre la tasa sin redondear; siempre es coherente con
  `estado == "CUMPLE"`.
- Los contadores son **en memoria**: se reinician con cada despliegue.

## 5. Consulta operativa con Actuator

Actuator expone `health`, `info` y `metrics`. Las métricas se consultan por nombre:

```bash
# Todas las series del contador del servidor.
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/actuator/metrics/reporte.exportaciones"

# Solo los éxitos.
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/actuator/metrics/reporte.exportaciones?tag=resultado:ok"

# Un tipo y formato concretos.
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/actuator/metrics/reporte.exportaciones?tag=tipo:ventas&tag=formato:pdf"

# Telemetría del navegador: errores de red.
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/actuator/metrics/reporte.exportaciones.cliente?tag=resultado:error_red"

# Duración de generación.
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/actuator/metrics/reporte.exportacion.duracion"
```

La respuesta tiene la forma:

```json
{
  "name": "reporte.exportaciones",
  "measurements": [{ "statistic": "COUNT", "value": 121.0 }],
  "availableTags": [
    { "tag": "tipo", "values": ["ventas", "demanda", "inventario", "busquedas"] },
    { "tag": "formato", "values": ["pdf", "xlsx", "csv"] },
    { "tag": "resultado", "values": ["ok", "rechazado", "error"] }
  ]
}
```

Actuator entrega una **foto** de los contadores; no calcula tasas ni conserva histórico. Para la
tasa del requisito se usa `/metricas/exito`, que aplica la fórmula y el umbral.

## 6. Operación y seguridad

- `application.properties` expone `health,info,metrics` en Actuator.
- `/actuator/health` es público; el resto de endpoints de Actuator exige `ADMINISTRADOR`.
- La telemetría del cliente está limitada a **30 eventos por usuario y minuto**; al excederse
  responde `429` y **no** incrementa el contador. El mapa de ventanas se poda al superar 10 000
  usuarios.
- Un fallo al enviar telemetría es «fire and forget»: nunca rompe la descarga ni muestra un error al
  usuario.
- El cliente envía `resultado` en minúsculas (`ok`, `error_red`, `blob_vacio`, `error_cliente`,
  `cancelado`), igual que las etiquetas de Micrometer.
- Al reiniciar el proceso, los contadores vuelven a cero y `desde` cambia.

## 7. Evidencia automatizada

| Qué se prueba | Test |
|---|---|
| Umbral estricto 0,98 y tasas | `ReporteMetricasTest` |
| Rechazos `4xx` excluidos y errores `5xx` contados | `ReporteServiceTest` |
| Contrato HTTP, extensión de archivo y telemetría | `ReporteControllerTest` |
| Seguridad real: 401, 403, roles y `400` de validación | `ReporteControllerSecurityTest` |
| Clasificación de telemetría y cancelación en Angular | `exportar-reporte.spec.ts`, `reportes.service.spec.ts` |
| Consulta JPQL del reporte de búsquedas | `ReporteBusquedaRepositoryTest` |

Comandos:

```bash
./mvnw test
cd frontend && npm test -- --watch=false
```

## 8. Verificación manual

```bash
# 1. Autenticarse como administrador y guardar el token.
TOKEN="<jwt-de-administrador>"

# 2. Consultar la foto del proceso.
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/reportes/metricas/exito | jq

# 3. Ver el contador del servidor y el del cliente.
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/actuator/metrics/reporte.exportaciones?tag=resultado:ok" | jq
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/actuator/metrics/reporte.exportaciones.cliente" | jq

# 4. Generar una exportación y volver a consultar.
curl -s -o /tmp/ventas.xlsx -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/reportes/ventas?formato=XLSX"
```

## 9. Decisiones registradas

1. **Contadores separados**: un solo contador con etiqueta `origen` obligaba a filtrar en cada
   consulta y permitía sumar por error eventos del cliente con respuestas del servidor. Separarlos
   hace que la consulta del RNF-24 sea la consulta por defecto.
2. **Minúsculas**: el contrato de telemetría y las etiquetas de Micrometer usan el mismo vocabulario.
   Mezclar `OK` y `ok` crearía series duplicadas y consultas inconsistentes.
3. **Umbral estricto**: «más del 98 %» no es «98 % o más». El caso 49/50 = 0,98 se prueba
   explícitamente para que nadie lo «arregle» a `>=`.
4. **`4xx` fuera de la tasa**: una fecha inválida o una categoría inexistente es un error de uso, no
   una caída del sistema. Contarla castigaría la calidad del producto por entradas del usuario.
5. **Cancelación fuera de la tasa**: cancelar es una acción deliberada; contarla como fallo
   distorsionaría la métrica.
6. **`desde` en la respuesta**: sin ese dato, un contador reiniciado parecía una caída de volumen.
   Con `desde` se distingue «no hubo solicitudes» de «el proceso acaba de arrancar».
7. **`SIN_DATOS` explícito**: con cero solicitudes la tasa no es `1.0`. Un auditor no puede aceptar
   como evidencia un «cumple» que nadie ejercitó; el estado separa «sin datos» de «cumple» y la tasa
   nula obliga a quien consuma el endpoint a tratar el caso.
8. **`ok` del cliente no es «guardado en disco»**: el navegador no expone esa confirmación. Se
   valida la firma del blob antes de entregarlo y se documenta lo que realmente se mide: archivo
   íntegro entregado. Prometer más sería maquillar la métrica.
9. **Sin registro externo**: para el alcance de la US-16, Actuator y `/metricas/exito` cubren la
   medición y la evidencia. No se agrega una dependencia de monitoreo que nadie va a consultar. Si
   más adelante se necesita histórico entre despliegues, se conecta un `MeterRegistry` persistente
   y se actualiza este documento.
