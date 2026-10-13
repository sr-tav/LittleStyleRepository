# US-10: Carrito de compras y coordinación del checkout

## Objetivo y alcance

US-10 pertenece al Proceso 2 de LittleStyle: gestión del catálogo, compras, pedidos e inventario. Coordina la selección de productos, disponibilidad, perfil infantil, dirección de entrega y resumen previo al inicio del pago. El flujo termina al crear el pedido en estado pendiente de pago y entregar sus datos al inicio de pago de US-11.

El pago, las interfaces UI-13/UI-14/UI-15, Wompi, las confirmaciones del proveedor y su webhook están fuera de US-10. No se implementan impuestos, descuentos ni cupones. El total de la compra se compone únicamente del subtotal de productos más el costo de envío.

**Estado documentado:** KAN-187 a KAN-191 implementados. Ni el carrito ni la creación del pedido descuentan o reservan stock. La lógica backend para confirmar el pago bloquea el pedido y las prendas, valida todas las líneas antes de descontar, cambia el estado a `PAGADO` si hay existencias y devuelve el detalle de prendas/tallas insuficientes si no las hay; en ese caso cancela el pedido sin descuento parcial. Esa lógica todavía no está conectada al endpoint/webhook de una pasarela ni a una interfaz, integración que corresponde a US-11. Los pedidos pendientes vencidos se cancelan sin modificar existencias. El resultado de las pruebas se registra en `casos-de-prueba.md`.

## Actor e historia de usuario

**Actor principal:** cliente autenticado con rol `CLIENTE`.

**Historia:** como cliente, quiero revisar los productos que seleccioné, validar su disponibilidad y compatibilidad con el perfil infantil, ingresar la dirección de entrega y consultar el total antes de continuar al pago, para coordinar mi compra.

Las rutas de cliente requieren autenticación y el rol `CLIENTE`. Un recurso de otro cliente debe tratarse como no encontrado (404).

## Trazabilidad

| Elemento | Relación con US-10 |
|---|---|
| UI-10 | Carrito y edición de productos seleccionados (KAN-188). |
| UI-11 | Formulario de destinatario, dirección, código postal, departamento, municipio y teléfono (KAN-190). |
| UI-12 | Resumen de productos, dirección, subtotal, envío y total (KAN-190). |
| RNF-18 | Operabilidad del flujo de compra; objetivo de abandono menor al 30 %. No se afirma medición de este indicador. |
| RNF-10 | Acceso restringido por rol `CLIENTE` y recursos aislados por propietario. |
| ADR-15 | Facade coordina validación y cálculo del checkout. |
| ADR-13 | Contratos de entrada y respuesta mediante DTOs. |
| ADR-17 | Seguridad aplicada por las rutas `/api/cliente/**`. |
| US-11 / ADR-14 | Integración de pago y pasarela. El descuento de stock se ejecuta únicamente al aprobarse el pago; la integración HTTP y su interfaz pertenecen a US-11. |

## Reglas funcionales

### KAN-187 — Facade y resumen

`CheckoutFacade.calcularResumen` coordina la obtención de un perfil infantil propio a través de `PerfilInfantilService`, la búsqueda de prendas activas, la validación de talla y stock actual, la verificación de compatibilidad textil y el cálculo del resumen. No accede al repositorio de perfiles.

- El identificador de perfil del resumen representa el perfil activo seleccionado por el cliente en la interfaz; el servidor verifica que pertenezca al usuario autenticado.
- Solo se consideran prendas en estado `ACTIVA`. Una prenda inexistente o inactiva responde como no encontrada.
- La talla debe existir para la prenda; se ignoran diferencias de mayúsculas y minúsculas en su búsqueda.
- Se agregan las cantidades repetidas de una misma prenda y talla para validar que el conjunto no exceda el stock existente.
- La evaluación de alergias reutiliza `FiltroAlergia` y sus reglas de exclusión compartidas con recomendaciones. Una evaluación marcada `EXCLUIDA` impide resumir la prenda; una advertencia no la excluye. Las reglas textiles se mantienen en `FiltroAlergiaEstandar`.
- `subtotal` es la suma de precio unitario por cantidad. En checkout `total = subtotal + costoEnvio`, con tarifa determinada por departamento y municipio; no hay otros conceptos.
- La respuesta lleva líneas de producto, talla, cantidad, precio unitario, subtotal de línea, subtotal general, envío y total.
- Esta validación no reserva ni descuenta unidades. El carrito y el pedido pendiente no apartan stock; el descuento atómico se realiza al aprobar el pago para impedir que dos clientes obtengan las mismas unidades.

