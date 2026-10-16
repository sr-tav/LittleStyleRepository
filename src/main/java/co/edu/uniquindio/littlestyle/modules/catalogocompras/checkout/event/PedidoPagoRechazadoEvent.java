package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event;

/** Wompi rechazó el cobro: no hay pedido ni cargo; se avisa al cliente para reintentar. */
public record PedidoPagoRechazadoEvent(Long transaccionId) {
}
