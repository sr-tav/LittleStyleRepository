# Evidencias de métricas: Sprints 2 y 3 (procesos 1, 2 y 3)

Dónde se obtiene la evidencia de cada métrica de las secciones 3.3 y 3.4 del *Plan de Gestión de la
Calidad*, para registrarla en la sección 5.

Los datos de uso (perfiles, pedidos, pagos) salen de **producción**, así que conviene medir **después**
de las sesiones de prueba en TestRail: esas pruebas son las que crean los datos.

## Fuentes de evidencia

| Fuente | Cómo se accede | Métricas |
|--------|----------------|----------|
| **A. Base de datos (RDS)** | SSH a la EC2 + `docs/calidad/metricas.sql` | Adopción de tallas, completitud, actualización, cifrado, abandono, sincronización de stock, trazabilidad, alertas, pagos |
| **B. Logs del backend** | SSH a la EC2, en `~/littlestyle`: `docker compose logs backend`. ⚠ Solo guarda desde el último despliegue: cada build de Jenkins recrea el contenedor. Saca estas capturas antes de mergear algo nuevo. | Fallos del motor, correos, webhooks |
| **C. Jenkins** | https://littlestyle.duckdns.org/jenkins → `littlestyle` → *Test Result* y *Cobertura* | Pruebas automatizadas, cobertura, tiempo de reparación |
| **D. Pruebas de carga (Artillery)** | [`pruebas-carga/README.md`](../../pruebas-carga/README.md) → `informe.html` | Tiempos de respuesta, filtros, 100 usuarios simultáneos, CPU y RAM |
| **E. AWS** | Consola de AWS (CloudWatch, S3) | CPU, disponibilidad, imágenes |
| **F. Herramientas externas** | Wompi, Gmail, SonarCloud, GitHub, navegador | Firmas HMAC, correos, defectos, dependencias, navegadores |

### Cómo ejecutar las consultas (fuente A)

En la EC2:

```bash
cd ~/littlestyle && git pull
docker run --rm -i --env-file .env -v ~/littlestyle/docs/calidad:/q mysql:8.4 \
  sh -c 'mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASSWORD" littlestyle -t < /q/metricas.sql'
```

Muestra todas las tablas de resultados seguidas. Para una captura por métrica, abre la consola de MySQL y
pega una consulta a la vez:

```bash
docker run --rm -it --env-file .env mysql:8.4 sh -c 'mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASSWORD" littlestyle'
```

Cada consulta tiene una etiqueta (`[P1-A]`, `[P2-C]`…) que coincide con las tablas de abajo.

---

## Proceso 1: Perfil infantil y recomendación

### 3.3.1 Métricas de proceso

| Métrica | De dónde sale la evidencia | Captura |
|---------|----------------------------|---------|
| **Tiempo medio de respuesta del motor** | Artillery contra `GET /api/cliente/recomendaciones` (fuente D). Rápido: navegador → F12 → **Network** → abrir *Recomendaciones* → columna *Time* de la petición `recomendaciones`. | Reporte de Artillery (`http.response_time`: mean, p95) o pestaña Network |
| **Densidad de defectos del módulo de perfiles** | **SonarCloud** (gratis con GitHub si el repo es público; si no, SonarQube en Docker). Defectos = *Bugs* de la carpeta `modules/perfilesinfantiles`; KLOC = *Lines of Code* de esa carpeta. Otra opción: casos *Failed* de perfiles en TestRail ÷ KLOC. | Pantalla *Code* de Sonar filtrada por la carpeta |
| **Cumplimiento de cifrado (Habeas Data)** | Consulta **[P1-D]**: nombre, fecha de nacimiento, contextura, alergias, estatura y peso se ven ilegibles. En el código, `PerfilInfantil.java` y `MedicionCrecimiento.java`: los 14 campos sensibles tienen `@Convert(...Cifrado...)` (AES-GCM) → **14/14 = 100 %**. La prueba `CifradoPerfilConverterTest` en Jenkins lo respalda. | Resultado de la consulta + código con los `@Convert` |
| **Tiempo medio entre fallos del motor** | Logs (fuente B): `docker compose logs backend \| grep -c "Error no controlado en /api/cliente/recomendaciones"`. MTBF = horas en línea ÷ fallos. Con 0 fallos: "sin fallos en N horas". | Salida del comando |
| **Soporte de plataformas compatibles** | Abrir crear perfil y recomendaciones en Chrome, Firefox, Edge y en móvil (F12 → icono de celular). Lo registra el equipo de TestRail como un *Test Run* por navegador. % = navegadores sin errores ÷ total. | Capturas por navegador o el *Test Run* de TestRail |

