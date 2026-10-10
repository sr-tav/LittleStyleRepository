package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;

import java.math.BigDecimal;
import java.util.List;

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
    public PedidoCreadoResponse {
        items = List.copyOf(items);
    }
}
