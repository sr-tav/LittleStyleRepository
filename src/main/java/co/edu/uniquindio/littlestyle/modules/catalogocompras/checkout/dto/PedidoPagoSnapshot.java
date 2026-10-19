package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Copia de la compra al iniciar el pago (perfil, dirección, líneas y totales). Se guarda en la transacción y
 * con ella se crea el pedido cuando Wompi aprueba el cobro.
 */
public record PedidoPagoSnapshot(
        Long perfilInfantilId,
        DireccionEnvioRequest direccion,
        List<LineaResumenCheckoutResponse> items,
        BigDecimal subtotal,
        BigDecimal costoEnvio,
        BigDecimal total
) {
    /** Guarda una copia inmutable de las líneas. */
    public PedidoPagoSnapshot {
        items = List.copyOf(items);
    }
}
