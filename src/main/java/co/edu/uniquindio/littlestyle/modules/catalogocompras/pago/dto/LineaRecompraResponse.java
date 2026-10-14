package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto;

/** Línea del pedido cancelado que sí tenía stock; permite volver a agregarla al carrito. */
public record LineaRecompraResponse(
        Long prendaId,
        String nombre,
        String talla,
        int cantidad
) {
}
