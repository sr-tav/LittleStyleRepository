package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event;

/** El pedido completo pasó a {@code ENVIADO}; dispara el correo con la guía. */
public record PedidoEnviadoEvent(Long pedidoId) {
}