**SUPUESTO aislado/configurable:** se conserva el cargo de envío por pedido acordado para el checkout: Armenia `$7.000`; Buenavista, Calarcá, Circasia, Córdoba, Filandia, Génova, La Tebaida, Montenegro, Pijao, Quimbaya y Salento (Quindío) `$10.000`; los demás destinos `$15.000`. La lista de municipios de la tarifa intermedia es configurable y su valor predeterminado corresponde a esos once municipios. Esta es una tabla comercial simplificada de LittleStyle, no una tarifa calculada en tiempo real ni una reproducción exacta de las tarifas de Inter Rapidísimo o Servientrega. Se configura mediante `app.checkout.costo-envio-armenia`, `app.checkout.costo-envio-quindio`, `app.checkout.costo-envio-otras-ciudades` (variables `CHECKOUT_COSTO_ENVIO_ARMENIA`, `CHECKOUT_COSTO_ENVIO_QUINDIO`, `CHECKOUT_COSTO_ENVIO_OTRAS_CIUDADES`) y `app.checkout.municipios-quindio` / `CHECKOUT_MUNICIPIOS_QUINDIO`. Antes de conocer la dirección, el carrito muestra que envío y total están pendientes de calcular; con carrito vacío ambos son cero.

#### Revisión de tarifas con transportadoras

Se volvieron a consultar los cotizadores oficiales de Inter Rapidísimo y Servientrega el **9 de octubre de 2026**. Para comparar rutas se usó Armenia (Quindío) como origen y como destinos Armenia, Calarcá, Pereira y Bogotá. Se cotizó un envío nacional de una pieza, entrega en dirección, peso facturable de 1 kg y valor declarado de `$100.000`; en ambos se tomó como referencia un paquete de `20 × 15 × 5 cm`. El formulario público de Inter Rapidísimo calcula el peso volumétrico como `(ancho × alto × largo) / 6.000` y lo redondea hacia arriba, por lo que ese tamaño da 1 kg facturable; el cotizador se consultó con ese peso. Se seleccionó Mensajería estándar; su respuesta separa transporte de prima de seguro y el total mostrado por el cotizador suma ambos. Servientrega se consultó como paquete, producto Mercancía Premier, servicio normal/terrestre; su total contiene flete y seguro. Los valores son cotizaciones puntuales, no contratos ni tarifas garantizadas.

| Destino | Inter Rapidísimo: transporte + prima = total | Servientrega: flete + seguro = total | Cargo de checkout que se conserva |
|---|---:|---:|---:|
| Armenia (Quindío) | `$7.900 + $2.000 = $9.900` | `$7.800 + $1.000 = $8.800` | `$7.000` |
| Calarcá (Quindío) | `$11.600 + $2.000 = $13.600` | `$7.800 + $1.000 = $8.800` | `$10.000` |
| Pereira (Risaralda) | `$11.600 + $2.000 = $13.600` | `$11.500 + $1.000 = $12.500` | `$15.000` |
| Bogotá (Cundinamarca) | `$17.600 + $2.000 = $19.600` | `$17.550 + $1.000 = $18.550` | `$15.000` |