### 3.4.1 Métricas de calidad

| Métrica | Meta | De dónde sale la evidencia |
|---------|------|----------------------------|
| **Adopción de recomendación de tallas** | > 80 % | Consulta **[P1-A]**. Compara la talla comprada (`pedido_items.talla`) con la que sugirió el motor (`talla_sugerida`). La recomendación sugiere talla, no color: `pedido_items.color` se conserva para auditoría pero no entra en la métrica. |
| **Completitud del perfil infantil** | > 90 % | Consulta **[P1-B]**. Los obligatorios no pueden quedar vacíos (la base lo impide), así que "completo" = tiene medición + colores + estampados. |
| **Tiempo de carga de recomendaciones** | < 2 s | Igual que el *tiempo medio de respuesta del motor* (Artillery o Network). |
| **Tasa de error en captura de medidas** | < 5 % | ⚠ El sistema no guarda los intentos fallidos. Opciones: (1) en las pruebas de usabilidad de TestRail, contar intentos con error ÷ intentos; (2) activar el log de accesos de Caddy y contar los `400` de `POST /api/cliente/perfiles`. |
| **Frecuencia de actualización de perfiles** | > 60 % | Consulta **[P1-C]**. Como el proyecto es nuevo, todos los perfiles tienen menos de 6 meses: anótalo como observación. |
| **Protección de datos sensibles** | > 96 % | Igual que *Cumplimiento de cifrado*: consulta **[P1-D]** + código → 100 %. |
| **Consultas de recomendación sin fallos** | ≥ 99 % | Artillery contra recomendaciones: `http.codes.200` ÷ `http.requests`. Respaldo: logs con 0 `Error no controlado` y la degradación controlada del motor (`MotorRecomendacionService`, RNF-29). |

---

## Proceso 2: Catálogo, compras, pedidos e inventario

### 3.3.2 Métricas de proceso

| Métrica | De dónde sale la evidencia | Captura |
|---------|----------------------------|---------|
| **Disponibilidad operativa** | **UptimeRobot** (gratis): monitor HTTPS a `https://littlestyle.duckdns.org` cada 5 min. Dejarlo 24–48 h antes de la entrega (~3–5 USD de créditos). Alternativa: EC2 → *Monitoring* → *Status check failed* en 0. Se miden solo las ventanas en que el servidor debía estar prendido. | Dashboard de UptimeRobot con el % |
| **Consumo de CPU y RAM** | Durante la prueba de carga: CPU en EC2 → *Monitoring* → *CPU utilization*; RAM con `docker stats --no-stream` en la EC2 (CloudWatch no mide RAM sin un agente). | Gráfica de CPU + salida de `docker stats` |
| **Cobertura de pruebas automatizadas** | Jenkins → `littlestyle` → **Cobertura**: % de líneas cubiertas por las pruebas (JaCoCo en el backend, Vitest en el frontend). Medido en local: backend 89.3 %, frontend 86.5 %, total 87.7 %. Detalle por paquete: *Build Artifacts* → `reportes/cobertura/backend/index.html` y `frontend/index.html`. | Gráfica y tabla de *Cobertura* + *Test Result Trend* |
| **Excepciones en integración con la pasarela** | Panel de Wompi *sandbox* → *Desarrolladores → Eventos*: cada evento enviado y la respuesta del servidor. Fallos en el backend: `docker compose logs backend \| grep -E "Evento de Wompi (rechazado\|descartado)\|No fue posible aplicar"`. % = fallidos ÷ eventos. | Lista de eventos de Wompi + salida del grep |
| **Tiempo medio de reparación ante caídas** | Historial de builds de Jenkins. Ejemplo real: el despliegue de US-12 falló por la tabla `despachos`, Jenkins hizo rollback automático y se corrigió con la migración. MTTR = tiempo desde el build fallido hasta el build verde. Gracias al rollback, la página no estuvo caída. | *Build History* con las horas del build rojo y del verde + consola del rollback |
| **Cobertura de pruebas E2E** | Fase 4 (Playwright): flujos automatizados exitosos ÷ flujos críticos de compra. Mientras no existan, los casos E2E ejecutados en TestRail. | Reporte de Playwright o *Test Run* de TestRail |

### 3.4.2 Métricas de calidad

