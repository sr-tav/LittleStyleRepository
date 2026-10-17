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
    PARCIALMENTE_ENVIADO,
    ENVIADO,
    CANCELADO;
    

    public boolean permiteTransicionA(EstadoPedido siguiente) {
        if (siguiente == null) {
            return false;
        }
        return switch (this) {
            case PENDIENTE_PAGO -> siguiente == PAGADO || siguiente == CANCELADO;
            case PAGADO -> siguiente == REGISTRADO || siguiente == CANCELADO;
            case REGISTRADO -> siguiente == EN_PREPARACION || siguiente == PARCIALMENTE_ENVIADO
                    || siguiente == ENVIADO || siguiente == CANCELADO;
            case EN_PREPARACION -> siguiente == PARCIALMENTE_ENVIADO || siguiente == ENVIADO
                    || siguiente == CANCELADO;
            case PARCIALMENTE_ENVIADO -> siguiente == ENVIADO;
            case ENVIADO, CANCELADO -> false;
        };
    }

    /**
     * Estado global derivado de los despachos del pedido (US-12 despachos por
     * vendedor): todos REGISTRADO queda REGISTRADO; alguno en preparación
     * queda EN_PREPARACION; alguno enviado y otros no queda
     * PARCIALMENTE_ENVIADO; todos enviados queda ENVIADO. Nadie lo edita a mano.
     */
    public static EstadoPedido calcularDerivado(java.util.Collection<EstadoPedido> estados) {
        if (estados == null || estados.isEmpty()) {
            return REGISTRADO;
        }
        boolean algunoEnviado = false;
        boolean algunoNoEnviado = false;
        boolean algunoEnPreparacion = false;
        for (EstadoPedido e : estados) {
            if (e == ENVIADO) {
                algunoEnviado = true;
            } else {
                algunoNoEnviado = true;
            }
            if (e == EN_PREPARACION) {
                algunoEnPreparacion = true;
            }
        }
        if (algunoEnviado && algunoNoEnviado) {
            return PARCIALMENTE_ENVIADO;
        }
        if (algunoEnviado) {
            return ENVIADO;
        }
        if (algunoEnPreparacion) {
            return EN_PREPARACION;
        }
        return REGISTRADO;
    }

    /** Etiqueta en español para UI y correos (nunca el enum crudo). */
    public String etiqueta() {
        return switch (this) {
            case PENDIENTE_PAGO -> "Pendiente de pago";
            case PAGADO -> "Pagado";
            case REGISTRADO -> "Registrado";
            case EN_PREPARACION -> "En preparación";
            case PARCIALMENTE_ENVIADO -> "Parcialmente enviado";
            case ENVIADO -> "Enviado";
            case CANCELADO -> "Cancelado";
        };
    }
}
