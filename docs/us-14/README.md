# US-14 — Gestión administrativa y moderación

Como administrador, supervisar cuentas y la calidad del catálogo; atender reportes y
apelaciones con trazabilidad de las acciones.

## Qué se implementó

| Subtarea | Implementación |
|---|---|
| Administración de cuentas | Bandeja paginada con filtros por rol, estado y texto; habilitar, suspender y bloquear con motivo obligatorio, reglas contra autosuspensión, administración de otras cuentas ADMINISTRADOR y cuentas ELIMINADO; eventos de correo tras commit. |
| Moderación posterior | El vendedor publica libremente y la prenda nace `ACTIVA`/`SIN_REVISAR`. La cola administrativa prioriza prendas sin revisar y reportes abiertos; permite aprobar, desactivar con motivo y restaurar. La desactivación oculta la prenda del catálogo, recomendaciones y nuevas compras sin cambiar su estado comercial. |
| Auditoría | Registros append-only en `auditoria_admin`, hash SHA-256 encadenado con bloqueo de escritura, consulta paginada, verificación de integridad, trigger MySQL contra UPDATE/DELETE y detección de alteración mediante hash. |
| Incidencias | Reportes de producto y solicitudes de soporte de clientes/vendedores; apelaciones de producto reservadas al vendedor dueño de una prenda desactivada; bandeja administrativa para tomar, resolver o rechazar con respuesta. Las suspensiones/bloqueos generan incidencia de apelación en forma automática. |
| Panel Angular | Dashboard KPI, administración de usuarios, moderación, auditoría de solo lectura y bandeja de incidencias; el vendedor ve el motivo y puede iniciar una apelación, y el cliente reporta desde la ficha del producto. |
| Pruebas | Unitarias JUnit/Mockito/AssertJ y escenarios de integración H2/MockMvc; pruebas Angular Vitest para servicios, páginas y flujos relevantes. |

## Archivos de esta documentación

| Archivo | Contenido |
|---|---|
| `descripcion-funcional.md` | Modelo, transiciones, decisiones, API, configuración, límites y diagramas. |
| `casos-de-prueba.md` | Casos CP-US14 y trazabilidad a la automatización. |
| `guia-usuario-panel-administrativo.md` | Instrucciones operativas para administrador, cliente y vendedor. |
| `auditoria-inmutable-mysql.sql` | Triggers MySQL que impiden modificar/eliminar filas de auditoría. |

## Evidencia de ejecución

Comandos ejecutados en Windows desde la raíz:

| Comando | Resultado |
|---|---|
| `.\mvnw.cmd test` | 404 pruebas backend, 0 fallos, 0 errores. |
| `cd frontend; npm ci` | Restauración desde el `package-lock.json`; no se modificaron manifiestos ni se agregaron dependencias. |
| `cd frontend; npm test -- --watch=false` | 246 pruebas Vitest, 0 fallos. |
| `cd frontend; npm run build` | Compilación Angular de producción correcta. |
| `git diff --check` | Sin errores de whitespace. |

`npm ci` reportó 14 alertas en las dependencias ya fijadas en el lockfile
(5 moderadas, 7 altas y 2 críticas). No se ejecutó `npm audit fix` ni se actualizaron
paquetes, porque no se añadieron dependencias y ese cambio excede esta fase.

La suite backend completa incluye `US14AdminIntegrationTest` (3 escenarios H2). La prueba
`persisteHashConPrecisionDeBaseDeDatosYAbreApelacionEnLaTransaccion` registra una
suspensión con MockMvc, fuerza `flush()`/`clear()` del `EntityManager` y verifica la
cadena tras recargar los registros; también comprueba la IP y la apelación automática.
Sus
aserciones ejercitan autenticación/autorización y validación, invalidación del token del
vendedor suspendido, visibilidad en catálogo y recomendaciones, prioridad de reportes,
motivo para el vendedor, bloqueo de reactivación, indisponibilidad en carrito/checkout y
auditoría.

## Definition of Done

