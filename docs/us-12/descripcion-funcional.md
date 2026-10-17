# US-12 Descripción funcional — Seguimiento de Estados y Notificaciones

## 1. Objetivo
Consultar el estado del pedido en tiempo real y recibir por correo el número
de guía al enviarse, con trazabilidad completa del ciclo de vida.

## 2. Alcance
Incluye máquina de estados con `REGISTRADO`, guía obligatoria en `ENVIADO`,
historial de transiciones, despachos por vendedor (estilo Mercado Libre),
avance del administrador (solo cancelación) y de cada vendedor sobre su
paquete, cancelación del cliente antes del pago, reposición total de stock al
cancelar, correos Gmail SMTP con plantillas HTML (creación, pago confirmado,
cambio de estado, confirmación con guía por despacho, aviso al vendedor y
alerta de stock), consulta de pedidos del cliente (estado global + bloque por
vendedor) y páginas de historial y detalle con refresco manual. No incluye
reembolso automático del dinero (pendiente del dueño), SMS ni app de
transportadora. El panel administrativo de pedidos (UI) queda para US-14.

## 3. Reglas
- Ciclo del pedido madre: `PENDIENTE_PAGO → PAGADO → REGISTRADO → EN_PREPARACION
  → PARCIALMENTE_ENVIADO → ENVIADO`; terminales: `ENVIADO` y `CANCELADO`.
  Se puede cancelar desde `PENDIENTE_PAGO`, `PAGADO`, `REGISTRADO` o
  `EN_PREPARACION`. No se puede cancelar un pedido ya parcial o totalmente
  enviado. Transición inválida → 409.
- Despachos por vendedor: al confirmarse el pago (Wompi), tras descontar el
  stock, el sistema agrupa las líneas por vendedor y crea un despacho por cada
  uno, todos en `REGISTRADO` (uno por pedido y vendedor). Los despachos se crean
  cuando el pedido pasa a `PAGADO` y `REGISTRADO`, nunca antes. Cada línea queda
  asignada a su despacho (`pedido_items.despacho_id`).
- La clase `Despacho` reutiliza `EstadoPedido` pero solo usa `REGISTRADO`,
  `EN_PREPARACION` y `ENVIADO`: `REGISTRADO → EN_PREPARACION → ENVIADO`.
  Cada vendedor ve en su panel solo su paquete, con su estado y sus productos,
  y lo avanza con su guía. Si intenta actuar sobre el paquete de otro, el
  servidor lo rechaza con 403 (el recurso existe pero no es suyo; el 404 se
  reserva para lo inexistente). El 403 es más viable que el 404 porque el
  vendedor está autenticado y el rechazo es por propiedad, no por inexistencia.
- Estado derivado del pedido madre (nadie lo edita a mano): cada vez que cambia
  un despacho, en la misma transacción y con `bloquearPorId` (pedido + despacho)
  para que dos vendedores no se pisen, se recalcula: todos `REGISTRADO` queda
  `REGISTRADO`; alguno en preparación queda `EN_PREPARACION`; alguno enviado y
  otros no queda `PARCIALMENTE_ENVIADO`; todos enviados queda `ENVIADO`.
- Mapeo con los documentos: "Registrado" (US-12) = `REGISTRADO` (pago
  confirmado y listo para preparar); "registrado, en preparación y
  entregado" (Definición, proceso 2) = `REGISTRADO`, `EN_PREPARACION` y
  `ENVIADO` — el sistema no rastrea la entrega de la transportadora, solo
  el despacho con guía. `PENDIENTE_PAGO` y `PAGADO` son técnicos previos.
- `PAGADO` es transitorio: la confirmación del pago deja el pedido en
  `REGISTRADO` en la misma transacción (dos filas de historial) y crea los
  despachos en esa misma transacción.
- Avance manual: el ADMINISTRADOR solo puede `CANCELADO` (409 a cualquier otro
  estado manual: el avance lo hace cada vendedor en su despacho y el pedido
  madre se recalcula solo). El VENDEDOR solo avanza su despacho a
  `EN_PREPARACION` y `ENVIADO` con guía. La cancelación sigue siendo del
  administrador (o del cliente antes de pagar) y repone el stock de todo el
  pedido.
