# US-10: Carrito de compras y coordinación del checkout

## Objetivo y alcance

US-10 pertenece al Proceso 2 de LittleStyle: gestión del catálogo, compras, pedidos e inventario. Coordina la selección de productos, disponibilidad, perfil infantil, dirección de entrega y resumen previo al pago. El pedido se crea únicamente tras la aprobación verificada del pago; iniciar o rechazar un pago no crea un pedido ni vacía el carrito.

La pasarela Wompi, sus interfaces UI-13/UI-14/UI-15, las confirmaciones del proveedor y su webhook se documentan en US-11. El checkout valida el estado autoritativo del carrito y entrega a la pasarela un snapshot cifrado de compra. No se implementan impuestos, descuentos ni cupones. El total de la compra se compone únicamente del subtotal de productos más el costo de envío.

**Estado documentado:** KAN-187 a KAN-191 implementados. Iniciar el pago no crea pedido, no vacía el carrito y no reserva stock. La aprobación verificada crea el pedido como `PAGADO`, valida todas las líneas y descuenta stock en una transacción; si no alcanza, el pedido se crea como `CANCELADO` para reembolso y el carrito se conserva. Los pedidos históricos `PENDIENTE_PAGO` aún se pueden expirar sin modificar existencias. El resultado de las pruebas se registra en `casos-de-prueba.md`.

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
- La variante `talla + color` debe existir para la prenda; se ignoran diferencias de mayúsculas y minúsculas en la talla. Si la prenda tiene varios colores y no se envía `color`, la API responde `400` con “Elige un color para esta talla”; si la combinación no existe, responde `400` con “La combinación de talla y color no está disponible para esta prenda”.
- Se agregan las cantidades repetidas de una misma prenda, talla y color para validar que el conjunto no exceda el stock de esa variante.
- La evaluación de alergias reutiliza `FiltroAlergia` y sus reglas de exclusión compartidas con recomendaciones. Una evaluación marcada `EXCLUIDA` impide resumir la prenda; una advertencia no la excluye. Las reglas textiles se mantienen en `FiltroAlergiaEstandar`.
- El bloqueo por alergias graves empieza en el carrito: `CarritoService.agregar` valida con `ValidadorAlergiaCompra` (misma evaluación `EXCLUIDA` compartida con el resumen) al agregar una prenda. Cuando la solicitud trae el `perfilInfantilId` activo, se evalúa solo contra ese perfil; si no lo trae, se evalúa contra todos los perfiles del cliente (defecto seguro para llamadas sin contexto). El rechazo responde HTTP 422 con código estable `PRENDA_EXCLUIDA_ALERGIA` y el ítem no entra al carrito; el mismo código quedó disponible para el rechazo del resumen. La página de compra distingue el bloqueo por código, no por texto del mensaje.
- `subtotal` es la suma de precio unitario por cantidad. En checkout `total = subtotal + costoEnvio`, con tarifa determinada por departamento y municipio; no hay otros conceptos.
- La respuesta lleva líneas de producto, talla, color, cantidad, precio unitario, subtotal de línea, subtotal general, envío y total.
- Esta validación no reserva ni descuenta unidades. El carrito y los intentos de pago no apartan stock; el descuento atómico se realiza al aprobar el pago para impedir que dos clientes obtengan las mismas unidades.

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

El backend persiste un carrito por cliente en `Carrito`/`ItemCarrito`; define `GET /api/cliente/carrito`, `POST /api/cliente/carrito/items`, `PUT /api/cliente/carrito/items/{itemId}`, `DELETE /api/cliente/carrito/items/{itemId}` y `DELETE /api/cliente/carrito`. Agregar la misma prenda, talla y color incrementa la cantidad existente; la unicidad es (`carrito_id`, `prenda_id`, `talla`, `color`). Cambios de cantidad y agregados validan el stock actual de la variante; un recurso de otro cliente se busca dentro del carrito de la sesión y responde 404.

