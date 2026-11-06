package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;

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
        ColorPreferido color,
        Estampado estampado
) {
    public LineaResumenCheckoutResponse(Long prendaId, String nombre, String talla, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal, String tallaSugerida, ColorPreferido color){
        this(prendaId, nombre, talla, cantidad, precioUnitario, subtotal, tallaSugerida, color, null);
    }

    public LineaResumenCheckoutResponse(Long prendaId, String nombre, String talla, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal, String tallaSugerida){
        this(prendaId, nombre, talla, cantidad, precioUnitario, subtotal, tallaSugerida, null, null);
    }
}
