package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import java.math.BigDecimal;

public record LineaResumenCheckoutResponse(
        Long prendaId,
        String nombre,
        String talla,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
