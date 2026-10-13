package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto;

import java.math.BigDecimal;
import java.util.List;

public record CarritoResponse(
        List<ItemCarritoResponse> items,
        int cantidad,
        BigDecimal subtotal,
        BigDecimal costoEnvio,
        BigDecimal total
) {
    public CarritoResponse {
        items = List.copyOf(items);
    }
}
