# US-11: Integración con pasarela de pago externa Wompi

## Objetivo y alcance

US-11 pertenece al Proceso 2 de LittleStyle (catálogo, compras, pedidos e inventario). Inicia el pago de la compra preparada en US-10 sin crear un pedido anticipadamente: genera datos firmados para Wompi, abre el Widget en Angular y recibe/verifica las notificaciones asíncronas del proveedor. Solo una aprobación verificada crea el pedido, descuenta el stock atómicamente y retira del carrito las cantidades adquiridas. Los pedidos `PENDIENTE_PAGO` generados por versiones anteriores siguen siendo compatibles.

Fuera de alcance: reembolsos automáticos (la transacción queda marcada `REEMBOLSO_PENDIENTE` para gestión manual), notificaciones por correo (US-12) y avance operativo del pedido después de `PAGADO` (US-12).

## Actor e historia de usuario

**Actor principal:** cliente autenticado con rol `CLIENTE`. **Actor externo:** Pasarela de Pagos Wompi.

**Historia:** yo como cliente quiero realizar el pago seguro de mi pedido mediante la pasarela Wompi, para completar la transacción financiera dentro de la plataforma.

## Trazabilidad

| Elemento | Relación con US-11 |
|---|---|
| Tarea 1 | Integrar API de Wompi en el backend para generar la transacción de pago: `PagoService.iniciar`, `FirmaWompiService.firmaIntegridad`, `WompiClient`. |
| Tarea 2 | Endpoint webhook de notificaciones asíncronas: `WompiWebhookController` (`POST /api/pagos/wompi/eventos`) y `ReceptorEventosWompi`. |
| Tarea 3 | Verificación de firma SHA-256 y vigencia del evento en el webhook: `FirmaWompiService.eventoValido`. |
| Tarea 4 | Widget de pagos en el checkout Angular: `PagoWompi`, `WompiWidgetService`, `PagoResultadoPage`. |
| Tarea 5 | Pruebas unitarias JUnit/Mockito: `FirmaWompiServiceTest`, `PagoServiceTest`, `ReceptorEventosWompiTest`, `WompiClientTest`, más integración `PagoWompiIntegrationTest` y Vitest. |
| UI-13 / UI-14 | Método de pago y pago: modal del Widget de Wompi abierto desde UI-12. |
| UI-15 | Confirmación de pago: `/cliente/pago/resultado`. |
| US-10 | Consume el snapshot cifrado del checkout; al aprobar, crea el pedido pagado y actualiza stock y carrito. Resuelve CP-US10-26 (aviso de prendas agotadas y reembolso). |
| Plan de calidad 3.8.6 | Integridad transaccional: validación obligatoria de la firma de los webhooks de Wompi. |

### Requisitos no funcionales (Plan de Gestión de la Calidad, sección 4)

| RNF | Requisito | Cómo lo atiende US-11 |
|---|---|---|
| RNF-07 | Autenticidad de notificaciones de pago (Webhooks) | Todo evento se autentica con el checksum SHA-256 de Wompi (el mecanismo que Wompi ofrece en lugar de un HMAC) antes de tocar el pedido. Además se rechaza si su `timestamp` tiene más de 10 minutos, para que un aviso capturado no se pueda reutilizar. El monto y la moneda deben coincidir con los registrados. |
| RNF-21 | Interoperabilidad con la pasarela de pagos externa (excepciones < 2 %) | Integración por API (consulta de transacciones) y webhook. Acepta la fecha del evento en segundos, milisegundos o ISO-8601, es idempotente ante reenvíos, reintenta internamente al aplicar un evento y responde 503 con la cola llena para que Wompi reintente. |
| RNF-28 | Tolerancia a fallos de la pasarela (preservación transaccional ≥ 99 %) | Tiempos máximos de conexión (3 s) y lectura (8 s) hacia Wompi, y de 15 s en el navegador. Si se agotan se responde “Intenta más tarde” sin modificar la transacción ni el pedido. Límite de 5 intentos de pago por pedido. |
| RNF-05 | Capacidad de respuesta ante concurrencia | El webhook responde tras validar la firma y aplica el evento en un grupo acotado de hilos. La verificación directa consulta a Wompi fuera de la transacción de base de datos, así una respuesta lenta no retiene conexiones ni bloqueos de otros pagos. |
| RNF-03 | Sincronización del inventario tras el pago | La aprobación verificada crea el pedido y descuenta el stock de forma atómica en la misma transacción; el carrito se actualiza solo al completar esa operación. |
| RNF-10 | Control de accesos por roles | Endpoints de pago restringidos al rol `CLIENTE` y aislados por propietario (404 para pedidos/pagos ajenos). El webhook es público pero está protegido por RNF-07. |
| RNF-18 | Operabilidad del flujo de compra | Widget abierto desde el checkout, aviso “No se completó el pago” con botón “Reintentar pago” al cerrar la ventana, y UI-15 con el resultado. |