| Métrica | Meta | De dónde sale la evidencia |
|---------|------|----------------------------|
| **Abandono del carrito** | < 30 % | Consulta **[P2-A]**: (carritos con productos sin comprar + checkouts sin pagar) ÷ (esos dos + compras pagadas). |
| **Sincronización de stock** | < 500 ms | Consulta **[P2-B]**: milisegundos entre la aprobación del pago y el cambio a `PAGADO`. El stock se descuenta en esa misma transacción (`PedidoPagoService`), por eso el tiempo es mínimo. |
| **Trazabilidad de estados** | > 98 % | Consulta **[P2-C]**: pedidos pagados con historial de estados y, si fueron enviados, con número de guía. |
| **Efectividad de filtros** | < 1.5 s | Artillery contra `GET /api/cliente/catalogo?...` con filtros. Rápido: pestaña Network al filtrar en *Explorar*. |
| **Precisión de alertas de reabastecimiento** | > 99 % | Consulta **[P2-D]**: de las variantes agotadas con mínimo definido (`prenda + talla + color`), cuántas tuvieron alerta antes. Stock y alertas corresponden a esa combinación; una talla con dos colores cuenta como dos variantes. Respaldo: log `Alerta de reabastecimiento ... emitida` y la pantalla de alertas del vendedor. |
| **Entrega de notificaciones** | > 98 % | Enviadas: carpeta **Enviados** del Gmail de `MAIL_USER`. Fallidas: `docker compose logs backend \| grep -c "No fue posible enviar"`. % = (enviadas − fallidas) ÷ enviadas. |
| **Disponibilidad de recursos multimedia** | > 99 % | Comando de la sección *Imágenes* (abajo): intenta abrir cada imagen de S3. % = respuestas `200` ÷ total. Respaldo: S3 → bucket → carpeta `catalogo/` con el número de objetos. |
| **Carga simultánea** | ≥ 99 % con 100 usuarios | Artillery con 100 usuarios virtuales sobre el catálogo: `http.codes.200` ÷ `http.requests` y `vusers.failed = 0`. |
| **Autenticación de notificaciones de pago (HMAC)** | ≥ 99.8 % | Válidas: eventos procesados en el panel de Wompi. Rechazadas: `docker compose logs backend \| grep -c "firma inválida"`. Respaldo: `FirmaWompiServiceTest` (11 pruebas) en Jenkins. |
| **Mantenimiento seguro de dependencias** | ≥ 98 % | Frontend: `cd frontend && npm audit`. Backend: `docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image littlestyle-backend`. También GitHub → *Security → Dependabot alerts* (se activa en *Settings*). % = dependencias sin vulnerabilidades ÷ total. |
| **Preservación transaccional ante fallos externos** | ≥ 99 % | Consulta **[P2-E]**: cada estado de pago corresponde a un estado de pedido coherente (`APROBADA` → pagado; `RECHAZADA` / `ERROR` → sin cobrar; `REEMBOLSO_PENDIENTE` → señalado). Respaldo: pruebas de reintentos y timeouts (`ReceptorEventosWompiTest`, `WompiClientTest`, `PagoServiceTest`). |

### Imágenes (disponibilidad multimedia)

En la EC2:

```bash
cd ~/littlestyle
docker run --rm --env-file .env mysql:8.4 sh -c 'mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASSWORD" littlestyle -N -e "SELECT url FROM prenda_imagenes"' 2>/dev/null \
  | while read -r u; do curl -s -o /dev/null -w "%{http_code}\n" "$u"; done | sort | uniq -c
```

Resultado esperado: una sola línea, `N 200`. Cualquier otro código es una imagen que no carga.

---

## Proceso 3: Reportes analíticos y exportación (US-16)

### 3.4.3 Métricas de calidad

#### RNF-24 — Tasa de éxito en la exportación de reportes (> 98 %)

La métrica se mide **de punta a punta**, no solo con el código HTTP del backend: un archivo que se
descarga con `200` pero no se puede abrir en el equipo del usuario también es un fallo de
interoperabilidad.

