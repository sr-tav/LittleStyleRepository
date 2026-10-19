package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import java.math.BigDecimal;
import java.util.List;

/** Resumen de la compra: líneas con precio vigente, subtotal, envío y total. */
public record ResumenCheckoutResponse(
        Long perfilInfantilId,
        List<LineaResumenCheckoutResponse> items,
        BigDecimal subtotal,
        BigDecimal costoEnvio,
        BigDecimal total
) {
    /** Guarda una copia inmutable de las líneas. */
    public ResumenCheckoutResponse {
        items = List.copyOf(items);
    }
}
