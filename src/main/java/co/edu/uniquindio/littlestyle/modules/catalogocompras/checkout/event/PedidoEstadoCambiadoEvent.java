package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;

/**
 * Cualquier transición del ciclo de vida (RNF-13). El paso a ENVIADO tiene
 * correo propio con guía ({@code PedidoEnviadoEvent}) y este evento no lo
 * duplica: el observador lo omite.
 */
public record PedidoEstadoCambiadoEvent(Long pedidoId, EstadoPedido nuevo) {
}
