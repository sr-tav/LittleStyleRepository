# Documentación de US-12 — Seguimiento de Estados del Pedido y Notificaciones por Correo

Yo como Cliente, quiero consultar el estado de mi pedido en tiempo real y
recibir correos con el número de guía de envío, para tener trazabilidad
continua de la entrega.

## Qué se implementó

- **Despachos por vendedor (estilo Mercado Libre):** al pagarse por Wompi, tras
  descontar el stock, el sistema agrupa las líneas por vendedor y crea un
  despacho por cada uno, todos en `REGISTRADO`. Cada vendedor ve en su panel
  solo su paquete, con su estado y sus productos, y lo pasa a `EN_PREPARACIÓN`
  y luego a `ENVIADO` ingresando su guía. Si intenta actuar sobre el paquete
  de otro, el servidor lo rechaza con 403. Los despachos se crean cuando el
  pedido pasa a `PAGADO` y `REGISTRADO`, no antes.
- **Máquina de estados derivada:** cada vez que cambia un despacho, el sistema
  recalcula el estado del pedido madre en la misma transacción y con
  `bloquearPorId`: todos `REGISTRADO` queda `REGISTRADO`; alguno en preparación
  queda `EN_PREPARACION`; alguno enviado y otros no queda
  `PARCIALMENTE_ENVIADO`; todos enviados queda `ENVIADO`. Nadie lo edita a
  mano. La cancelación sigue siendo del administrador (o del cliente antes de
  pagar) con reposición total del stock; no se puede cancelar un pedido ya
  parcial o totalmente enviado. `ENVIADO` y `CANCELADO` son terminales; las
  transiciones inválidas responden 409. El número de guía es obligatorio para
  enviar cada despacho. Cancelar con pago previo repone el stock y, si una
  línea falla, la transacción completa falla (el reembolso del dinero no es
  automático).
- **Historial de estados:** cada transición (incluida la creación) queda
  registrada con fecha, con `despacho_id` y guía en las filas de despacho, lo
  que da la trazabilidad de RNF-13 y la línea de tiempo del detalle.
- **Avance por administrador y por vendedor:** `PUT /api/admin/pedidos/{id}/estado`
  (admin: solo `CANCELADO`; cualquier otro estado manual es 409) y
  `PUT /api/vendedor/pedidos/{despachoId}/estado` (vendedor: solo
  `EN_PREPARACION` y `ENVIADO` sobre SU despacho; ajeno 403, inexistente 404).
  El panel admin de pedidos (UI) queda para US-14. El cliente cancela solo
  antes del pago (`POST /api/cliente/checkout/pedidos/{id}/cancelar`).
- **Correo Gmail SMTP (ADR-07):** `CorreoService` asíncrono con plantilla HTML y
  3 reintentos ante fallo transitorio, timeouts SMTP configurados; un correo
  por despacho con su guía cuando el vendedor envía su paquete (solo sus
  líneas), más creación, pago, cambio con etiqueta en español y aviso al
  vendedor con sus líneas. `PAGADO` no duplica. Sin credenciales
  (`MAIL_USER`/`MAIL_PASS`, contraseña de aplicación) solo se registra.
- **Consulta del cliente:** `GET /api/cliente/checkout/pedidos` y
  `GET /api/cliente/checkout/pedidos/{id}` (pedido ajeno = 404): estado global
  y, dentro del pedido, un bloque por vendedor con su estado y su guía.
  Páginas `MisPedidos` (UI-17, con fecha, total y guía) y `PedidoDetallePage`
  (UI-16) con línea de tiempo, bloques por vendedor, historial con fechas,
  guía, productos, totales, botón Actualizar y cancelación pre-pago. Tile de
  Mis pedidos habilitado en el inicio.
- **Paquetes del vendedor:** `GET /api/vendedor/pedidos` y
  `GET /api/vendedor/pedidos/{despachoId}` devuelven solo sus despachos con
  sus productos y su guía, más UI de avance a preparación/enviado con guía.
  Páginas del panel vendedor con tile habilitado.
- **Rendimiento y carga (RNF-13/RNF-23):** medición formal en US-13.

## Archivos de documentación

| Archivo | Contenido |
|---|---|
| `descripcion-funcional.md` | Despachos, estado derivado, correo por despacho, endpoints, RNF-13/RNF-23 |
| `casos-de-prueba.md` | CP-US12 con trazabilidad a tests |
| `migracion-despachos.sql` | DDL + backfill de despachos para bases existentes |
