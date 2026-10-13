package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto;

/**
 * Datos que el frontend entrega al Widget de Wompi. La firma de integridad se calcula en el
 * servidor para que el monto y la referencia no puedan alterarse desde el navegador.
 */
public record InicioPagoResponse(
        Long pedidoId,
        String referencia,
        long montoCentavos,
        String moneda,
        String llavePublica,
        String firmaIntegridad,
        String urlRedireccion,
        String emailCliente,
        String nombreCliente,
        String telefonoCliente
) {
}