**Lectura y límite de la comparación:** las cotizaciones varían incluso entre municipios cercanos y según transportadora, servicio, peso/volumen, valor declarado y fecha. Por eso los cargos `$7.000 / $10.000 / $15.000` se mantienen como valores de checkout por zona solicitados para esta historia, pero no deben describirse como los precios exactos publicados por esas empresas. En esta muestra, el cargo queda por debajo de ambas cotizaciones para Armenia y Bogotá, por encima de ambas para Pereira y entre las dos cotizaciones para Calarcá. US-10 no consulta las APIs de transportadoras ni define quién asume diferencias entre el cargo cobrado y el flete real; esa política debe confirmarse antes de producción. Si se requiere trasladar el costo real al cliente, habrá que cotizar con origen, destino y datos del paquete o definir una tabla comercial revisada.

**Fuentes primarias consultadas:**

- [Inter Rapidísimo — Cotizador de envíos](https://servicios.interrapidisimo.com/Cotizador/vista/). Interfaz oficial; la comparación tomó Mensajería estándar, 1 kg facturable, `$100.000` declarados y entrega en dirección. La respuesta separa `Precio.Valor` (transporte) y `Precio.ValorPrimaSeguro`.
- [Servientrega — Cotizador](https://www.servientrega.com/wps/portal/cotizador), que carga el [formulario de cotización de mercancías](https://mobile.servientrega.com/WebSitePortal/cotizador.html). Se usaron las ciudades sugeridas por el formulario, peso, dimensiones y valor declarado indicados arriba; la respuesta expone flete, seguro y tarifa total.
- [Servientrega — tarifas y precios](https://www.servientrega.com/wps/portal/tarifas). La página publica tarifarios por ciudades y muestra archivos identificados como `08_2026`; son referencia adicional, no una matriz universal para todos los municipios ni el origen Armenia.

**Supuesto de comparación aislado en esta documentación:** el paquete, dimensiones y valor declarado descritos arriba se eligieron solo para reproducir una consulta comparable; no son datos guardados en pedidos ni una configuración de cobro. Las cotizaciones pueden cambiar y deben volver a verificarse con las transportadoras al escoger proveedor.

### KAN-188 — Carrito reactivo

El backend persiste un carrito por cliente en `Carrito`/`ItemCarrito`; define `GET /api/cliente/carrito`, `POST /api/cliente/carrito/items`, `PUT /api/cliente/carrito/items/{itemId}`, `DELETE /api/cliente/carrito/items/{itemId}` y `DELETE /api/cliente/carrito`. Agregar la misma prenda/talla incrementa la cantidad existente. Cambios de cantidad y agregados validan el stock actual; un recurso de otro cliente se busca dentro del carrito de la sesión y responde 404.

El servicio Angular usa signals para exponer ítems, cantidad, subtotal, envío y total, y consulta el servidor para cargar el estado autoritativo. LocalStorage guarda una copia por ID de cliente solo como caché inicial. `CarritoService`, instanciado en el componente raíz, elimina la clave de caché al detectar cierre de sesión o cambio de usuario; no se modificó el módulo auth.

UI-10 se accede desde la ruta protegida por `roleGuard` en `/cliente/carrito`, el inicio de cliente y la barra superior. El tile del inicio presenta badge de unidades. Las tarjetas de la vitrina y novedades muestran las tallas con stock desde el listado; las recomendaciones muestran directamente la talla sugerida disponible. Al pasar el cursor o enfocar el acceso rápido se ven la acción y las tallas al mismo tiempo; al elegir una talla se agrega inmediatamente, sin abrir el detalle ni volver a pulsar “Agregar al carrito”. Si se pulsa esa acción sin elegir talla, se solicita seleccionar una. La ficha del producto mantiene la opción de agregar desde el detalle. La página permite cambiar cantidad, eliminar un producto y vaciar el carrito.

Archivos incorporados para KAN-188: entidad/servicio/controlador y DTOs del carrito en `modules/catalogocompras/carrito`; configuración validada de envío en `config/CheckoutProperties.java`; modelos, servicio con signals, página UI-10 y rutas diferidas en `frontend/src/app/features/catalogoCompras`; integración de acceso desde ficha, inicio y barra; estilos en `frontend/src/styles/_carrito.scss`.

El carrito se abre como panel lateral desde el ícono de la barra superior y automáticamente después de agregar una prenda. Desde el panel se puede ir a la página completa del carrito. En cada línea del panel se permite aumentar/disminuir la cantidad o eliminar la prenda con confirmación; las operaciones actualizan el carrito persistido y el subtotal, y muestran el error del backend si no se completan. El indicador informa que los artículos no están reservados. La respuesta paginada de catálogo añade `tallasDisponibles`, con nombres de talla que tienen stock positivo; no entrega existencias exactas.

**SUPUESTO aislado/documentado:** una sola cesta por cliente, con la combinación prenda/talla única y cantidades repetidas agregadas. Agregar un producto no bloquea unidades para ese cliente. Varios clientes pueden incluir en sus carritos cantidades que, sumadas, superen el stock físico; al consultar, modificar o confirmar el pedido, cada carrito se contrasta con el stock actual y puede marcarse como no disponible. El carrito conserva el artículo para que el cliente pueda reducir su cantidad o eliminarlo. El envío se muestra desde la misma propiedad configurable del Facade; un carrito vacío tiene total cero.

### KAN-189 — Disponibilidad basada en stock físico

No se generan reservas al agregar, cambiar o eliminar líneas del carrito ni al crear un pedido pendiente. Al confirmar una aprobación de pago, el servicio backend bloquea el pedido y las prendas en orden estable, valida todas las cantidades y descuenta el stock en la misma transacción solo si todas las líneas están disponibles. Si dos pagos compiten por las últimas unidades, solo el primero que confirma con stock disponible lo obtiene; el siguiente obtiene un resultado `STOCK_INSUFICIENTE`, con nombre, talla, cantidad solicitada y stock disponible, y su pedido se cancela sin descontar ninguna otra línea. Las líneas de carrito de otros clientes no apartan stock y se marcan no disponibles cuando ya no alcanzan las existencias. El aviso “Los artículos no están reservados. ¡No los dejes escapar!” se presenta en el panel lateral y en la página del carrito. La conexión de este servicio con la confirmación verificada de la pasarela y la interfaz que informe al cliente corresponden a US-11. Si el proveedor ya cobró cuando se detecta falta de stock, US-11 debe ejecutar el reembolso antes de ofrecer retomar la compra; el pedido cancelado no se puede reabrir.

**Decisión funcional:** no existe un plazo de reserva en US-10. Columnas de vencimiento heredadas de la implementación anterior pueden permanecer físicamente en bases de datos existentes, pero dejan de mapearse y no afectan la disponibilidad; su eliminación requiere una migración posterior no destructiva.

### KAN-190 — Dirección y resumen previo al pago

UI-11 en `/cliente/checkout` permite usar o editar la dirección guardada para la cuenta del cliente. Captura destinatario, dirección, complemento opcional (apartamento, piso, interior u otras indicaciones), código postal colombiano de seis dígitos, departamento, municipio y teléfono celular colombiano `3XXXXXXXXX`; departamento y municipio se eligen desde el catálogo completo local de municipios colombianos, y el municipio disponible depende del departamento seleccionado. Al cambiar de departamento se limpia el municipio anterior. Al continuar, guarda o actualiza esa única dirección asociada a la cuenta. La misma dirección se consulta y edita desde “Mi cuenta”, en la sección “Dirección de envío”, usando los mismos endpoints `/api/cliente/checkout/direccion`; los cambios hechos desde cualquiera de las dos pantallas quedan disponibles en la otra. El pedido conserva una copia de los datos de envío como snapshot, independiente de futuras ediciones de la dirección guardada. Checkout usa automáticamente el perfil infantil activo de la cuenta para validar alergias; no vuelve a pedir que se seleccione. Al continuar, Angular envía la dirección y el perfil a `POST /api/cliente/checkout/resumen`; backend toma los productos del carrito persistido y vuelve a validar perfil, compatibilidad y stock físico, devolviendo los totales calculados. Tras revisar UI-12, “Confirmar pedido” crea el pedido pendiente de pago; el servidor usa las líneas del carrito persistido, no líneas enviadas por el navegador.

El catálogo de ubicaciones es estático y local, derivado de `RafaelRamosR/dane-codigos-municipios` (datos DANE publicados en 2023, licencia MIT); su atribución y licencia se conservan junto al código fuente en `frontend/src/app/features/catalogoCompras/data/LICENSE-municipios-colombia.txt`. La interfaz no depende de un servicio externo para cargar departamentos o municipios.

UI-12 muestra líneas del resumen (producto, talla, cantidad, precio unitario/subtotal), dirección, subtotal, tarifa de envío correspondiente al departamento y municipio y total. El costo de envío se calcula una vez por pedido, independientemente del número de prendas. El stepper es “Información de envío → Resumen → Pago”. Antes de confirmar se puede regresar a editar dirección. “Confirmar pedido” persiste el snapshot de dirección, perfil, líneas y totales y devuelve el identificador, estado y datos que consumirá US-11. Durante la solicitud el botón se deshabilita para evitar confirmaciones repetidas. Al crearse el pedido, la pantalla muestra una confirmación accesible con su número y el estado pendiente de pago. La acción “Pagar” permanece deshabilitada hasta integrar el flujo de pago de US-11; US-10 no inicia pagos ni integra Wompi.

Si al confirmar el servidor detecta que el stock cambió, el checkout actualiza el carrito e identifica por nombre y talla cada prenda agotada. El cliente puede seguir comprando o confirmar explícitamente el pedido sin esas prendas; en ese caso se quitan del carrito, se recalculan los totales y solo se crea el pedido si aún quedan artículos disponibles.

**SUPUESTOS aislados:** se conserva el cargo de envío simplificado por zona que se describe en KAN-187; la comparación puntual con transportadoras y sus límites están registrados en la sección “Revisión de tarifas con transportadoras”. Se mantiene una sola dirección editable por cuenta, sin libreta de varias direcciones ni campo específico para barrio. El código postal es obligatorio y debe tener seis dígitos numéricos, conforme al formato colombiano. El pedido conserva snapshots cifrados de dirección y detalle de producto para que cambios posteriores del catálogo, del perfil o de la dirección guardada no alteren el pedido confirmado. Al crear el pedido, las líneas salen del carrito, pero no se reservan ni descuentan unidades. El descuento ocurre al aprobar el pago; si el pedido se cancela o vence antes del pago, no hay stock que reponer. No se recoge ni procesa información de pago en US-10.

### Ciclo de vida del pedido

El pedido se crea como `PENDIENTE_PAGO`. El modelo limita las transiciones a `PENDIENTE_PAGO → PAGADO → EN_PREPARACION → ENVIADO → ENTREGADO`; se permite pasar a `CANCELADO` desde `PENDIENTE_PAGO`, `PAGADO` o `EN_PREPARACION`. `ENTREGADO` y `CANCELADO` son estados terminales. Las transiciones inválidas responden HTTP 409. El checkout responde inicialmente `PENDIENTE_PAGO`; la integración de pago y las acciones para avanzar pedidos se implementan en las historias correspondientes.

El carrito y los pedidos `PENDIENTE_PAGO` no reservan inventario. La lógica backend de pago descuenta el stock atómicamente solo después de validar que todas las líneas están disponibles. Si el pedido sigue `PENDIENTE_PAGO` durante 30 minutos, un proceso programado lo cambia a `CANCELADO`; no se restituyen unidades porque no se habían descontado. El plazo se configura con `app.checkout.tiempo-expiracion-pedido` (`CHECKOUT_TIEMPO_EXPIRACION_PEDIDO`). La integración de esa lógica con el endpoint de pago, la pasarela y la notificación al cliente pertenece a US-11.
- Los datos personales de entrega (destinatario, dirección, complemento, código postal, departamento, municipio y teléfono) se cifran en la base de datos en la dirección guardada y en el snapshot del pedido, mediante el convertidor AES-GCM versionado existente y la clave de `app.crypto.perfil-key`. Las filas históricas creadas antes de este cambio en claro se leen por compatibilidad, pero requieren migración operativa para cifrarse en almacenamiento; no se afirma que se haya migrado información histórica productiva.
- La dirección predeterminada de la cuenta se persiste en `direcciones_cliente` y solo se consulta/actualiza con la identidad autenticada del cliente. Al desactivar la cuenta se elimina esa dirección guardada; los snapshots de pedidos históricos tienen ciclo de conservación independiente y requieren política de retención.

## Contratos backend

El endpoint HTTP del resumen invoca el Facade y valida la solicitud con Bean Validation. No crea pedidos ni procesa pagos.

| Contrato | Campos |
|---|---|
| `ResumenCheckoutRequest` | `perfilInfantilId` y dirección (código postal, departamento, municipio y demás datos de entrega) para calcular tarifa; las líneas se obtienen del carrito del servidor. |
| `ResumenCheckoutResponse` | `perfilInfantilId`, `items`, `subtotal`, `costoEnvio`, `total`. |
| `LineaResumenCheckoutResponse` | `prendaId`, `nombre`, `talla`, `cantidad`, `precioUnitario`, `subtotal`. |
| `POST /api/cliente/checkout/resumen` | Recibe el perfil, toma líneas del carrito persistido y devuelve el resumen validado; requiere rol `CLIENTE`. |
| `CrearPedidoRequest` | `perfilInfantilId` y `direccion` (destinatario, dirección, complemento opcional, código postal de seis dígitos, departamento, municipio y celular colombiano). Las líneas y precios provienen del carrito del servidor. |
| `PedidoCreadoResponse` | `pedidoId`, `estado` (`PENDIENTE_PAGO`), perfil, dirección, líneas confirmadas, subtotales y total. |
| `GET /api/cliente/checkout/direccion` | Devuelve la dirección guardada del cliente autenticado o `guardada=false`; no acepta IDs de otros clientes. |
| `PUT /api/cliente/checkout/direccion` | Crea o actualiza la única dirección asociada a la cuenta autenticada, incluyendo complemento opcional. |
| `POST /api/cliente/checkout/pedidos` | Crea el pedido `PENDIENTE_PAGO` sin reservar ni descontar stock y responde 201 con los datos que consumirá US-11. No inicia el pago. |

## Estado y límites del flujo

| Momento | Resultado |
|---|---|
| KAN-187, resumen | Valida el perfil, compatibilidad de alergias, existencia de producto/talla y stock físico actual; calcula valores. No persiste pedido, reserva ni descuenta stock. |
| KAN-188, carrito | Carrito persistido por cliente; servicios y página cubiertos por pruebas unitarias, Vitest e integración de propiedad. |
| KAN-189, disponibilidad | El carrito y el pedido pendiente no reservan unidades; se comparan las líneas con stock físico actual. El stock se descuenta atómicamente al aprobar el pago; esa operación está implementada en el servicio backend, pero todavía no está expuesta por un endpoint de cliente. |
| KAN-190, dirección/resumen/pedido | UI-11/12 y endpoint de resumen; “Confirmar pedido” crea el pedido pendiente sin afectar stock. El botón de pago permanece deshabilitado hasta integrar US-11. |
| KAN-191, pruebas | Pruebas unitarias del Facade y disponibilidad, integración de seguridad/propiedad/stock y Vitest de servicios y páginas; ver resultado actualizado en `casos-de-prueba.md`. |
| US-11 | Consume los datos del pedido pendiente para iniciar y confirmar el pago. Debe conectar la pasarela con la validación/descuento de stock ya preparada en backend y mostrar el aviso de falta de existencias, con opciones para continuar sin las prendas agotadas o volver al catálogo. No forma parte del alcance de US-10. |

## Archivos y estado de pruebas

Los casos, clases de prueba y resultados efectivamente ejecutados se registran en `casos-de-prueba.md`. El indicador de abandono de RNF-18 no se ha medido en producción; las pruebas automatizadas verifican comportamiento, no tasas de conversión.
