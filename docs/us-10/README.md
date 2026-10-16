# Documentación de US-10

Esta carpeta reúne la descripción funcional, los casos de prueba y la evidencia automatizada del carrito de compras y la coordinación del checkout en LittleStyle.

## Qué se implementó

- **Carrito persistido por cliente:** backend con entidad, servicio, repositorio, DTOs y endpoints bajo `/api/cliente/carrito`; el carrito se protege por usuario y evita acceso a recursos ajenos.
- **Carrito reactivo en Angular:** servicio con signals, cantidad, subtotal, envío y total; integración con la UI del cliente y con el badge de la vista principal.
- **Panel lateral del carrito:** se abre al agregar una prenda o desde el ícono de la barra superior; avisa que agregar al carrito no reserva unidades y enlaza a la página completa.
- **Validación del checkout:** facade que valida stock, perfil infantil activo, incompatibilidades por alergias textiles, suma de cantidades repetidas y cálculo de subtotal/total.
- **Disponibilidad del carrito:** iniciar un pago no crea un pedido, vacía el carrito ni reserva existencias. Al aprobarse el pago se crea el pedido como `PAGADO`, se descuenta el stock atómicamente y se retiran del carrito las cantidades compradas; si ya no alcanza el stock, el pedido se cancela para reembolso y el carrito se conserva.
- **Dirección y resumen previo al pago:** dirección guardada y asociada a la cuenta, opción de usarla o editarla, complemento opcional, código postal de seis dígitos, departamento, municipio, celular colombiano y resumen con subtotal, tarifa configurable y total.
- **Dirección compartida con Mi cuenta:** la dirección capturada o editada desde checkout aparece en la sección “Dirección de envío” de la cuenta y se puede actualizar desde allí con los mismos endpoints.
- **Ubicación colombiana:** selects encadenados de los 33 departamentos y 1.123 municipios, cargados desde datos locales con atribución y licencia MIT.
- **Acceso rápido a tallas:** en vitrina, novedades y recomendaciones se ven la acción “Agregar al carrito” y las tallas disponibles directamente al pasar el cursor o enfocar la tarjeta; no se necesita abrir el detalle ni desplazar la página para encontrar las tallas.
- **Compra desde la ficha de la prenda:** muestra la cantidad disponible por talla, permite seleccionar unidades con controles `−/+` sin superar el stock y agrega al carrito la cantidad elegida. Si no hay existencias, deshabilita la compra e indica “En este momento no hay unidades disponibles”.
- **Compatibilidad del esquema de pedidos:** al iniciar la aplicación, se detecta si una tabla `pedidos` ya existente no tiene `fecha_actualizacion`; para H2 y MySQL/MariaDB se agrega la columna y se inicializa desde `fecha_creacion`, sin borrar pedidos.
- **Ciclo de vida del pedido:** el pedido se crea al aprobar el pago (`PENDIENTE_PAGO → PAGADO`) y luego avanza a `EN_PREPARACION → ENVIADO`, con cancelación permitida antes del despacho. Los pedidos históricos sin pagar se cancelan al vencer el plazo de 30 minutos; como no apartan stock, la expiración no repone existencias.

US-10 coordina el checkout y US-11 inicia Wompi. El servidor conserva una instantánea cifrada del checkout en el intento de pago; solo la aprobación verificada del webhook o la consulta de Wompi crea el pedido. Un pago rechazado o pendiente deja el pedido sin crear y conserva los productos en el carrito. No hay impuestos, descuentos ni cupones; el total sigue siendo subtotal + envío.

## Archivos de documentación

| Archivo | Contenido |
|---|---|
| [`descripcion-funcional.md`](descripcion-funcional.md) | Objetivo, alcance, reglas funcionales, supuestos configurables, trazabilidad, endpoints, estados y límites de la US-10. |
| [`casos-de-prueba.md`](casos-de-prueba.md) | Casos CP-US10, pruebas asociadas, resultados ejecutados y evidencias backend/frontend. |

## Evidencia registrada

La suite automatizada cubre checkout, estados y aprobación de pago: antes de la aprobación no hay pedido y el carrito permanece; al aprobar, se crea el pedido pagado, se descuenta stock y se quitan del carrito las cantidades adquiridas. Si falta stock, se crea un pedido cancelado para reembolso sin vaciar el carrito.

La cantidad de pruebas y el resultado de la última ejecución completa se actualizan en [`casos-de-prueba.md`](casos-de-prueba.md).

## Configuración y alcance pendiente

- El envío conserva los cargos por zona acordados: Armenia $7.000, otros municipios configurados del Quindío $10.000 y otras ciudades $15.000. Son valores comerciales simplificados de LittleStyle, no tarifas en tiempo real de las transportadoras. La comparación reciente con Inter Rapidísimo y Servientrega, con ciudades, entradas, resultados y enlaces oficiales, está en “Revisión de tarifas con transportadoras” de `descripcion-funcional.md`.
- La dirección de envío exige código postal colombiano de seis dígitos, departamento, municipio, dirección y destinatario; dirección guardada y snapshot del pedido se cifran.
- Departamento y municipio se eligen del catálogo estático local. El listado de catálogo muestra tallas disponibles sin cantidades; la ficha muestra existencias por talla para limitar la cantidad seleccionable.
- Ni el carrito ni un intento de pago previo a la confirmación reservan existencias. El descuento ocurre al aprobar el pago, con validación atómica; si ya no alcanza, se crea un pedido cancelado para reembolso y se conserva el carrito.
- En H2 local, la columna `pedidos.fecha_actualizacion` faltante se migra automáticamente. En producción, donde `ddl-auto=validate`, la columna debe existir antes de arrancar: aplicar `ALTER TABLE pedidos ADD COLUMN fecha_actualizacion DATETIME(6) NULL`, completar nulos con `fecha_creacion` y luego cambiarla a `NOT NULL`. La migración de aplicación no sustituye este paso previo a la validación del esquema.
- El pedido se crea dentro de la transacción de aprobación, transita inmediatamente de `PENDIENTE_PAGO` a `PAGADO` y queda visible en estado pagado; los registros históricos `PENDIENTE_PAGO` conservan la expiración de 30 minutos. El modelo valida las transiciones posteriores a `EN_PREPARACION`, `ENVIADO` y `CANCELADO`.
- Las columnas antiguas de vencimiento de reserva, si ya existen en una base de datos, son obsoletas e inertes; su retiro requiere una migración posterior.
- Los datos de dirección guardados y los snapshots nuevos de pedido se cifran en persistencia con el convertidor AES-GCM existente; las filas históricas creadas en claro antes de este cambio requieren migración operativa.

La nueva persistencia requiere aplicar la migración SQL manual antes de desplegar con `ddl-auto=validate`; se conserva compatibilidad con transacciones y pedidos históricos.
