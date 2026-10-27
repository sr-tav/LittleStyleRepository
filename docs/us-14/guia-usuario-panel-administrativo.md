# Guía de usuario — Panel Administrativo

## Acceso

1. Inicia sesión con una cuenta cuyo rol sea **Administrador**.
2. Abre **Panel administrativo** desde el menú lateral.
3. Usa las tarjetas para ir a Usuarios, Moderación, Auditoría o Incidencias.

Las opciones y acciones dependen del rol. Clientes y vendedores no pueden acceder a
rutas administrativas.

## Dashboard

El dashboard muestra usuarios por estado, publicaciones activas, prendas sin revisar,
incidencias abiertas/en revisión y la tasa calculada de resolución en menos de 24 horas.
Si el resumen no carga, usa **Reintentar**. La tasa refleja los datos registrados en el
sistema; el panel no promete por sí mismo el cumplimiento futuro del objetivo RNF-17.

## Administrar cuentas

1. Abre **Usuarios**.
2. Filtra por rol, estado o nombre/correo/tienda; usa **Limpiar** para restablecer.
3. Recorre páginas con los controles al pie de la tabla.
4. En una cuenta de cliente o vendedor selecciona **Cambiar estado**.
5. Elige **Suspender**, **Bloquear** o **Habilitar**, según el estado actual.
6. Escribe el motivo obligatorio (10–500 caracteres) y confirma. Si la operación falla,
   el diálogo mostrará el mensaje recibido; el estado no debe considerarse cambiado.

No se ofrecen acciones para cuentas administrativas ni anonimizadas/eliminadas. El
administrador tampoco puede cambiar su propia cuenta. Suspender/bloquear a un vendedor
deja sus prendas fuera de la vista del cliente sin modificar el stock ni el estado de
publicación.

## Revisar prendas

1. Abre **Moderación**. La cola prioriza prendas **Sin revisar**, luego reportes
   abiertos/en revisión, y finalmente las más recientemente actualizadas.
2. Filtra por moderación, estado comercial, vendedor o texto y pulsa **Filtrar**.
3. Selecciona **Ver detalle** para revisar descripción, composición, tallas e imágenes.
4. **Aprobar** marca la prenda como aprobada. **Desactivar** la oculta a clientes y exige
   un motivo de 10–500 caracteres que verá el vendedor. **Restaurar** devuelve una
   desactivada a estado aprobado.

La moderación ocurre después de publicar: «Sin revisar» no significa que la prenda esté
oculta. El sistema no elimina el producto ni destruye su inventario.

## Auditoría

Abre **Auditoría** para buscar por administrador, acción, tipo/id de entidad o fechas
desde/hasta (los filtros de fechas y resultado también están disponibles en la API).
Los registros son de solo lectura. **Verificar integridad** recalcula la cadena hash y
muestra el total, si está íntegra o el primer registro inconsistente. Una inconsistencia
debe comunicarse al responsable técnico; no intentes corregirla manualmente.

## Incidencias

1. Abre **Incidencias** y filtra por estado, tipo, rol o texto.
2. Usa **Tomar** para pasar una incidencia abierta a revisión.
3. En una incidencia en revisión, selecciona resolver o rechazar.
4. Escribe una respuesta clara (10–4000 caracteres) y confirma.

La respuesta queda registrada y se notifica al reportante por correo si el envío está
configurado. Reportes de producto destacados tienen referencia a la prenda que los
originó.

## Reportes y apelaciones para usuarios

- **Cliente:** en el detalle del producto elige **Reportar producto**, escribe asunto y
  una descripción (20–4000 caracteres) y envía. El reporte se suma a la cola admin.
- **Vendedor:** en **Mis productos**, una prenda desactivada muestra el motivo del admin.
  Elige **Apelar** para abrir **Mis incidencias** con referencia y asunto precargados;
  completa la descripción. No se permite editar ni reactivar hasta la decisión del admin.
- Una cuenta suspendida/bloqueada no puede iniciar sesión. Aunque el sistema crea una
  incidencia cuando el admin cambia el estado, el envío público de una apelación que
  verifique correo está pendiente de un flujo seguro. No compartas credenciales ni uses
  otra cuenta para simular una apelación.

## Estados de carga y errores

Los módulos muestran indicador mientras consultan, un mensaje cuando no hay resultados,
y un aviso con el error de API cuando la operación falla. Usa **Reintentar** o actualiza
los filtros; no repitas acciones de escritura de manera sucesiva si la respuesta quedó
indeterminada. Comprueba el estado antes de reintentar.