## Flujo

1. En UI-12 el cliente revisa el resumen de checkout y pulsa **Pagar ahora**.
2. Angular llama `POST /api/cliente/pagos/checkout` con perfil y dirección, además de una clave `Idempotency-Key`. El backend vuelve a validar el perfil y el carrito persistido, calcula totales y crea una transacción `CREADA` que guarda el snapshot cifrado. Aún no existe pedido y el carrito sigue intacto. Los reintentos con la misma clave o el mismo checkout activo reutilizan la transacción.
3. La respuesta trae `pedidoId=null`, `llavePublica`, `referencia`, `montoCentavos`, `moneda` y `firmaIntegridad = SHA256(referencia + montoCentavos + moneda + secretoIntegridad)`. El secreto nunca sale del servidor, por lo que el navegador no puede alterar el monto.
4. `WompiWidgetService` carga `https://checkout.wompi.co/widget.js` (una sola vez) y abre el widget. El cliente paga con tarjeta, PSE, Nequi u otro medio habilitado.
5. Wompi notifica al webhook, que valida firma y vigencia, responde 200 de inmediato y aplica el evento en segundo plano. Si el cliente cierra la ventana, puede reintentar; el carrito sigue intacto. Al cerrar el widget con una transacción, Angular navega a `/cliente/pago/resultado?id={transaccionId}` (si se configura `WOMPI_REDIRECT_URL` con un dominio público, Wompi también redirige ahí), y la página llama `POST /api/cliente/pagos/verificar`, que consulta la transacción en Wompi. Lo que llegue primero aplica el estado; lo segundo es idempotente.
6. Si el estado es `APPROVED`, el backend crea el pedido y cambia su estado a `PAGADO`, descuenta stock y quita del carrito las unidades compradas en una transacción atómica. Si ya no hay stock suficiente, crea un pedido cancelado para reembolso y mantiene el carrito. Los pagos pendientes, rechazados o anulados no crean pedido y no alteran el carrito.

## Reglas funcionales

### Verificación de firma del webhook (SHA-256)

Wompi envía `signature.properties` (por ejemplo `transaction.id`, `transaction.status`, `transaction.amount_in_cents`), `signature.checksum` y `timestamp`. El backend concatena los valores de esas rutas dentro de `data`, luego el `timestamp` y el secreto de eventos, calcula SHA-256 y compara en tiempo constante (`MessageDigest.isEqual`, sin distinguir mayúsculas). Si falta cualquier dato, el secreto no está configurado o el checksum no coincide, responde **401** y no toca pedidos ni stock. Un 200 confirma la recepción; si la cola de eventos está llena se devuelve 503 para que Wompi reintente.

### Vigencia y formato de la fecha del evento (RNF-07, RNF-21)

- Un evento con firma válida solo se acepta si su `timestamp` está entre 10 minutos antes y 1 minuto después de la hora del servidor (`app.pagos.vigencia-evento`, `app.pagos.tolerancia-reloj`). Así, un aviso auténtico capturado no puede reenviarse después para cambiar el estado de un pago.
- El `timestamp` se acepta en segundos Unix (lo que envía Wompi hoy), milisegundos Unix o fecha ISO-8601 con o sin zona (sin zona se asume UTC). La firma se calcula con el valor tal como llegó y la vigencia con la fecha interpretada. Un formato irreconocible se rechaza con 401.

### Robustez ante fallos y carga (RNF-05, RNF-28)

- **Tiempos máximos con Wompi:** la consulta `GET /v1/transactions/{id}` tiene 3 s para conectar y 8 s para responder. Si se agotan, `POST /api/cliente/pagos/verificar` responde **504** “Wompi está tardando en responder. Intenta más tarde; tu pedido se conserva sin cambios.”; si Wompi no está disponible, **503**. La transacción local y el pedido no se modifican.
- **Tiempo máximo en el navegador:** las peticiones de pago y la carga de `widget.js` se cancelan a los 15 s con el mensaje “Intenta más tarde” y el botón se libera.
- **Avisos simultáneos sin fila:** `ReceptorEventosWompi` valida firma y vigencia sin tocar la base de datos, responde 200 y aplica el evento en un grupo de 4 hilos con cola de 100. Cada evento solo bloquea su propia transacción y su pedido. Si la aplicación falla se reintenta hasta 3 veces; si aun así falla, el estado se obtiene con la consulta directa de UI-15.
- **Sin retener conexiones:** la verificación directa consulta a Wompi antes de abrir la transacción de base de datos, que solo dura lo que tarda aplicar el estado.
- **Límite de intentos:** los pedidos históricos admiten como máximo 5 intentos (`app.pagos.max-intentos-por-pedido`). Para checkout nuevo se usa clave idempotente y solo se permite un pago activo por cliente; un checkout distinto responde **409** mientras haya un pago pendiente.

