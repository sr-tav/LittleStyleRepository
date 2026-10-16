# Documentación de US-12 — Seguimiento de Estados del Pedido y Notificaciones por Correo

Yo como Cliente, quiero consultar el estado de mi pedido en tiempo real y
recibir correos con el número de guía de envío, para tener trazabilidad
continua de la entrega.

## Qué se implementó

- **Máquina de estados:** `PENDIENTE_PAGO → PAGADO → REGISTRADO → EN_PREPARACION → ENVIADO`,
  con cancelación desde `PENDIENTE_PAGO`, `PAGADO`, `REGISTRADO` o `EN_PREPARACION`.
  `PAGADO` es transitorio (el pago deja el pedido `REGISTRADO` en la misma
  transacción). El ciclo dentro de la plataforma termina en `ENVIADO`, cuando el
  paquete pasa a la transportadora con su número de guía (la logística externa
  no es alcance). `ENVIADO` y `CANCELADO` son terminales; las transiciones
  inválidas responden 409. El número de guía es obligatorio para pasar a `ENVIADO`.
  El avance usa bloqueo pesimista y cancelar con pago previo repone el stock
  (el reembolso del dinero no es automático).
- **Historial de estados:** cada transición (incluida la creación) queda
  registrada con fecha, lo que da la trazabilidad de RNF-13 y la línea de
  tiempo del detalle.
- **Avance por administrador y por vendedor:** `PUT /api/admin/pedidos/{id}/estado`
  (admin: `REGISTRADO`, `EN_PREPARACION`, `ENVIADO`, `CANCELADO`; `PAGADO` a mano
  es 409) y `PUT /api/vendedor/pedidos/{id}/estado` (vendedor: solo `EN_PREPARACION`
  y `ENVIADO` sobre pedidos con sus prendas). El panel admin de pedidos (UI)
  queda para US-14. El cliente cancela solo antes del pago
  (`POST /api/cliente/checkout/pedidos/{id}/cancelar`).
- **Correo Gmail SMTP (ADR-07):** `CorreoService` asíncrono con plantilla HTML y
  3 reintentos ante fallo transitorio, timeouts SMTP configurados; un correo por
  hecho (creación, pago, cambio con etiqueta en español, guía en `ENVIADO`,
  aviso al vendedor con sus líneas). `PAGADO` no duplica. Sin credenciales
  (`MAIL_USER`/`MAIL_PASS`, contraseña de aplicación) solo se registra.
- **Consulta del cliente:** `GET /api/cliente/checkout/pedidos` y
  `GET /api/cliente/checkout/pedidos/{id}` (pedido ajeno = 404). Páginas
  `MisPedidos` (UI-17, con fecha, total y guía) y `PedidoDetallePage` (UI-16)
  con línea de tiempo, historial con fechas, guía, productos, totales, botón
  Actualizar y cancelación pre-pago. Tile de Mis pedidos habilitado en el inicio.
- **Pedidos del vendedor:** `GET /api/vendedor/pedidos` y
  `GET /api/vendedor/pedidos/{id}` devuelven solo los pedidos con líneas de
  sus prendas (de cada pedido, solo sus líneas) con dirección completa para
  preparar el despacho, más UI de avance a preparación/enviado con guía.
  Páginas del panel vendedor con tile habilitado.
- **Rendimiento y carga (RNF-13/RNF-23):** medición formal en US-13.

## Archivos de documentación

| Archivo | Contenido |
|---|---|
| `descripcion-funcional.md` | Máquina de estados, correo, endpoints, RNF-13/RNF-23 |
| `casos-de-prueba.md` | CP-US12 con trazabilidad a tests |
