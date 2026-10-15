# US-12 Descripción funcional — Seguimiento de Estados y Notificaciones

## 1. Objetivo
Consultar el estado del pedido en tiempo real y recibir por correo el número
de guía al enviarse, con trazabilidad completa del ciclo de vida.

## 2. Alcance
Incluye máquina de estados con guía obligatoria en `ENVIADO`, historial de
transiciones, avance por administrador, correos Gmail SMTP con plantillas HTML
(creación, pago confirmado, cambio de estado y confirmación con guía al pasar
a `ENVIADO`), consulta de pedidos del cliente y páginas de historial y detalle.
No incluye reembolso automático (US-11/US-12 pendiente de dueño), SMS ni app de
transportadora.

## 3. Reglas
- Ciclo: `PENDIENTE_PAGO → PAGADO → EN_PREPARACION → ENVIADO`; terminales:
  `ENVIADO` y `CANCELADO`. Se puede cancelar desde `PENDIENTE_PAGO`,
  `PAGADO` o `EN_PREPARACION`. Transición inválida → 409.
- `ENVIADO` exige número de guía no vacío → 400 sin ella.
- Cada transición (y la creación) escribe una fila de historial con fecha.
- El correo de confirmación con guía se envía solo al pasar a `ENVIADO`,
  después de confirmado el cambio; si el correo falla, el pedido ya quedó
  enviado y el error solo queda en el log.
- Al crear el pedido, al confirmarse el pago y en cada cambio de estado
  (salvo `ENVIADO`, que ya tiene el suyo) se publica un evento de dominio y,
  tras el commit, se envía el correo correspondiente al cliente. Si el correo
  falla, el pedido no se revierte.
- Un cliente solo ve sus pedidos; el pedido ajeno responde 404.
- Sin credenciales de correo no se envía nada (se registra y se continúa).

## 4. Seguridad y datos
Avance solo ADMINISTRADOR; consulta solo CLIENTE dueño. La dirección del
pedido viaja cifrada en reposo (convertidores existentes); el correo usa la
dirección de la cuenta. Sin secretos en código: todo por entorno.

## 5. Endpoints
- `PUT /api/admin/pedidos/{id}/estado` `{nuevo, numeroGuia?}` → 200, 400, 404, 409.
- `GET /api/cliente/checkout/pedidos` → 200 lista propia.
- `GET /api/cliente/checkout/pedidos/{id}` → 200 detalle con historial, 404 ajeno.

## 6. Supuestos y brechas
- País de origen, cuidado e importador no están en el modelo (igual que US-09).
- RNF-13 (>98% trazabilidad) y RNF-23 (interop SMTP) se miden en US-13; aquí
  quedan mecanismos y pruebas de casos.
- En `prod` (`validate`) se requieren las columnas `numero_guia`,
  `fecha_actualizacion` y la tabla `pedido_estados` antes de arrancar.
