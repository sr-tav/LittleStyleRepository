package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model;

public enum EstadoPedido {
    PENDIENTE_PAGO,
    PAGADO,
    EN_PREPARACION,
    ENVIADO,
    ENTREGADO,
    CANCELADO;

    public boolean permiteTransicionA(EstadoPedido siguiente) {
        if (siguiente == null) {
            return false;
        }
        return switch (this) {
            case PENDIENTE_PAGO -> siguiente == PAGADO || siguiente == CANCELADO;
            case PAGADO -> siguiente == EN_PREPARACION || siguiente == CANCELADO;
            case EN_PREPARACION -> siguiente == ENVIADO || siguiente == CANCELADO;
            case ENVIADO -> siguiente == ENTREGADO;
            case ENTREGADO, CANCELADO -> false;
        };
    }
}
