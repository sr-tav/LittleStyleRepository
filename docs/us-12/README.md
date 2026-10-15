# Documentación de US-12 — Seguimiento de Estados del Pedido y Notificaciones por Correo

Yo como Cliente, quiero consultar el estado de mi pedido en tiempo real y
recibir correos con el número de guía de envío, para tener trazabilidad
continua de la entrega.

## Qué se implementó

- **Máquina de estados:** `PENDIENTE_PAGO → PAGADO → EN_PREPARACION → ENVIADO`,
  con cancelación desde `PENDIENTE_PAGO`, `PAGADO` o `EN_PREPARACION`. El ciclo
  dentro de la plataforma termina en `ENVIADO`, cuando el paquete pasa a la
  transportadora con su número de guía (la logística externa no es alcance).
  `ENVIADO` y `CANCELADO` son terminales; las transiciones inválidas
  responden 409. El número de guía es obligatorio para pasar a `ENVIADO`.
- **Historial de estados:** cada transición (incluida la creación) queda
  registrada con fecha, lo que da la trazabilidad de RNF-13 y la línea de
  tiempo del detalle.
- **Avance por administrador:** `PUT /api/admin/pedidos/{id}/estado` (US-14
  construirá su UI sobre este endpoint). El cliente solo consulta.
- **Correo Gmail SMTP (ADR-07):** `CorreoService` asíncrono con plantilla HTML;
  al confirmarse el `ENVIADO` se envía confirmación con número de guía
  (oyente `AFTER_COMMIT`, nunca rompe la transición). Sin credenciales
  (`MAIL_USER`/`MAIL_PASS`, contraseña de aplicación) solo se registra.
- **Consulta del cliente:** `GET /api/cliente/checkout/pedidos` y
  `GET /api/cliente/checkout/pedidos/{id}` (pedido ajeno = 404). Páginas
  `MisPedidos` (UI-17) y `PedidoDetallePage` (UI-16) con línea de tiempo,
  guía, productos y totales. Tile de Mis pedidos habilitado en el inicio.
- **Pedidos del vendedor:** `GET /api/vendedor/pedidos` y
  `GET /api/vendedor/pedidos/{id}` devuelven solo los pedidos con líneas de
  sus prendas (de cada pedido, solo sus líneas) con dirección completa para
  preparar el despacho. Sin acciones de cambio de estado: eso es del
  administrador (US-14). Páginas del panel vendedor con tile habilitado.
- **Rendimiento y carga (RNF-13/RNF-23):** medición formal en US-13.

## Archivos de documentación

| Archivo | Contenido |
|---|---|
| `descripcion-funcional.md` | Máquina de estados, correo, endpoints, RNF-13/RNF-23 |
| `casos-de-prueba.md` | CP-US12 con trazabilidad a tests |
