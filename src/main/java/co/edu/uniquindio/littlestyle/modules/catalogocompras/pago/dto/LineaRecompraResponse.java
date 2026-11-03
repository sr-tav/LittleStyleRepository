package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;

/** Línea del pedido cancelado que sí tenía stock; permite volver a agregarla al carrito. */
public record LineaRecompraResponse(
        Long prendaId,
        String nombre,
        String talla,
        int cantidad,
        ColorPreferido color
) {
    public LineaRecompraResponse(Long prendaId, String nombre, String talla, int cantidad){
        this(prendaId, nombre, talla, cantidad, null);
    }
}
