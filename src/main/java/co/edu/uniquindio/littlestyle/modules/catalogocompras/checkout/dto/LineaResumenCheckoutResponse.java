package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;

import java.math.BigDecimal;

/** Línea del resumen de compra con el precio vigente y su subtotal. */
public record LineaResumenCheckoutResponse(
        Long prendaId,
        String nombre,
        String talla,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        String tallaSugerida,
        ColorPreferido color
) {
    public LineaResumenCheckoutResponse(Long prendaId, String nombre, String talla, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal, String tallaSugerida){
        this(prendaId, nombre, talla, cantidad, precioUnitario, subtotal, tallaSugerida, null);
    }
}
