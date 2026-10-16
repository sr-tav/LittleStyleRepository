# Casos de prueba — US-12 Seguimiento de Estados y Correo

> Trazabilidad: cada CP mapea a un RNF y a una prueba automatizada. Las
> mediciones de tasas y tiempos van en US-13.

## 1. Matriz de casos

| ID | RNF | Descripción | Precondiciones | Resultado esperado | Tipo | Evidencia |
|---|---|---|---|---|---|---|
| CP-US12-01 | RNF-13 | Flujo normal termina en ENVIADO terminal | Pedido en `PENDIENTE_PAGO`. | `PAGADO → REGISTRADO → EN_PREPARACION → ENVIADO`; desde `ENVIADO` nada avanza. | Unitaria | `EstadoPedidoTest.permiteElFlujoNormalYTerminaEnEnviado`, `pagadoSoloRegistraYRegistradoSoloPrepara` |
| CP-US12-02 | RNF-13 | Cancelación y saltos inválidos | Pedido en varios estados. | Cancelar antes del despacho; saltos y reapertura → 409. | Unitaria | `EstadoPedidoTest` (3 casos) |
| CP-US12-03 | RNF-13 | Avanzar guarda historial y avisa al enviar | Pedido en `EN_PREPARACION`. | Estado `ENVIADO` con guía, fila de historial y evento publicado; avance con bloqueo. | Unitaria mock | `PedidoEstadoServiceTest.avanzarGuardaHistorialYPublicaEventoAlEnviar` |
| CP-US12-04 | RNF-13 | ENVIADO sin guía es 400 | Pedido en `EN_PREPARACION`. | 400 asociado a `numeroGuia`. | Unitaria | `PedidoEstadoServiceTest.enviarSinGuiaDevuelve400` |
| CP-US12-05 | RNF-13 | Admin avanza, resto no | Pedido existente. | 200 con guía; 404 inexistente; 409 transición inválida; `PAGADO` a mano → 409. | Controller | `PedidoAdminControllerTest` (3 casos) |
| CP-US12-06 | RNF-23 | Sin credenciales no se envía | Sin `MAIL_USER`/`MAIL_PASS`. | Se omite el envío sin error. | Unitaria | `CorreoServiceTest.sinCredencialesNoEnviaNada` |
| CP-US12-07 | RNF-23 | Correo HTML con guía | Credenciales presentes. | Se envía con asunto y guía. | Unitaria | `CorreoServiceTest.conCredencialesEnviaElHtml` |
| CP-US12-08 | RNF-13 | Cliente lista y detalla lo propio | Pedidos propios y ajenos. | Lista ordenada, detalle con ítems e historial, ajeno 404. | Unitaria mock | `PedidoConsultaServiceTest` (3 casos) |
| CP-US12-09 | RNF-13 | Lista con fecha, total y guía | Pedido con guía. | Tarjeta con estado, fecha, total y guía; vacío con invitación al catálogo. | Frontend | `mis-pedidos.spec.ts` (2 casos) |
| CP-US12-10 | RNF-13 | Detalle con línea de tiempo e historial | Pedido `ENVIADO` con historial. | Guía, 5 pasos con actual resaltado, productos, totales, historial con fechas, botón Actualizar. | Frontend | `pedido-detalle.spec.ts` (6 casos) |
| CP-US12-11 | RNF-13 | Servicio y tile de pedidos | Cliente autenticado. | Endpoints correctos; tile enlaza a `/cliente/pedidos`; cancelar pre-pago. | Frontend | `pedidos.service.spec.ts` (3 casos), `cliente-inicio.spec.ts` |
| CP-US12-12 | RNF-13 | Trazabilidad del ciclo de vida | Pedidos con historial completo. | Transiciones y guías registradas sin inconsistencias. | Manual | Pendiente de medición en US-13 |
| CP-US12-13 | RNF-23 | Interop SMTP real | Credenciales Gmail válidas. | Correo HTML recibido con guía. | Manual | Pendiente de prueba con contraseña de aplicación en US-13 |
| CP-US12-14 | RNF-13 | Vendedor ve solo sus pedidos | Pedido con líneas propias y ajenas. | Lista y detalle solo con líneas propias, dirección completa e historial; ajeno 404. | Unitaria mock | `PedidoVendedorServiceTest`, `PedidoVendedorControllerTest` |
| CP-US12-15 | RNF-13 | Páginas del vendedor con avance | Vendedor autenticado. | Lista con estado y enlace; detalle avanza a preparación y a enviado con guía. | Frontend | `pedidos-vendedor.spec.ts`, `pedido-vendedor-detalle.spec.ts` (3 casos), `pedidos-vendedor.service.spec.ts` (3 casos) |
| CP-US12-16 | RNF-23 | Correo al crear el pedido | Pedido creado. | Evento + correo con total e ítems. | Unitaria | `NotificacionPedidoListenerTest.creacionYPagoAvisanAlCliente` |
| CP-US12-17 | RNF-23 | Correo al confirmar el pago, sin duplicar | Pago aprobado. | Un correo de pago + uno de registrado; el genérico de `PAGADO` no se publica. | Unitaria | `NotificacionPedidoListenerTest.creacionYPagoAvisanAlCliente`, `PedidoPagoServiceTest.confirmaPagoRegistraYPublicaUnSoloCorreoDePago` |
| CP-US12-17b | RNF-23 | Correo al rechazar el pago | Wompi rechaza (`DECLINED`). | Estado `RECHAZADA`, evento publicado y correo con referencia, sin pedido ni cargo. | Unitaria | `PagoServiceTest.rechazoDeWompiPublicaEventoParaAvisarAlCliente`, `NotificacionPedidoListenerTest.rechazoAvisaAlClienteSinPedido`, `CorreoServiceTest.avisaElPagoRechazadoConLaReferencia` |
| CP-US12-18 | RNF-13 | Correo en cada cambio de estado, sin duplicar ENVIADO | Transición a EN_PREPARACION y a ENVIADO. | Genérico solo en la primera; ENVIADO usa el de guía; etiqueta en español. | Unitaria | `NotificacionPedidoListenerTest` (3 casos) |
| CP-US12-19 | RNF-23 | Correo de alerta de stock al vendedor | Alerta emitida. | Correo con prenda, nivel y stocks. | Unitaria | `NotificacionPedidoListenerTest.alertaStockAvisaAlVendedor` |
| CP-US12-20 | RNF-13 | Pago deja el pedido REGISTRADO | Pedido en `PENDIENTE_PAGO` con stock. | Estado `REGISTRADO`, stock descontado, historial `PAGADO → REGISTRADO`. | Unitaria mock | `PedidoPagoServiceTest` (2 casos) |
| CP-US12-21 | RNF-13 | Vendedor avanza lo suyo; cancelar es 409 | Pedido con línea propia y ajena. | Delega a estados; ajeno 404; `CANCELADO` por vendedor 409. | Unitaria mock | `PedidoVendedorServiceTest.avanzarEstadoDePedidoPropioDelegaAlServicioDeEstados`, `avanzarEstadoDePedidoAjenoDevuelve404`, `PedidoVendedorControllerTest` (2 casos) |
| CP-US12-22 | RNF-13 | Cancelar con pago previo repone stock | Pedido `EN_PREPARACION` con 2 uds. | Estado `CANCELADO` y stock 5 → 7. | Unitaria mock | `PedidoEstadoServiceTest.cancelarConPagoPrevioReponeStock` |
| CP-US12-23 | RNF-13 | Cliente cancela solo antes del pago | Pedidos `PENDIENTE_PAGO` y `PAGADO`. | 200 el primero; 409 el segundo; ajeno 404. | Unitaria mock | `PedidoEstadoServiceTest.clienteSoloCancelaAntesDelPago`, `clienteNoCancelaPedidoPagado` |
| CP-US12-24 | RNF-23 | Reintentos y aviso al vendedor | Fallo transitorio; pago aprobado. | 3 intentos de envío; correo al vendedor con sus líneas. | Unitaria | `CorreoServiceTest.reintentaElEnvioAnteFalloTransitorio`, `avisaAlVendedorDelPedidoNuevo`, `NotificacionPedidoListenerTest.pagoAvisaAlVendedorConSusLineas` |
| CP-US12-25 | RNF-13 | Integración punta a punta con seguridad | Pedidos en varios estados. | Admin y vendedor avanzan, `PAGADO` manual 409, cliente cancela pre-pago, cancel repone stock. | Integración | `PedidoEstadoIntegrationTest` (5 casos) |
| CP-US12-26 | RNF-10 | Roles en endpoints de pedidos | Tokens de cada rol. | Cliente/vendedor/admin solo lo suyo; anónimo 401. | Integración | `PedidoEstadoIntegrationTest`, `CatalogoClienteIntegrationTest.vitrinaExigeRolCliente` |
