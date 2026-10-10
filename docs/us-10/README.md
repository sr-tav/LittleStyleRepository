# Documentación de US-10

Esta carpeta reúne la descripción funcional, los casos de prueba y la evidencia automatizada del carrito de compras y la coordinación del checkout en LittleStyle.

## Qué se implementó

- **Carrito persistido por cliente:** backend con entidad, servicio, repositorio, DTOs y endpoints bajo `/api/cliente/carrito`; el carrito se protege por usuario y evita acceso a recursos ajenos.
- **Carrito reactivo en Angular:** servicio con signals, cantidad, subtotal, envío y total; integración con la UI del cliente y con el badge de la vista principal.
- **Panel lateral del carrito:** se abre al agregar una prenda o desde el ícono de la barra superior; incluye el aviso de que los artículos no se reservan y un enlace a la página completa del carrito.
- **Validación del checkout:** facade que valida stock, perfil infantil activo, incompatibilidades por alergias textiles, suma de cantidades repetidas y cálculo de subtotal/total.
- **Disponibilidad sin reserva:** cada carrito se valida contra el stock físico actual. Los carritos de otros clientes y pedidos pendientes no bloquean unidades; si las existencias bajan, el carrito muestra el artículo como no disponible.
- **Dirección y resumen previo al pago:** dirección guardada y asociada a la cuenta, opción de usarla o editarla, complemento opcional, código postal de seis dígitos, departamento, municipio, celular colombiano y resumen con subtotal, tarifa configurable y total.
- **Dirección compartida con Mi cuenta:** la dirección capturada o editada desde checkout aparece en la sección “Dirección de envío” de la cuenta y se puede actualizar desde allí con los mismos endpoints.
- **Ubicación colombiana:** selects encadenados de los 33 departamentos y 1.123 municipios, cargados desde datos locales con atribución y licencia MIT.
- **Acceso rápido a tallas:** en vitrina, novedades y recomendaciones se ven la acción “Agregar al carrito” y las tallas disponibles directamente al pasar el cursor o enfocar la tarjeta; no se necesita abrir el detalle ni desplazar la página para encontrar las tallas.
- **Pedido pendiente de pago:** creación del pedido en estado `PENDIENTE_PAGO` con snapshot de dirección, perfil y líneas; el flujo entrega la información al inicio del pago de US-11 sin integrar Wompi ni cobrar.

Se excluye de este alcance el pago real, la pasarela, la confirmación del proveedor, el webhook de Wompi y el descuento definitivo de stock. No hay impuestos, descuentos ni cupones; el total sigue siendo subtotal + envío.

## Archivos de documentación

| Archivo | Contenido |
|---|---|
| [`descripcion-funcional.md`](descripcion-funcional.md) | Objetivo, alcance, reglas funcionales, supuestos configurables, trazabilidad, endpoints, estados y límites de la US-10. |
| [`casos-de-prueba.md`](casos-de-prueba.md) | Casos CP-US10, pruebas asociadas, resultados ejecutados y evidencias backend/frontend. |

## Evidencia registrada

Última ejecución automatizada registrada para US-10:

- Backend: `.\mvnw.cmd -q test` — 197 pruebas aprobadas; 0 fallidas, 0 errores.
- Frontend: desde `frontend/`, `.\node_modules\.bin\ng.cmd test --watch=false` — 25 archivos aprobados; 122 pruebas aprobadas.

La suite incluye la regla sin reservas, el panel lateral y los nuevos campos de dirección. Las pruebas no miden el abandono real de RNF-18 en producción.

## Supuestos y deuda funcional documentados

- El envío conserva los cargos por zona acordados: Armenia $7.000, otros municipios configurados del Quindío $10.000 y otras ciudades $15.000. Son valores comerciales simplificados de LittleStyle, no tarifas en tiempo real de las transportadoras. La comparación reciente con Inter Rapidísimo y Servientrega, con ciudades, entradas, resultados y enlaces oficiales, está en “Revisión de tarifas con transportadoras” de `descripcion-funcional.md`.
- La dirección de envío exige código postal colombiano de seis dígitos, departamento, municipio, dirección y destinatario; dirección guardada y snapshot del pedido se cifran.
- Departamento y municipio se eligen del catálogo estático local. Los nombres de talla disponibles se incluyen en el listado del catálogo sin exponer cantidades de stock.
- Los artículos no se reservan al agregarlos al carrito ni al confirmar un pedido pendiente. US-11 debe revalidar y descontar el stock en una operación atómica al aprobar el pago; sin existencias suficientes, debe cancelar el pedido y gestionar el reembolso.
- El pedido en `PENDIENTE_PAGO` requiere reconciliación con la definición del ciclo de vida del pedido en US-12.
- La cancelación automática por vencimiento del pedido pendiente queda como deuda funcional de US-11/US-12.
- Las columnas antiguas de vencimiento de reserva, si ya existen en una base de datos, son obsoletas e inertes; su retiro requiere una migración posterior.
- Los datos de dirección guardados y los snapshots nuevos de pedido se cifran en persistencia con el convertidor AES-GCM existente; las filas históricas creadas en claro antes de este cambio requieren migración operativa.

Estos supuestos quedan explicitados en la documentación funcional para no confundir alcance de la historia con la siguiente etapa de pago y cierre del pedido.
