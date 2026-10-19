package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Carrito con sus líneas, unidades y subtotal. El envío y el total solo se conocen en el checkout, cuando hay
 * dirección: quedan nulos salvo con el carrito vacío, donde son cero.
 */
public record CarritoResponse(
        List<ItemCarritoResponse> items,
        int cantidad,
        BigDecimal subtotal,
        BigDecimal costoEnvio,
        BigDecimal total
) {
    /** Guarda una copia inmutable de las líneas. */
    public CarritoResponse {
        items = List.copyOf(items);
    }
}
