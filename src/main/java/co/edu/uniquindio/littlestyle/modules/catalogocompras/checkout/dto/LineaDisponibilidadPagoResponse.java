package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;

/** Prenda y talla sin stock suficiente al confirmar un pago: cantidad pedida y stock que había. */
public record LineaDisponibilidadPagoResponse(
        Long prendaId,
        String nombre,
        String talla,
        int cantidadSolicitada,
        int stockDisponible,
        ColorPreferido color
) {
    public LineaDisponibilidadPagoResponse(Long prendaId, String nombre, String talla, int cantidadSolicitada, int stockDisponible) {
        this(prendaId, nombre, talla, cantidadSolicitada, stockDisponible, null);
    }
}