El servicio Angular usa signals para exponer ítems, cantidad, subtotal, envío y total, y consulta el servidor para cargar el estado autoritativo. LocalStorage guarda una copia por ID de cliente solo como caché inicial. `CarritoService`, instanciado en el componente raíz, elimina la clave de caché al detectar cierre de sesión o cambio de usuario; no se modificó el módulo auth.

UI-10 se accede desde la ruta protegida por `roleGuard` en `/cliente/carrito`, el inicio de cliente y la barra superior. El tile del inicio presenta badge de unidades. Las tarjetas de prendas no muestran información al pasar el cursor; el cliente abre el detalle con “Ver” y allí elige color y talla antes de agregar al carrito. La página permite cambiar cantidad, eliminar un producto y vaciar el carrito.

Desde la ficha de la prenda, el cliente ve la cantidad disponible por variante (`talla + color`), selecciona el color y la talla, y ajusta las unidades con controles `−/+` sin superar las existencias indicadas. “Agregar al carrito” envía color, talla y cantidad. Cuando ninguna variante tiene unidades, el botón de compra se deshabilita y se muestra “En este momento no hay unidades disponibles”. El backend valida nuevamente el stock de la variante al agregar, por lo que un cambio concurrente de inventario no permite comprar más de lo disponible.

#### Compatibilidad de esquema para pedidos existentes

`PedidoFechaActualizacionSchemaMigrator` revisa al iniciar la aplicación si existe la tabla `pedidos` y si le falta `fecha_actualizacion`. En H2 y MySQL/MariaDB agrega la columna, copia `fecha_creacion` a los pedidos existentes y la establece como obligatoria; si la tabla aún no existe o la columna ya está, no modifica el esquema. La migración conserva los registros y es idempotente. Su prueba H2 ejecuta la migración dos veces y comprueba que la fecha se conserva. Como `ddl-auto=validate` verifica el esquema antes de ejecutar los `ApplicationRunner`, una base productiva configurada así debe actualizarse antes del arranque con:

```sql
ALTER TABLE pedidos ADD COLUMN fecha_actualizacion DATETIME(6) NULL;
UPDATE pedidos SET fecha_actualizacion = fecha_creacion WHERE fecha_actualizacion IS NULL;
ALTER TABLE pedidos MODIFY COLUMN fecha_actualizacion DATETIME(6) NOT NULL;
```

Archivos incorporados para KAN-188: entidad/servicio/controlador y DTOs del carrito en `modules/catalogocompras/carrito`; configuración validada de envío en `config/CheckoutProperties.java`; modelos, servicio con signals, página UI-10 y rutas diferidas en `frontend/src/app/features/catalogoCompras`; integración de acceso desde ficha, inicio y barra; estilos en `frontend/src/styles/_carrito.scss`.

El carrito se abre como panel lateral desde el ícono de la barra superior y automáticamente después de agregar una prenda. Desde el panel se puede ir a la página completa del carrito. En cada línea del panel se permite aumentar/disminuir la cantidad o eliminar la prenda con confirmación; las operaciones actualizan el carrito persistido y el subtotal, y muestran el error del backend si no se completan. El indicador informa que los artículos no están reservados. La respuesta paginada de catálogo añade `tallasDisponibles` con las variantes que tienen stock positivo, sin cantidades; el detalle por prenda expone `stockDisponible` y `color` por variante para informar y limitar la cantidad elegida.

**SUPUESTO aislado/documentado:** una sola cesta por cliente, con la combinación prenda/talla/color única y cantidades repetidas agregadas. Agregar un producto no bloquea unidades para ese cliente. Varios clientes pueden incluir en sus carritos cantidades que, sumadas, superen el stock físico; al consultar, modificar o confirmar el pedido, cada carrito se contrasta con el stock actual de su variante y puede marcarse como no disponible. El carrito conserva el artículo para que el cliente pueda reducir su cantidad o eliminarlo. El envío se muestra desde la misma propiedad configurable del Facade; un carrito vacío tiene total cero.

### KAN-189 — Disponibilidad basada en stock físico

