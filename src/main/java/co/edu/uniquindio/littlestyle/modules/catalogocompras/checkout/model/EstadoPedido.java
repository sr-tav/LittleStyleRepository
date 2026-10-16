package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model;

/**
 * Ciclo de vida del pedido dentro de la plataforma (US-12, RNF-13).
 * Mapeo con los documentos: "Registrado" (US-12) = REGISTRADO, pago
 * confirmado y listo para preparar; "registrado, en preparación y
 * entregado" (Definición, proceso 2) = REGISTRADO, EN_PREPARACION y
 * ENVIADO (el sistema no rastrea la entrega de la transportadora, solo el
 * despacho con guía). PENDIENTE_PAGO y PAGADO son técnicos previos al
 * registro. ENVIADO y CANCELADO son terminales.
 */
public enum EstadoPedido {
    PENDIENTE_PAGO,
    PAGADO,
    REGISTRADO,
    EN_PREPARACION,
    ENVIADO,
    CANCELADO;

    public boolean permiteTransicionA(EstadoPedido siguiente) {
        if (siguiente == null) {
            return false;
        }
        return switch (this) {
            case PENDIENTE_PAGO -> siguiente == PAGADO || siguiente == CANCELADO;
            case PAGADO -> siguiente == REGISTRADO || siguiente == CANCELADO;
            case REGISTRADO -> siguiente == EN_PREPARACION || siguiente == CANCELADO;
            case EN_PREPARACION -> siguiente == ENVIADO || siguiente == CANCELADO;
            case ENVIADO, CANCELADO -> false;
        };
    }

    /** Etiqueta en español para UI y correos (nunca el enum crudo). */
    public String etiqueta() {
        return switch (this) {
            case PENDIENTE_PAGO -> "Pendiente de pago";
            case PAGADO -> "Pagado";
            case REGISTRADO -> "Registrado";
            case EN_PREPARACION -> "En preparación";
            case ENVIADO -> "Enviado";
            case CANCELADO -> "Cancelado";
        };
    }
}
