package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

public record LineaDisponibilidadPagoResponse(
        Long prendaId,
        String nombre,
        String talla,
        int cantidadSolicitada,
        int stockDisponible
) {
}