- [x] Los estados de usuario incluyen `BLOQUEADO`; `ELIMINADO` no es operable por admin.
- [x] Moderación posterior: las nuevas prendas se publican activas y visibles en `SIN_REVISAR`; no hay cola previa.
- [x] `DESACTIVADA` no es visible ni comprable; vendedores no pueden modificar/reactivar; solo admin restaura.
- [x] Auditoría se guarda en la misma transacción de la acción exitosa, y registra intentos administrativos fallidos en transacción independiente.
- [x] Auditoría no expone operaciones de actualización/borrado en el repositorio y el script MySQL añade triggers de protección.
- [x] Incidencias de reporte, soporte, apelación de producto y apelación automática de suspensión tienen flujo de seguimiento.
- [x] Rutas administrativas y de usuario están restringidas por rol.
- [x] Frontend presenta estados de carga/vacío/error y ofrece controles accesibles en los diálogos implementados.
- [x] Suites backend y frontend, build y whitespace check pasaron.
- [ ] Flujo público autenticado por correo, con limitación de solicitudes, para apelar una cuenta suspendida/bloqueada: no implementado; requiere endpoint seguro sin sesión.
- [ ] Ejecución del trigger sobre una instancia MySQL real y validación operacional de las metas RNF: pendiente del despliegue y medición.

## Verificación de RNF

| RNF | Implementado | Evidencia y límite |
|---|---|---|
| RNF-08 — trazabilidad de acciones administrativas > 99,5 % | Acciones de cuenta, moderación y ciclo de vida de incidencia generan registro con admin, timestamp `Instant` normalizado a microsegundos, IP remota de la petición, rol, acción, entidad, valores, motivo, resultado y hashes. Los éxitos comparten transacción con su operación; los intentos fallidos se persisten con `REQUIRES_NEW`. | `AuditoriaServiceTest` comprueba hash determinista, serialización sin colisiones por delimitadores, integridad y detección de manipulación. `US14AdminIntegrationTest` recarga tras `flush/clear`, comprueba IP e integridad de la cadena. **El 99,5 % es un objetivo de disponibilidad/cobertura en producción, no se puede certificar solo con pruebas locales.** |
| RNF-17 — > 90 % de incidencias resueltas en menos de 24 h | Incidencias con estados, asignación a admin, tiempos de creación/cierre, respuesta y KPI de tasa de resolución en ventana de 30 días. | Pruebas de servicios y UI ejercitan operaciones y el dashboard consulta la tasa; `ResumenAdminService` calcula la métrica. **La meta > 90 % requiere medición de datos operacionales reales.** No hay SLA/alerta de vencimiento automatizado en esta fase. |

## Suposiciones y decisiones

- **Moderación posterior:** se mantiene la decisión de negocio: publicar libremente, revisar después; `SIN_REVISAR` no bloquea la venta.
- **Carrito/checkout:** una prenda o vendedor que deja de ser visible se muestra no disponible en el carrito y no se puede incluir en un checkout nuevo. No se cancelan ni reescriben pedidos ya pagados.
- **Apelación de cuenta:** al suspender/bloquear se crea una incidencia ligada al titular dentro de la misma transacción; si no puede guardarse, tampoco se confirma el cambio de estado. El login muestra un mensaje distinto para suspensión y bloqueo, e informa que la apelación ya fue abierta y la respuesta llegará por correo. A falta de endpoint de apelación público con verificación del correo, no se habilita una página de envío sin autenticación.
- **Protección MySQL:** un operador autorizado aplica `auditoria-inmutable-mysql.sql` después de que exista `auditoria_admin`; H2 no ejecuta esos triggers.
- **Restauración:** es una decisión administrativa y deja moderación `APROBADA`; no altera `EstadoPrenda`, inventario ni pedidos existentes.
- **Reportes:** el cliente puede reportar desde la ficha; el reporte se vincula a la prenda y eleva prioridad en la cola. Las apelaciones de producto son exclusivas del vendedor propietario y requieren prenda desactivada.

## Archivos creados o modificados en la implementación US-14

La lista completa de archivos versionados creados o modificados para las fases 1–5 se
mantiene al cierre en la tabla de `descripcion-funcional.md`, sección 10. Los archivos
que ya existían en el repositorio antes de estas fases no se presentan como creaciones.