No se generan reservas al agregar, cambiar o eliminar líneas del carrito ni al iniciar el pago. Al confirmar una aprobación de pago, el procesador backend valida todas las cantidades y crea el pedido, descuenta stock y retira las unidades adquiridas del carrito en una única transacción. Si dos pagos compiten por las últimas unidades, solo el primero que confirma con stock disponible lo obtiene; al segundo se le crea un pedido cancelado y la transacción queda para reembolso, con nombre, talla, color, cantidad solicitada y stock disponible. El carrito del segundo cliente se conserva para revisar o ajustar. Las líneas de carrito de otros clientes no apartan stock y se marcan no disponibles cuando ya no alcanzan las existencias. El aviso “Los artículos no están reservados. ¡No los dejes escapar!” se presenta en el panel lateral y en la página del carrito. Si el proveedor ya cobró cuando se detecta falta de stock, US-11 marca la devolución como pendiente de gestión manual; el pedido cancelado no se puede reabrir.

**Decisión funcional:** no existe un plazo de reserva en US-10. Columnas de vencimiento heredadas de la implementación anterior pueden permanecer físicamente en bases de datos existentes, pero dejan de mapearse y no afectan la disponibilidad; su eliminación requiere una migración posterior no destructiva.

### KAN-190 — Dirección y resumen previo al pago

