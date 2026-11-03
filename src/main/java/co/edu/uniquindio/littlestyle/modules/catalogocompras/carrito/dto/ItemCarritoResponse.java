package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;

import java.math.BigDecimal;

/**
 * Línea del carrito con el precio actual de la prenda. {@code disponible} es falso si la prenda dejó de estar
 * publicada o no hay stock suficiente para la cantidad pedida.
 */
public record ItemCarritoResponse(
        Long id,
        Long prendaId,
        String nombre,
        String imagenUrl,
        String talla,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        boolean disponible,
        String tallaSugerida,
        ColorPreferido color
) {
    public ItemCarritoResponse(Long id, Long prendaId, String nombre, String imagenUrl, String talla, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal, boolean disponible, String tallaSugerida) {
        this(id, prendaId, nombre, imagenUrl, talla, cantidad, precioUnitario, subtotal, disponible, tallaSugerida, null);
    }
}
