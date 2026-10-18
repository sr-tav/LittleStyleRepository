package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model;

/**
 * Estado local de un intento de pago con Wompi. {@code REEMBOLSO_PENDIENTE} indica que Wompi
 * aprobó el cobro pero la transacción no puede convertirse en pedido (sin stock, anulado
 * por el cliente antes del cobro o el pedido ya no está pendiente de pago).
 */
public enum EstadoTransaccionPago {
    CREADA,
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    ANULADA,
    ERROR,
    REEMBOLSO_PENDIENTE;

    /** Estados que ningún evento posterior de Wompi puede modificar. */
    public boolean esFinal() {
        return this == APROBADA || this == RECHAZADA || this == ANULADA
                || this == ERROR || this == REEMBOLSO_PENDIENTE;
    }

    public static EstadoTransaccionPago desdeWompi(String estadoWompi) {
        if (estadoWompi == null) {
            return PENDIENTE;
        }
        return switch (estadoWompi) {
            case "APPROVED" -> APROBADA;
            case "DECLINED" -> RECHAZADA;
            case "VOIDED" -> ANULADA;
            case "ERROR" -> ERROR;
            default -> PENDIENTE;
        };
    }
}
