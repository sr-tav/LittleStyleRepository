package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import java.math.BigDecimal;
import java.util.List;

public record PedidoPagoSnapshot(
        Long perfilInfantilId,
        DireccionEnvioRequest direccion,
        List<LineaResumenCheckoutResponse> items,
        BigDecimal subtotal,
        BigDecimal costoEnvio,
        BigDecimal total
) {
    public PedidoPagoSnapshot {
        items = List.copyOf(items);
    }
}