- El CLIENTE solo cancela antes del pago (`PENDIENTE_PAGO`).
- Cancelar con stock descontado (`PAGADO`, `REGISTRADO`, `EN_PREPARACION`,
  `PARCIALMENTE_ENVIADO`) repone las unidades talla por talla; si una línea
  falla, la transacción completa falla (rollback): dejar el pedido `CANCELADO`
  con stock repuesto a medias y solo un log es peor que reintentar. El
  reembolso del dinero NO es automático (pendiente del dueño).
- Concurrencia: el avance de despachos usa bloqueo pesimista (`bloquearPorId`
  de pedido y despacho en la misma transacción); dos vendedores no pisan el
  estado ni duplican la reposición.
- `ENVIADO` (del despacho) exige número de guía no vacío → 400 sin ella.
- Cada transición (y la creación) escribe una fila de historial con fecha; las
  filas de despacho guardan `despacho_id` y la guía de ese envío.
- Correos (un correo por despacho, con su guía): creación, pago confirmado,
  pago rechazado (sin cargo ni pedido, con la referencia para reintentar),
  cambio de estado con etiqueta en español (nunca el enum crudo), confirmación
  con guía cuando un vendedor envía SU paquete (solo sus líneas), y aviso a cada
  vendedor con solo sus líneas al confirmarse el pago (Definición §3.2.1).
  `PAGADO` no duplica correo: tiene el suyo y el genérico se omite. Sin
  credenciales no se envía (se registra y se continúa); ante fallo transitorio
  se reintenta 3 veces (ADR-14) con timeouts SMTP configurados.
- Un cliente solo ve sus pedidos; el pedido ajeno responde 404. En "Mis Pedidos"
  el cliente ve el estado global y, dentro del pedido, un bloque por vendedor
  con su estado y su guía.
- La UI recarga el detalle con botón "Actualizar" (sin polling) y muestra
  historial con fechas; la lista muestra fecha, total y guía.

## 4. Seguridad y datos
Avance solo VENDEDOR dueño del despacho (403 si es de otro); cancelación del
administrador; cancelación del cliente solo sobre pedido propio pendiente de
pago; consulta solo CLIENTE dueño. La dirección del pedido viaja cifrada en
reposo (convertidores existentes); el correo usa la dirección de la cuenta.
Sin secretos en código: todo por entorno.

## 5. Endpoints
- `PUT /api/admin/pedidos/{id}/estado` `{nuevo: CANCELADO}` → 200, 404, 409
  (cualquier otro estado manual → 409; parcial/totalmente enviado → 409).
- `PUT /api/vendedor/pedidos/{despachoId}/estado` `{nuevo, numeroGuia?}` →
  200, 400, 403 (paquete de otro), 404 (inexistente), 409.
- `GET /api/vendedor/pedidos` y `GET /api/vendedor/pedidos/{despachoId}` →
  200 solo con sus despachos, 403 ajeno, 404 inexistente.
- `GET /api/cliente/checkout/pedidos` → 200 lista propia.
- `GET /api/cliente/checkout/pedidos/{id}` → 200 detalle con estado global,
  bloques por vendedor e historial, 404 ajeno.
- `POST /api/cliente/checkout/pedidos/{id}/cancelar` → 200 cancelado, 404 ajeno, 409 si ya se pagó.

## 6. Supuestos y brechas
- País de origen e importador no están en el modelo. Cuidado SÍ está
  (`instruccionesCuidado`, obligatorio al crear la prenda) y se muestra en
  la ficha (US-09).
- Resuelta la limitación multi-vendedor: un despacho por vendedor con su estado
  y su guía; el pedido madre deriva su estado solo.
- RNF-13 (>98% trazabilidad) y RNF-23 (interop SMTP) se miden en US-13; aquí
  quedan mecanismos y pruebas de casos.
- En `prod` (`validate`) se requieren las columnas `numero_guia`,
  `fecha_actualizacion`, la tabla `pedido_estados`, la tabla `despachos` con
  su clave única `(pedido_id, vendedor_id)`, `pedido_items.despacho_id` y
  `pedido_estados.despacho_id` (ver `docs/us-12/migracion-despachos.sql`).