| Dato | De dónde sale | Cómo se captura |
|------|---------------|-----------------|
| Éxitos del servidor | `reporte.exportaciones{resultado="ok"}` | `GET /actuator/metrics/reporte.exportaciones?tag=resultado:ok` (administrador) o `GET /api/v1/reportes/metricas/exito` |
| Fallos del servidor | `reporte.exportaciones{resultado="error"}` | Igual que el anterior |
| Rechazos de validación | `reporte.exportaciones{resultado="rechazado"}` | Se registran, pero **no** entran en la tasa: son errores del usuario (filtros inválidos) |
| Éxitos/fallos del cliente | `reporte.exportaciones.cliente{resultado=ok\|error_red\|blob_vacio\|error_cliente\|cancelado}` alimentado por `POST /api/v1/reportes/telemetria` | El navegador lo envía al guardar, abrir, imprimir o cancelar; se consulta en `/actuator/metrics/reporte.exportaciones.cliente` |
| Duración de generación | `reporte.exportacion.duracion{tipo,formato}` | `GET /actuator/metrics/reporte.exportacion.duracion` (count, total y max; percentiles si se habilitan) |
| Detalle por intento | Log `reporte.exportacion resultado=OK\|RECHAZADO\|ERROR tipo=... formato=... filas=... bytes=... ms=... usuarioId=...` | `docker compose logs backend \| grep reporte.exportacion` |

Cálculo del requisito (tasa del servidor):

```text
total = count(resultado="ok") + count(resultado="error")
tasa  = total == 0 ? 1.0 : ok / total
cumpleRnf24 = total == 0 || tasa > 0.98
```

> El umbral es **estrictamente mayor** que 0,98. Un `49 OK + 1 error = 0,98` **no cumple**.
> Los `rechazado` y los `cancelado` quedan fuera del denominador.

`GET /api/v1/reportes/metricas/exito` devuelve la foto ya calculada:

```json
{
  "ok": 120,
  "error": 1,
  "rechazado": 4,
  "tasaExito": 0.9917,
  "cumpleRnf24": true,
  "desde": "2026-10-31T10:00:00Z"
}
```

Comandos para la captura en producción:

```bash
# 1. Tasa calculada (requiere token de administrador)
curl -s -H "Authorization: Bearer $TOKEN_ADMIN" \
  https://littlestyle.duckdns.org/api/v1/reportes/metricas/exito

# 2. Contadores del servidor y del cliente
curl -s -H "Authorization: Bearer $TOKEN_ADMIN" \
  "https://littlestyle.duckdns.org/actuator/metrics/reporte.exportaciones?tag=resultado:ok"
curl -s -H "Authorization: Bearer $TOKEN_ADMIN" \
  "https://littlestyle.duckdns.org/actuator/metrics/reporte.exportaciones.cliente"

# 3. Últimos intentos en el log
cd ~/littlestyle && docker compose logs backend \
  | grep 'reporte.exportacion' | tail -n 50
```

Prueba automatizada de la lógica: `ReporteMetricasTest` cubre sin evidencia, exportación exitosa,
rechazos excluidos, `49/50 = 0,98` sin cumplir, telemetría en su propio contador, cancelación y límite
de 30 eventos por usuario y minuto. `ReporteServiceTest` cubre que un `5xx` sí resta y un `4xx` no.
`ReporteControllerSecurityTest` valida el contrato HTTP real. Detalle:
[`docs/us-16/metricas-rnf24.md`](../us-16/metricas-rnf24.md).

> ⚠ Los contadores viven en memoria del proceso: se reinician con cada despliegue. Para conservar el
> histórico entre builds hay que **persistir los contadores o conectar un registro externo**
> (`MeterRegistry` remoto). Mientras no exista ese registro, la captura del endpoint es una foto del
> proceso actual.
>
> ⚠ El límite de telemetría es por instancia. Con varias réplicas, el límite efectivo se multiplica
> por el número de instancias.

Evidencia mínima recomendada para la sección 5:

1. Captura de `GET /api/v1/reportes/metricas/exito` después de una sesión de exportaciones.
2. Captura de `/actuator/metrics` con los contadores del servidor y del cliente por formato.
3. Captura del log con al menos una línea `OK` y una `ERROR`/`RECHAZADO`.
4. Resultado de `ReporteMetricasTest` en Jenkins.

---

## Pendientes para completar las evidencias

| Pendiente | Para qué métricas |
|-----------|-------------------|
| Ejecutar Artillery contra producción (`pruebas-carga/README.md`) | Tiempos del motor y filtros, carga simultánea, CPU y RAM, consultas sin fallos |
| SonarCloud o SonarQube | Densidad de defectos |
| UptimeRobot 24–48 h | Disponibilidad |
| Sesiones de TestRail en producción | Generan los datos de las consultas de la fuente A, plataformas compatibles y error de captura |
| Registro persistente de métricas | Histórico del RNF-24 entre despliegues (hoy los contadores viven en memoria) |