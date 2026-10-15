# Casos de prueba — US-12 Seguimiento de Estados y Correo

> Trazabilidad: cada CP mapea a un RNF y a una prueba automatizada. Las
> mediciones de tasas y tiempos van en US-13.

## 1. Matriz de casos

| ID | RNF | Descripción | Precondiciones | Resultado esperado | Tipo | Evidencia |
|---|---|---|---|---|---|---|
| CP-US12-01 | RNF-13 | Flujo normal termina en ENVIADO terminal | Pedido en `PENDIENTE_PAGO`. | `PAGADO → EN_PREPARACION → ENVIADO`; desde `ENVIADO` nada avanza. | Unitaria | `EstadoPedidoTest.permiteElFlujoNormalYTerminaEnEnviado` |
| CP-US12-02 | RNF-13 | Cancelación y saltos inválidos | Pedido en varios estados. | Cancelar antes del despacho; saltos y reapertura → 409. | Unitaria | `EstadoPedidoTest` (2 casos) |
| CP-US12-03 | RNF-13 | Avanzar guarda historial y avisa al enviar | Pedido en `EN_PREPARACION`. | Estado `ENVIADO` con guía, fila de historial y evento publicado. | Unitaria mock | `PedidoEstadoServiceTest.avanzarGuardaHistorialYPublicaEventoAlEnviar` |
| CP-US12-04 | RNF-13 | ENVIADO sin guía es 400 | Pedido en `EN_PREPARACION`. | 400 asociado a `numeroGuia`. | Unitaria | `PedidoEstadoServiceTest.enviarSinGuiaDevuelve400` |
| CP-US12-05 | VAL | Admin avanza, resto no | Pedido existente. | 200 con guía; 404 inexistente; 409 transición inválida. | Controller | `PedidoAdminControllerTest` (2 casos) |
| CP-US12-06 | RNF-23 | Sin credenciales no se envía | Sin `MAIL_USER`/`MAIL_PASS`. | Se omite el envío sin error. | Unitaria | `CorreoServiceTest.sinCredencialesNoEnviaNada` |
| CP-US12-07 | RNF-23 | Correo HTML con guía | Credenciales presentes. | Se envía con asunto y guía. | Unitaria | `CorreoServiceTest.conCredencialesEnviaElHtml` |
| CP-US12-16 | RNF-23 | Correo al crear el pedido | Pedido creado. | Evento + correo con total e ítems. | Unitaria | `NotificacionPedidoListenerTest.creacionYPagoAvisanAlCliente` |
| CP-US12-17 | RNF-23 | Correo al confirmar el pago | Pago aprobado. | Evento + correo de pago confirmado. | Unitaria | `NotificacionPedidoListenerTest.creacionYPagoAvisanAlCliente` |
| CP-US12-18 | RNF-13 | Correo en cada cambio de estado, sin duplicar ENVIADO | Transición a EN_PREPARACION y a ENVIADO. | Genérico solo en la primera; ENVIADO usa el de guía. | Unitaria | `NotificacionPedidoListenerTest` (2 casos) |
| CP-US12-19 | RNF-23 | Correo de alerta de stock al vendedor | Alerta emitida. | Correo con prenda, nivel y stocks. | Unitaria | `NotificacionPedidoListenerTest.alertaStockAvisaAlVendedor` |
| CP-US12-08 | RNF-13 | Cliente lista y detalla lo propio | Pedidos propios y ajenos. | Lista ordenada, detalle con ítems e historial, ajeno 404. | Unitaria mock | `PedidoConsultaServiceTest` (3 casos) |
| CP-US12-09 | RNF-13 | Historial muestra estado y guía | Pedidos con y sin pedidos. | Tarjetas con estado y enlace; vacío con invitación al catálogo. | Frontend | `mis-pedidos.spec.ts` (2 casos) |
| CP-US12-10 | VAL | Detalle con línea de tiempo | Pedido `ENVIADO` con historial. | Guía, 4 pasos con actual resaltado, productos y totales. | Frontend | `pedido-detalle.spec.ts` |
| CP-US12-11 | VAL | Servicio y tile de pedidos | Cliente autenticado. | Endpoints correctos; tile enlaza a `/cliente/pedidos`. | Frontend | `pedidos.service.spec.ts`, `cliente-inicio.spec.ts` |
| CP-US12-14 | VAL | Vendedor ve solo sus pedidos | Pedido con líneas propias y ajenas. | Lista y detalle solo con líneas propias, dirección completa e historial; ajeno 404. | Unitaria mock | `PedidoVendedorServiceTest`, `PedidoVendedorControllerTest` |
| CP-US12-15 | VAL | Páginas del vendedor | Vendedor autenticado. | Lista con estado y enlace, detalle con dirección e historial. | Frontend | `pedidos-vendedor.spec.ts`, `pedido-vendedor-detalle.spec.ts`, `pedidos-vendedor.service.spec.ts` |
| CP-US12-12 | RNF-13 | Trazabilidad del ciclo de vida | Pedidos con historial completo. | Transiciones y guías registradas sin inconsistencias. | Manual | Pendiente de medición en US-13 |
| CP-US12-13 | RNF-23 | Interop SMTP real | Credenciales Gmail válidas. | Correo HTML recibido con guía. | Manual | Pendiente de prueba con contraseña de aplicación en US-13 |
