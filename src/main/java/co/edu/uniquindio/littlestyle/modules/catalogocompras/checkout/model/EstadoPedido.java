package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model;

/**
 * Ciclo de vida del pedido dentro de la plataforma: termina en ENVIADO, cuando
 * el paquete pasa a la transportadora con su número de guía (la logística
 * externa no es alcance del sistema). ENVIADO y CANCELADO son terminales.
 */
public enum EstadoPedido {
    PENDIENTE_PAGO,
    PAGADO,
    EN_PREPARACION,
    ENVIADO,
    CANCELADO;

    public boolean permiteTransicionA(EstadoPedido siguiente) {
        if (siguiente == null) {
            return false;
        }
        return switch (this) {
            case PENDIENTE_PAGO -> siguiente == PAGADO || siguiente == CANCELADO;
            case PAGADO -> siguiente == EN_PREPARACION || siguiente == CANCELADO;
            case EN_PREPARACION -> siguiente == ENVIADO || siguiente == CANCELADO;
            case ENVIADO, CANCELADO -> false;
        };
    }
}
