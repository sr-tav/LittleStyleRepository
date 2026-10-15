package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event;

/**
 * Pago confirmado y stock descontado (PAGADO). El correo de confirmación de
 * pago se envía solo si la transacción se confirma (RNF-23).
 */
public record PedidoPagadoEvent(Long pedidoId) {
}
