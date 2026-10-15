package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event;

/**
 * Pedido recién creado (PENDIENTE_PAGO). El correo de confirmación de
 * creación se envía solo si la transacción se confirma (RNF-23).
 */
public record PedidoCreadoEvent(Long pedidoId) {
}
