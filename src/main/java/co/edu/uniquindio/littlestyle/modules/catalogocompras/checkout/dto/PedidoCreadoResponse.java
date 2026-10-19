package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;

import java.math.BigDecimal;
import java.util.List;

/** Pedido recién creado con su dirección, líneas y totales. */
public record PedidoCreadoResponse(
        Long pedidoId,
        EstadoPedido estado,
        Long perfilInfantilId,
        DireccionEnvioRequest direccion,
        List<LineaResumenCheckoutResponse> items,
        BigDecimal subtotal,
        BigDecimal costoEnvio,
        BigDecimal total
) {
    /** Guarda una copia inmutable de las líneas. */
    public PedidoCreadoResponse {
        items = List.copyOf(items);
    }
}