### Cierre de la ventana de pago (RNF-18)

Wompi no invoca el callback del Widget cuando el cliente cierra el modal sin pagar; al cerrarlo, agrega el atributo `hidden` a su elemento `.waybox-modal`. `WompiWidgetService` lo detecta con un `MutationObserver` (y también trata como cierre un callback sin transacción) y emite `{ tipo: 'cerrado' }`. `PagoWompi` muestra entonces “No se completó el pago. Cerraste la ventana de pago antes de terminar; tu carrito se conserva y puedes reintentarlo.” y el botón cambia a **“Reintentar pago”**.

### Validaciones adicionales antifraude

- La referencia debe existir en `transacciones_pago`; eventos de referencias desconocidas se registran y se ignoran (200).
- El `amount_in_cents` y la `currency` reportados deben coincidir con los registrados al iniciar el pago; si no, la transacción queda en `ERROR` y el pedido no se confirma.
- La verificación directa solo procede si la transacción pertenece al cliente autenticado (404 en caso contrario).

### Estados de la transacción

| Wompi | `EstadoTransaccionPago` | Efecto sobre el pedido |
|---|---|---|
| — | `CREADA` | No se ha creado pedido; carrito intacto. |
| `PENDING` | `PENDIENTE` | No se crea pedido y no se permite iniciar otro checkout mientras siga activo. |
| `APPROVED` | `APROBADA` | Crea pedido `PAGADO`, descuenta stock y quita del carrito las cantidades pagadas. |
| `APPROVED` sin stock | `REEMBOLSO_PENDIENTE` | Crea pedido `CANCELADO`, registra motivo y prendas agotadas; conserva el carrito. |
| `DECLINED` | `RECHAZADA` | No crea pedido y conserva el carrito; puede iniciar un nuevo intento. |
| `VOIDED` | `ANULADA` | No crea pedido y conserva el carrito. |
| `ERROR` | `ERROR` | No crea pedido y conserva el carrito; puede reintentar. |

`APROBADA`, `ANULADA` y `REEMBOLSO_PENDIENTE` son finales: eventos repetidos o fuera de orden no los modifican, de modo que un reenvío del webhook nunca descuenta stock dos veces.

### Reembolso

**Decisión de alcance:** si Wompi aprueba el cobro pero el pedido no puede pagarse, la transacción queda `REEMBOLSO_PENDIENTE` con `motivoReembolso` (`STOCK_INSUFICIENTE`, `PEDIDO_NO_PAGABLE` o `PAGO_ANULADO` —cobro tardío sobre una transacción que el cliente anuló—) para que el equipo gestione la devolución desde el panel de Wompi. El reembolso automático por API no se implementa porque Wompi solo permite anular por API ciertas transacciones con tarjeta. El cliente ve el aviso de reembolso en UI-15.

### Prendas agotadas al pagar (resuelve CP-US10-26)

UI-15 lista nombre, talla, cantidad solicitada y existencia actual de cada prenda agotada. Como el carrito no se vacía antes de aprobar, el cliente puede revisar/ajustar directamente las cantidades conservadas y volver al checkout; el pedido cancelado no se reabre. La pantalla también ofrece volver al catálogo.

## Contratos backend

| Contrato | Descripción |
|---|---|
| `POST /api/cliente/pagos/checkout` | Rol `CLIENTE`. Body `CrearPedidoRequest` con `perfilInfantilId` y `direccion`; las líneas se toman del carrito autenticado. Acepta `Idempotency-Key`. 201 con `InicioPagoResponse` (`pedidoId=null`, referencia, monto en centavos, moneda, llave pública y firma). 409 si hay otro pago activo para un checkout distinto; 503 si Wompi no está configurado. No crea pedido ni vacía carrito. |
| `DELETE /api/cliente/pagos/{referencia}` | Rol `CLIENTE`. Anula la transacción del cliente si está en `CREADA` (200 con `ResultadoPagoResponse` en `ANULADA`) y libera la compra para un nuevo intento. Idempotente si ya está `ANULADA`; 409 en cualquier otro estado; 404 si no es suya. |
| `POST /api/cliente/pagos/verificar` | Rol `CLIENTE`. Body `{ "transaccionId": "..." }`. Consulta Wompi, aplica el estado y responde `ResultadoPagoResponse`. 504 si Wompi no responde a tiempo, 503 si no está disponible, 502 si responde algo inválido. |
| `GET /api/cliente/pagos/{referencia}` | Rol `CLIENTE`. Estado local del pago sin consultar Wompi. |
| `POST /api/pagos/wompi/eventos` | Público (sin JWT), protegido por checksum SHA-256 y vigencia de 10 minutos. 200 recibido (se aplica en segundo plano); 401 firma inválida o evento vencido; 503 cola llena. |
| `ResultadoPagoResponse` | `pedidoId` (nulo hasta aprobación), `referencia`, `estadoPago`, `estadoPedido`, `motivoReembolso`, `metodoPago`, `total`, `prendasAgotadas`, `lineasDisponibles`. |

