package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

/** Prenda y talla sin stock suficiente al confirmar un pago: cantidad pedida y stock que había. */
public record LineaDisponibilidadPagoResponse(
        Long prendaId,
        String nombre,
        String talla,
        int cantidadSolicitada,
        int stockDisponible
) {
}
