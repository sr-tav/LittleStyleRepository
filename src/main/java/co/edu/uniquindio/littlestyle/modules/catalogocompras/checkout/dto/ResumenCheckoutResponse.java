package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumenCheckoutResponse(
        Long perfilInfantilId,
        List<LineaResumenCheckoutResponse> items,
        BigDecimal subtotal,
        BigDecimal costoEnvio,
        BigDecimal total
) {
    public ResumenCheckoutResponse {
        items = List.copyOf(items);
    }
}