## Persistencia

- `transacciones_pago`: cliente, pedido opcional (solo para compatibilidad histórica), snapshot cifrado del checkout, clave de idempotencia, referencia única, id de Wompi, monto en centavos, moneda, estado, método, motivo de reembolso y fechas. Se requiere la migración SQL manual documentada en `migracion-checkout-pago.sql` antes de desplegar con `ddl-auto=validate`.
- `transacciones_pago_agotadas`: prendas/tallas sin stock al aprobarse el pago.
- No se almacenan datos de tarjeta ni de cuenta: el cliente los ingresa solo en el widget de Wompi.

## Configuración

Propiedades `app.wompi.*` en `application.properties`, leídas de variables de entorno (`WOMPI_PUBLIC_KEY`, `WOMPI_INTEGRITY_SECRET`, `WOMPI_EVENTS_SECRET`, `WOMPI_API_URL`, `WOMPI_REDIRECT_URL`). `WompiProperties.toString()` oculta los secretos.

Los límites operativos están en `app.pagos.*` (`PagoProperties`): `vigencia-evento` (PT10M), `tolerancia-reloj` (PT1M), `timeout-conexion` (PT3S), `timeout-lectura` (PT8S), `max-intentos-por-pedido` (5), `eventos-asincronos` (true), `hilos-eventos` (4) y `cola-eventos` (100). La vigencia, los dos tiempos máximos y el máximo de intentos se pueden ajustar con `PAGOS_VIGENCIA_EVENTO`, `PAGOS_TIMEOUT_CONEXION`, `PAGOS_TIMEOUT_LECTURA` y `PAGOS_MAX_INTENTOS`. Para producción se cambia `WOMPI_API_URL` a `https://production.wompi.co/v1` y las llaves `pub_prod_`/`prod_integrity_`/`prod_events_`.

## Cambios mínimos sobre código existente

- `SecurityConfig`: una regla `permitAll` para `POST /api/pagos/wompi/eventos`.
- `WompiConfig`: registra también `PagoProperties`.
- `checkout.html`/`checkout.ts` (US-10): `<app-pago-wompi>` ahora inicia el pago desde el resumen; no existe pedido previo pendiente.
- `checkout.spec.ts` (US-10): se ajustó la aserción que esperaba el botón deshabilitado y se agregó el mock de `PagoService`.
- `app.routes.ts`, `catalogo-compras.routes.ts`, `styles.scss`, `application.properties`: solo se agregaron entradas.

## Límites conocidos

- El reembolso es manual (`REEMBOLSO_PENDIENTE`), incluido el caso de un cobro aprobado por Wompi después de que el cliente canceló el pago.
- Con el flujo de checkout no existe pedido mientras el pago está pendiente, así que no está en vigor la expiración de 30 minutos de pedidos `PENDIENTE_PAGO`; el cliente abandona una compra con la cancelación (`DELETE /api/cliente/pagos/{referencia}`).
- La notificación por correo del pago aprobado corresponde a US-12.
- Wompi reintenta el webhook cuando no recibe 200. Si un reintento llega después de la vigencia de 10 minutos se rechaza; el estado del pago se recupera igualmente con la consulta directa de UI-15 (`POST /api/cliente/pagos/verificar`).
- Los eventos que esperan en la cola en memoria se pierden si el backend se reinicia antes de aplicarlos. Ya respondieron 200, así que Wompi no los reenvía; el estado se obtiene con la consulta directa.
- La detección del cierre del Widget depende de la clase `waybox-modal` que usa Wompi. Si Wompi la cambia, el botón se libera igual al abrirse la pasarela, pero no se mostraría el aviso “No se completó el pago”.
- Wompi responde **403** al abrir el checkout si `redirect-url` apunta a `localhost` o `127.0.0.1`. Por eso `WompiProperties.urlRedireccionPublica()` omite las URLs locales y en desarrollo el regreso a UI-15 lo hace el callback del Widget. En producción se define `WOMPI_REDIRECT_URL` con el dominio público.
