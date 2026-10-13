# US-11: Integración con pasarela de pago externa Wompi

## Objetivo y alcance

US-11 pertenece al Proceso 2 de LittleStyle (catálogo, compras, pedidos e inventario). Parte del pedido `PENDIENTE_PAGO` que crea US-10 y completa la transacción financiera con Wompi: genera los datos firmados del pago, abre el Widget en Angular, recibe y verifica las notificaciones asíncronas del proveedor y conecta la aprobación con el descuento atómico de stock ya preparado en US-10.

Fuera de alcance: reembolsos automáticos (la transacción queda marcada `REEMBOLSO_PENDIENTE` para gestión manual), notificaciones por correo (US-12) y avance operativo del pedido después de `PAGADO` (US-12).

## Actor e historia de usuario

**Actor principal:** cliente autenticado con rol `CLIENTE`. **Actor externo:** Pasarela de Pagos Wompi.

**Historia:** yo como cliente quiero realizar el pago seguro de mi pedido mediante la pasarela Wompi, para completar la transacción financiera dentro de la plataforma.

## Trazabilidad

| Elemento | Relación con US-11 |
|---|---|
| Tarea 1 | Integrar API de Wompi en el backend para generar la transacción de pago: `PagoService.iniciar`, `FirmaWompiService.firmaIntegridad`, `WompiClient`. |
| Tarea 2 | Endpoint webhook de notificaciones asíncronas: `WompiWebhookController` (`POST /api/pagos/wompi/eventos`). |
| Tarea 3 | Verificación de firma SHA-256 en el webhook: `FirmaWompiService.eventoValido`. |
| Tarea 4 | Widget de pagos en el checkout Angular: `PagoWompi`, `WompiWidgetService`, `PagoResultadoPage`. |
| Tarea 5 | Pruebas unitarias JUnit/Mockito: `FirmaWompiServiceTest`, `PagoServiceTest`, más integración `PagoWompiIntegrationTest` y Vitest. |
| UI-13 / UI-14 | Método de pago y pago: modal del Widget de Wompi abierto desde UI-12. |
| UI-15 | Confirmación de pago: `/cliente/pago/resultado`. |
| US-10 | Consume el pedido `PENDIENTE_PAGO` y conecta `PedidoPagoService.confirmarPago`; resuelve CP-US10-26 (aviso de prendas agotadas y reembolso). |
| Plan de calidad 3.8.6 | Integridad transaccional: validación obligatoria de la firma de los webhooks de Wompi. |
| RNF-10 | Endpoints de pago restringidos al rol `CLIENTE` y aislados por propietario (404 para pedidos/pagos ajenos). |

## Flujo

1. En UI-12 el cliente confirma el pedido (US-10) y pulsa **Pagar**.
2. Angular llama `POST /api/cliente/pagos/pedidos/{pedidoId}`. El backend bloquea el pedido, valida propietario, estado `PENDIENTE_PAGO`, plazo de 30 minutos y que no exista otro intento `PENDIENTE`; crea `TransaccionPago` en `CREADA` con referencia `LS-{pedidoId}-{10 hex aleatorios}` y monto `total × 100` en centavos.
3. La respuesta trae `llavePublica`, `referencia`, `montoCentavos`, `moneda` y `firmaIntegridad = SHA256(referencia + montoCentavos + moneda + secretoIntegridad)`. El secreto nunca sale del servidor, por lo que el navegador no puede alterar el monto.
4. `WompiWidgetService` carga `https://checkout.wompi.co/widget.js` (una sola vez) y abre el widget. El cliente paga con tarjeta, PSE, Nequi u otro medio habilitado.
5. Wompi notifica al webhook. Además, al cerrar el widget con una transacción, Angular navega a `/cliente/pago/resultado?id={transaccionId}` (si se configura `WOMPI_REDIRECT_URL` con un dominio público, Wompi también redirige ahí), y la página llama `POST /api/cliente/pagos/verificar`, que consulta la transacción en la API de Wompi. Lo que llegue primero aplica el estado; lo segundo es idempotente.
6. Si el estado es `APPROVED`, se invoca `PedidoPagoService.confirmarPago` (US-10): descuenta stock y pasa el pedido a `PAGADO`.

## Reglas funcionales

### Verificación de firma del webhook (SHA-256)

Wompi envía `signature.properties` (por ejemplo `transaction.id`, `transaction.status`, `transaction.amount_in_cents`), `signature.checksum` y `timestamp`. El backend concatena los valores de esas rutas dentro de `data`, luego el `timestamp` y el secreto de eventos, calcula SHA-256 y compara en tiempo constante (`MessageDigest.isEqual`, sin distinguir mayúsculas). Si falta cualquier dato, el secreto no está configurado o el checksum no coincide, responde **401** y no toca pedidos ni stock. Un 200 confirma la recepción; ante errores internos se devuelve 5xx para que Wompi reintente.

### Validaciones adicionales antifraude

- La referencia debe existir en `transacciones_pago`; eventos de referencias desconocidas se registran y se ignoran (200).
- El `amount_in_cents` y la `currency` reportados deben coincidir con los registrados al iniciar el pago; si no, la transacción queda en `ERROR` y el pedido no se confirma.
- La verificación directa solo procede si la transacción pertenece al cliente autenticado (404 en caso contrario).

### Estados de la transacción