UI-11 en `/cliente/checkout` permite elegir una dirección de la cuenta para cada compra, sin seleccionar una automáticamente, o agregar una nueva. En “Mi cuenta” el cliente puede guardar varias direcciones y editar cada una de forma independiente. Cada dirección captura destinatario, dirección en un solo campo con formato colombiano (por ejemplo, «Carrera 12 # 34-56»), complemento opcional (apartamento, piso, interior u otras indicaciones), código postal colombiano de seis dígitos, departamento, municipio y teléfono celular colombiano `3XXXXXXXXX`; el frontend separa la dirección en sus cuatro componentes estructurados al guardarla. Departamento y municipio se eligen desde el catálogo completo local de municipios colombianos, y el municipio disponible depende del departamento seleccionado. Al cambiar de departamento se limpia el municipio anterior. Checkout exige elegir una dirección guardada o completar una nueva antes de continuar; crear o editar persiste el cambio y deja seleccionada esa dirección para la compra actual. El pedido conserva una copia cifrada de los datos de envío, independiente de futuras ediciones. Checkout usa automáticamente el perfil infantil activo de la cuenta para validar alergias; no vuelve a pedir que se seleccione. Al continuar, Angular envía la dirección y el perfil a `POST /api/cliente/checkout/resumen`; backend toma los productos del carrito persistido y vuelve a validar perfil, compatibilidad y stock físico, devolviendo los totales calculados.

El catálogo de ubicaciones es estático y local, derivado de `RafaelRamosR/dane-codigos-municipios` (datos DANE publicados en 2023, licencia MIT); su atribución y licencia se conservan junto al código fuente en `frontend/src/app/features/catalogoCompras/data/LICENSE-municipios-colombia.txt`. La interfaz no depende de un servicio externo para cargar departamentos o municipios.

UI-12 muestra líneas del resumen (producto, talla, cantidad, precio unitario/subtotal), dirección, subtotal, tarifa de envío correspondiente al departamento y municipio y total. El costo de envío se calcula una vez por compra, independientemente del número de prendas. El stepper es “Información de envío → Resumen → Pago”. Antes de iniciar el pago se puede regresar a editar dirección. Al pulsar “Pagar ahora”, `POST /api/cliente/pagos/checkout` valida nuevamente el carrito y guarda un snapshot cifrado asociado a la transacción; el pedido no se crea en este paso. Si el pago queda pendiente o es rechazado, el carrito sigue visible y se puede volver al checkout. Si Wompi lo aprueba, el backend crea el pedido como `PAGADO`, descuenta stock y retira del carrito las cantidades compradas. Al agotarse stock, registra el pedido cancelado para reembolso y mantiene el carrito para que el cliente pueda revisarlo.

**SUPUESTOS aislados:** se conserva el cargo de envío simplificado por zona que se describe en KAN-187; la comparación puntual con transportadoras y sus límites están registrados en la sección “Revisión de tarifas con transportadoras”. No se define una dirección predeterminada: el cliente elige una dirección en cada compra. El código postal es obligatorio y debe tener seis dígitos numéricos, conforme al formato colombiano. El intento de pago y el pedido final conservan snapshots cifrados para que cambios posteriores del catálogo, perfil o dirección no alteren la compra iniciada o pagada. Las unidades no se reservan al iniciar el pago; se descuentan únicamente al aprobarse.

### Ciclo de vida del pedido

En el flujo nuevo, el pedido se crea al aprobarse el pago y transita dentro de la misma transacción de `PENDIENTE_PAGO` a `PAGADO`. Después, el modelo limita las transiciones a `PAGADO → EN_PREPARACION → ENVIADO`; se permite pasar a `CANCELADO` desde `PENDIENTE_PAGO`, `PAGADO` o `EN_PREPARACION`. `ENVIADO` y `CANCELADO` son estados terminales: el ciclo dentro de la plataforma termina al entregar el paquete a la transportadora. Los pedidos históricos `PENDIENTE_PAGO` siguen soportados.

El carrito y los intentos de pago no reservan inventario. La confirmación del proveedor crea el pedido y descuenta stock atómicamente solo después de validar todas sus líneas. El proceso programado que cancelaba pedidos `PENDIENTE_PAGO` a los 30 minutos fue retirado con el flujo de checkout; no se generan pedidos pendientes visibles antes del pago, el cliente abandona una compra anulando su pago en proceso.
- Los datos personales de entrega (destinatario, dirección, complemento, código postal, departamento, municipio y teléfono) se cifran en la base de datos en la dirección guardada y en el snapshot del pedido, mediante el convertidor AES-GCM versionado existente y la clave de `app.crypto.perfil-key`. Las filas históricas creadas antes de este cambio en claro se leen por compatibilidad, pero requieren migración operativa para cifrarse en almacenamiento; no se afirma que se haya migrado información histórica productiva.
- Las direcciones de la cuenta se persisten en `direcciones_cliente` y solo se consultan/actualizan con la identidad autenticada del cliente; al desactivar la cuenta se eliminan. Los snapshots de pedidos históricos tienen ciclo de conservación independiente y requieren política de retención.

### Estampado en la línea del carrito (auditoría)

`ItemCarrito` guarda el `estampado` elegido en el detalle y la unicidad de la línea pasa a ser `(carrito, prenda, talla, color, estampado)`: la misma talla y color con estampados distintos son líneas separadas. `AgregarItemCarritoRequest.estampado` es obligatorio si la prenda ofrece varios, se asume si ofrece uno y se rechaza con 400 (campo `estampado`) si no pertenece a la prenda. `ItemCarritoResponse` lo devuelve y el carrito lo muestra junto al color. El stock sigue controlándose por talla y color. Requiere la migración `docs/us-08/migracion-estampados.sql`.

## Contratos backend

El endpoint HTTP del resumen invoca el Facade y valida la solicitud con Bean Validation. No crea pedidos ni procesa pagos.

| Contrato | Campos |
|---|---|
| `ResumenCheckoutRequest` | `perfilInfantilId` y dirección (código postal, departamento, municipio y demás datos de entrega) para calcular tarifa; las líneas se obtienen del carrito del servidor. |
| `ResumenCheckoutResponse` | `perfilInfantilId`, `items`, `subtotal`, `costoEnvio`, `total`. |
| `LineaResumenCheckoutResponse` | `prendaId`, `nombre`, `talla`, `color`, `cantidad`, `precioUnitario`, `subtotal`. |
| `POST /api/cliente/checkout/resumen` | Recibe el perfil, toma líneas del carrito persistido y devuelve el resumen validado; requiere rol `CLIENTE`. |
| `GET /api/cliente/checkout/direccion` | Endpoint anterior de compatibilidad: devuelve la primera dirección guardada del cliente o `guardada=false`. |
| `PUT /api/cliente/checkout/direccion` | Endpoint anterior de compatibilidad: crea o actualiza la primera dirección del cliente. |
| `GET /api/cliente/checkout/direcciones` | Lista las direcciones del cliente autenticado con sus identificadores. |
| `POST /api/cliente/checkout/direcciones` | Agrega una dirección a la cuenta autenticada. |
| `PUT /api/cliente/checkout/direcciones/{id}` | Actualiza una dirección propia; oculta como no encontrada una dirección de otro cliente. |
| `POST /api/cliente/pagos/checkout` | Inicia una transacción Wompi con el perfil, dirección y productos autoritativos del carrito; devuelve datos firmados con `pedidoId=null`. No crea pedido ni vacía el carrito. Ver US-11 para el contrato completo. |

## Estado y límites del flujo

| Momento | Resultado |
|---|---|
| KAN-187, resumen | Valida el perfil, compatibilidad de alergias, existencia de producto/talla y stock físico actual; calcula valores. No persiste pedido, reserva ni descuenta stock. |
| KAN-188, carrito | Carrito persistido por cliente; servicios y página cubiertos por pruebas unitarias, Vitest e integración de propiedad. |
| KAN-189, disponibilidad | El carrito y los intentos de pago no reservan unidades. La aprobación verificada crea el pedido, descuenta stock y quita del carrito las cantidades adquiridas de forma atómica; si falta stock, se conserva el carrito y la transacción queda para reembolso. |
| KAN-190, dirección/resumen/pago | UI-11/12 y endpoint de resumen; el inicio de pago guarda el snapshot, sin crear pedido ni vaciar el carrito. La aprobación verificada crea el pedido pagado y descuenta stock. |
| KAN-191, pruebas | Pruebas unitarias del Facade y disponibilidad, integración de seguridad/propiedad/stock y Vitest de servicios y páginas; ver resultado actualizado en `casos-de-prueba.md`. |
| US-11 | Integra Wompi y crea el pedido solamente ante una aprobación verificada; los pagos rechazados o pendientes conservan el carrito. |

## Archivos y estado de pruebas

Los casos, clases de prueba y resultados efectivamente ejecutados se registran en `casos-de-prueba.md`. El indicador de abandono de RNF-18 no se ha medido en producción; las pruebas automatizadas verifican comportamiento, no tasas de conversión.

## Dirección estructurada (auditoría, punto 7)
- La dirección de entrega ya no es un campo libre. Se captura con la nomenclatura colombiana:
  **tipo de vía** (Calle, Carrera, Avenida, Avenida Calle, Avenida Carrera, Diagonal,
  Transversal, Circular, Autopista, Variante, Kilómetro), **número de vía** (`45`, `45A`,
  `45 bis`, `45 Sur`), **número de cruce** (mismo formato), **placa** (1 a 3 dígitos) y
  **complemento** opcional (apartamento, torre, interior).
- El servidor compone el texto `direccion` («Calle 45 # 12 - 34») a partir de las partes y
  descarta el texto libre que envíe el cliente. `pedidos.direccion` guarda ese texto compuesto,
  por lo que vendedor, correos y reportes siguen funcionando sin cambios.
- Checkout y «Mi cuenta» presentan la dirección en un solo campo; el frontend la separa en componentes estructurados antes de enviarla al backend.
- Validaciones en Angular y en backend (`DireccionEnvioRequest`): tipo de vía de la lista,
  patrones de número de vía/cruce y placa; mensajes por campo.
- `direcciones_cliente` agrega `tipo_via`, `numero_via`, `numero_cruce` y `placa` (cifradas).
  Migración: `migracion-direccion-estructurada.sql`. Las direcciones guardadas antes del cambio
  quedan con esas columnas en NULL y el cliente debe completarlas en su próximo checkout.
- Para permitir varias direcciones por cliente, `DireccionClienteSchemaMigrator` elimina al iniciar
  la aplicación el índice único histórico de `cliente_id` en MySQL/MariaDB o H2, sin borrar filas.
- Fuera de este punto: la coherencia código postal ↔ departamento/municipio (punto 8).
