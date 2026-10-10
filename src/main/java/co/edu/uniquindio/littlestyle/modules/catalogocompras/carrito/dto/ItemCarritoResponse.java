package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto;

import java.math.BigDecimal;

public record ItemCarritoResponse(
        Long id,
        Long prendaId,
        String nombre,
        String imagenUrl,
        String talla,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        boolean disponible
) {
}