| Wompi | `EstadoTransaccionPago` | Efecto sobre el pedido |
|---|---|---|
| — | `CREADA` | Pedido sigue `PENDIENTE_PAGO`. |
| `PENDING` | `PENDIENTE` | Sin cambios; no se permite iniciar otro intento mientras dure. |
| `APPROVED` | `APROBADA` | `confirmarPago`: descuenta stock y pasa a `PAGADO`. |
| `APPROVED` sin stock / vencido / pedido no pendiente | `REEMBOLSO_PENDIENTE` | Pedido `CANCELADO` (por US-10) o sin cambios si ya no estaba pendiente; se guarda el motivo y las prendas agotadas. |
| `DECLINED` | `RECHAZADA` | Pedido sigue `PENDIENTE_PAGO`; el cliente puede reintentar mientras no venza. |
| `VOIDED` | `ANULADA` | Sin cambios en el pedido. |
| `ERROR` | `ERROR` | Sin cambios; se puede reintentar. |

`APROBADA`, `ANULADA` y `REEMBOLSO_PENDIENTE` son finales: eventos repetidos o fuera de orden no los modifican, de modo que un reenvío del webhook nunca descuenta stock dos veces.

### Reembolso

**Decisión de alcance:** si Wompi aprueba el cobro pero el pedido no puede pagarse, la transacción queda `REEMBOLSO_PENDIENTE` con `motivoReembolso` (`STOCK_INSUFICIENTE`, `PEDIDO_VENCIDO` o `PEDIDO_NO_PAGABLE`) para que el equipo gestione la devolución desde el panel de Wompi. El reembolso automático por API no se implementa porque Wompi solo permite anular por API ciertas transacciones con tarjeta. El cliente ve el aviso de reembolso en UI-15.

### Prendas agotadas al pagar (resuelve CP-US10-26)

UI-15 lista nombre, talla, cantidad solicitada y existencia actual de cada prenda agotada. Si el pedido tenía otras líneas con stock, ofrece **“Continuar sin las prendas agotadas”**, que las vuelve a agregar al carrito y lleva al checkout para crear un nuevo pedido; siempre ofrece **“Volver al catálogo”**. El pedido cancelado no se reabre, de acuerdo con las transiciones de US-10.

## Contratos backend

| Contrato | Descripción |
|---|---|
| `POST /api/cliente/pagos/pedidos/{pedidoId}` | Rol `CLIENTE`. 201 con `InicioPagoResponse` (`pedidoId`, `referencia`, `montoCentavos`, `moneda`, `llavePublica`, `firmaIntegridad`, `urlRedireccion`, datos de contacto para prellenar). 404 pedido ajeno, 409 no pendiente/vencido/pago en proceso, 503 pasarela sin configurar. |
| `POST /api/cliente/pagos/verificar` | Rol `CLIENTE`. Body `{ "transaccionId": "..." }`. Consulta Wompi, aplica el estado y responde `ResultadoPagoResponse`. 502 si Wompi no responde. |
| `GET /api/cliente/pagos/{referencia}` | Rol `CLIENTE`. Estado local del pago sin consultar Wompi. |
| `POST /api/pagos/wompi/eventos` | Público (sin JWT), protegido por checksum SHA-256. 200 recibido; 401 firma inválida. |
| `ResultadoPagoResponse` | `pedidoId`, `referencia`, `estadoPago`, `estadoPedido`, `motivoReembolso`, `metodoPago`, `total`, `prendasAgotadas`, `lineasDisponibles`. |

## Persistencia

- `transacciones_pago`: pedido, referencia única, id de Wompi, monto en centavos, moneda, estado, método, motivo de reembolso y fechas. Un pedido puede tener varios intentos.
- `transacciones_pago_agotadas`: prendas/tallas sin stock al aprobarse el pago.
- No se almacenan datos de tarjeta ni de cuenta: el cliente los ingresa solo en el widget de Wompi.

## Configuración

Propiedades `app.wompi.*` en `application.properties`, leídas de variables de entorno (`WOMPI_PUBLIC_KEY`, `WOMPI_INTEGRITY_SECRET`, `WOMPI_EVENTS_SECRET`, `WOMPI_API_URL`, `WOMPI_REDIRECT_URL`). `WompiProperties.toString()` oculta los secretos. Para producción se cambia `WOMPI_API_URL` a `https://production.wompi.co/v1` y las llaves `pub_prod_`/`prod_integrity_`/`prod_events_`.

## Cambios mínimos sobre código existente

- `SecurityConfig`: una regla `permitAll` para `POST /api/pagos/wompi/eventos`.
- `checkout.html`/`checkout.ts` (US-10): el botón deshabilitado “Pagar” se reemplazó por `<app-pago-wompi>`; no se modificó otra lógica del checkout.
- `checkout.spec.ts` (US-10): se ajustó la aserción que esperaba el botón deshabilitado y se agregó el mock de `PagoService`.
- `app.routes.ts`, `catalogo-compras.routes.ts`, `styles.scss`, `application.properties`: solo se agregaron entradas.

## Límites conocidos

- El reembolso es manual (`REEMBOLSO_PENDIENTE`).
- Si un pago `PENDIENTE` (p. ej. PSE) se aprueba después de los 30 minutos, el pedido ya fue cancelado por el proceso de expiración de US-10 y la transacción queda para reembolso.
- La notificación por correo del pago aprobado corresponde a US-12.
- Wompi responde **403** al abrir el checkout si `redirect-url` apunta a `localhost` o `127.0.0.1`. Por eso `WompiProperties.urlRedireccionPublica()` omite las URLs locales y en desarrollo el regreso a UI-15 lo hace el callback del Widget. En producción se define `WOMPI_REDIRECT_URL` con el dominio público.
