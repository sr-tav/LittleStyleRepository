package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event;

/** Un vendedor envió su despacho de un pedido con número de guía. */
public record DespachoEnviadoEvent(Long pedidoId, Long despachoId) {
}
