# US-12 Descripción funcional — Seguimiento de Estados y Notificaciones

## 1. Objetivo
Consultar el estado del pedido en tiempo real y recibir por correo el número
de guía al enviarse, con trazabilidad completa del ciclo de vida.

## 2. Alcance
Incluye máquina de estados con `REGISTRADO`, guía obligatoria en `ENVIADO`,
historial de transiciones, avance por administrador y por vendedor,
cancelación del cliente antes del pago, reposición de stock al cancelar,
correos Gmail SMTP con plantillas HTML (creación, pago confirmado, cambio de
estado, confirmación con guía, aviso al vendedor y alerta de stock), consulta
de pedidos del cliente y páginas de historial y detalle con refresco manual.
No incluye reembolso automático del dinero (pendiente del dueño), SMS ni app
de transportadora. El panel administrativo de pedidos (UI) queda para US-14.

## 3. Reglas
- Ciclo: `PENDIENTE_PAGO → PAGADO → REGISTRADO → EN_PREPARACION → ENVIADO`;
  terminales: `ENVIADO` y `CANCELADO`. Se puede cancelar desde
  `PENDIENTE_PAGO`, `PAGADO`, `REGISTRADO` o `EN_PREPARACION`. Transición
  inválida → 409.
- Mapeo con los documentos: "Registrado" (US-12) = `REGISTRADO` (pago
  confirmado y listo para preparar); "registrado, en preparación y
  entregado" (Definición, proceso 2) = `REGISTRADO`, `EN_PREPARACION` y
  `ENVIADO` — el sistema no rastrea la entrega de la transportadora, solo
  el despacho con guía. `PENDIENTE_PAGO` y `PAGADO` son técnicos previos.
- `PAGADO` es transitorio: la confirmación del pago deja el pedido en
  `REGISTRADO` en la misma transacción (dos filas de historial).
- Avance manual: el ADMINISTRADOR acepta `REGISTRADO`, `EN_PREPARACION`,
  `ENVIADO` y `CANCELADO` — `PAGADO` a mano es 409 (sería vender sin pago
  ni descuento de stock). El VENDEDOR solo avanza sus pedidos a
  `EN_PREPARACION` y `ENVIADO`; cancelar es del administrador porque el
  pedido tiene estado y guía únicos aunque incluya varios vendedores.
- El CLIENTE solo cancela antes del pago (`PENDIENTE_PAGO`); después, la
  cancelación es del vendedor/administrador.
- Cancelar con stock descontado (`PAGADO`, `REGISTRADO`, `EN_PREPARACION`)
  repone las unidades talla por talla; el reembolso del dinero NO es
  automático (pendiente del dueño).
- Concurrencia: el avance usa bloqueo pesimista (`bloquearPorId`), igual
  que pago y expiración; dos avances no pisan el estado ni duplican la
  reposición.
- `ENVIADO` exige número de guía no vacío → 400 sin ella.
- Cada transición (y la creación) escribe una fila de historial con fecha.
- Correos (un solo aviso por hecho): creación, pago confirmado, pago rechazado
  (sin cargo ni pedido, con la referencia para reintentar), cambio de estado
  con etiqueta en español (nunca el enum crudo), confirmación con guía al
  pasar a `ENVIADO`, y aviso a cada vendedor con solo sus líneas al
  confirmarse el pago (Definición §3.2.1). `PAGADO` no duplica correo: tiene
  el suyo y el genérico se omite. Sin credenciales no se envía (se registra
  y se continúa); ante fallo transitorio se reintenta 3 veces (ADR-14) con
  timeouts SMTP configurados.
- Un cliente solo ve sus pedidos; el pedido ajeno responde 404. El vendedor
  solo ve y avanza pedidos con sus prendas.
- La UI recarga el detalle con botón "Actualizar" (sin polling) y muestra
  historial con fechas; la lista muestra fecha, total y guía.

## 4. Seguridad y datos
Avance solo ADMINISTRADOR o VENDEDOR dueño de líneas; cancelación del
cliente solo sobre pedido propio pendiente de pago; consulta solo CLIENTE
dueño. La dirección del pedido viaja cifrada en reposo (convertidores
existentes); el correo usa la dirección de la cuenta. Sin secretos en
código: todo por entorno.

## 5. Endpoints
- `PUT /api/admin/pedidos/{id}/estado` `{nuevo, numeroGuia?}` → 200, 400, 404, 409.
- `PUT /api/vendedor/pedidos/{id}/estado` `{nuevo, numeroGuia?}` → 200, 400, 404, 409.
- `GET /api/vendedor/pedidos` y `GET /api/vendedor/pedidos/{id}` → 200, 404 ajeno.
- `GET /api/cliente/checkout/pedidos` → 200 lista propia.
- `GET /api/cliente/checkout/pedidos/{id}` → 200 detalle con historial, 404 ajeno.
- `POST /api/cliente/checkout/pedidos/{id}/cancelar` → 200 cancelado, 404 ajeno, 409 si ya se pagó.

## 6. Supuestos y brechas
- País de origen e importador no están en el modelo. Cuidado SÍ está
  (`instruccionesCuidado`, obligatorio al crear la prenda) y se muestra en
  la ficha (US-09).
- Pedido multi-vendedor: un solo estado y una sola guía por pedido
  (limitación conocida; la coordinación es del administrador).
- RNF-13 (>98% trazabilidad) y RNF-23 (interop SMTP) se miden en US-13; aquí
  quedan mecanismos y pruebas de casos.
- En `prod` (`validate`) se requieren las columnas `numero_guia`,
  `fecha_actualizacion` y la tabla `pedido_estados` antes de arrancar.
