# Documentación de US-10

Esta carpeta reúne la descripción funcional, los casos de prueba y la evidencia automatizada del carrito de compras y la coordinación del checkout en LittleStyle.

## Qué se implementó

- **Carrito persistido por cliente:** backend con entidad, servicio, repositorio, DTOs y endpoints bajo `/api/cliente/carrito`; el carrito se protege por usuario y evita acceso a recursos ajenos.
- **Carrito reactivo en Angular:** servicio con signals, cantidad, subtotal, envío y total; integración con la UI del cliente y con el badge de la vista principal.
- **Panel lateral del carrito:** se abre al agregar una prenda o desde el ícono de la barra superior; avisa que agregar al carrito no reserva unidades y enlaza a la página completa.
- **Validación del checkout:** facade que valida stock, perfil infantil activo, incompatibilidades por alergias textiles, suma de cantidades repetidas y cálculo de subtotal/total.
- **Disponibilidad del carrito:** los carritos y pedidos pendientes no reservan ni descuentan existencias. El stock se valida y descuenta atómicamente cuando se aprueba el pago; si ya no alcanza, ese pago debe rechazarse.
- **Dirección y resumen previo al pago:** dirección guardada y asociada a la cuenta, opción de usarla o editarla, complemento opcional, código postal de seis dígitos, departamento, municipio, celular colombiano y resumen con subtotal, tarifa configurable y total.
- **Dirección compartida con Mi cuenta:** la dirección capturada o editada desde checkout aparece en la sección “Dirección de envío” de la cuenta y se puede actualizar desde allí con los mismos endpoints.
- **Ubicación colombiana:** selects encadenados de los 33 departamentos y 1.123 municipios, cargados desde datos locales con atribución y licencia MIT.
- **Acceso rápido a tallas:** en vitrina, novedades y recomendaciones se ven la acción “Agregar al carrito” y las tallas disponibles directamente al pasar el cursor o enfocar la tarjeta; no se necesita abrir el detalle ni desplazar la página para encontrar las tallas.
- **Ciclo de vida del pedido:** `PENDIENTE_PAGO → PAGADO → EN_PREPARACION → ENVIADO → ENTREGADO`, con cancelación permitida antes del despacho. Los pedidos sin pagar se cancelan al vencer el plazo de 30 minutos; como no apartan stock, la expiración no repone existencias.

La integración del pago real, la pasarela, la confirmación del proveedor y el webhook de Wompi corresponden a US-11. US-10 crea el pedido pendiente, sin iniciar el pago ni descontar stock. El servicio backend de coordinación de inventario al confirmar el pago aún no está expuesto a un endpoint ni integrado con la interfaz; su conexión y el aviso al cliente cuando falte stock quedan para US-11. No hay impuestos, descuentos ni cupones; el total sigue siendo subtotal + envío.

## Archivos de documentación

| Archivo | Contenido |
|---|---|
| [`descripcion-funcional.md`](descripcion-funcional.md) | Objetivo, alcance, reglas funcionales, supuestos configurables, trazabilidad, endpoints, estados y límites de la US-10. |
| [`casos-de-prueba.md`](casos-de-prueba.md) | Casos CP-US10, pruebas asociadas, resultados ejecutados y evidencias backend/frontend. |

## Evidencia registrada

La suite automatizada cubre checkout, estados, expiración y la regla de competencia de inventario en el servicio backend: el primer pago confirmado descuenta stock y el siguiente recibe un resultado con el detalle de las prendas/tallas insuficientes, sin descuento parcial. El checkout bloquea el botón durante la solicitud para evitar pedidos repetidos y muestra el número del pedido creado. El endpoint conectado a la pasarela y el aviso en pantalla durante el pago siguen pendientes de US-11.

La cantidad de pruebas y el resultado de la última ejecución completa se actualizan en [`casos-de-prueba.md`](casos-de-prueba.md).

## Configuración y alcance pendiente

- El envío conserva los cargos por zona acordados: Armenia $7.000, otros municipios configurados del Quindío $10.000 y otras ciudades $15.000. Son valores comerciales simplificados de LittleStyle, no tarifas en tiempo real de las transportadoras. La comparación reciente con Inter Rapidísimo y Servientrega, con ciudades, entradas, resultados y enlaces oficiales, está en “Revisión de tarifas con transportadoras” de `descripcion-funcional.md`.
- La dirección de envío exige código postal colombiano de seis dígitos, departamento, municipio, dirección y destinatario; dirección guardada y snapshot del pedido se cifran.
- Departamento y municipio se eligen del catálogo estático local. Los nombres de talla disponibles se incluyen en el listado del catálogo sin exponer cantidades de stock.
- Ni el carrito ni el pedido `PENDIENTE_PAGO` reservan existencias. El descuento ocurre al aprobar el pago, con validación atómica; si dos pagos compiten por la última unidad, solo el primero que logra confirmarse puede descontarla. El segundo pedido se cancela por falta de stock. La integración HTTP, el aviso de prendas/tallas agotadas y la opción de continuar sin ellas o volver al catálogo deben completarse en US-11.
- El modelo define y valida las transiciones entre `PENDIENTE_PAGO`, `PAGADO`, `EN_PREPARACION`, `ENVIADO`, `ENTREGADO` y `CANCELADO`. Los endpoints de pago y actualización operativa corresponden a las historias siguientes.
- Las columnas antiguas de vencimiento de reserva, si ya existen en una base de datos, son obsoletas e inertes; su retiro requiere una migración posterior.
- Los datos de dirección guardados y los snapshots nuevos de pedido se cifran en persistencia con el convertidor AES-GCM existente; las filas históricas creadas en claro antes de este cambio requieren migración operativa.

Estos límites distinguen el modelo de estados y la expiración de pedidos pendientes ya implementados del pago real y la coordinación con la pasarela, que corresponden a US-11.
