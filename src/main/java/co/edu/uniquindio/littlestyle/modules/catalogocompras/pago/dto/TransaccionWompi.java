package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto;

import java.util.Map;

/** Vista mínima de una transacción de Wompi, construida desde un evento o desde la API. */
public record TransaccionWompi(
        String id,
        String estado,
        String referencia,
        Long montoCentavos,
        String moneda,
        String metodoPago
) {
    /** Construye la vista desde el objeto {@code transaction} de Wompi; falla si no es un mapa. */
    public static TransaccionWompi desdeMapa(Object valor) {
        if (!(valor instanceof Map<?, ?> mapa)) {
            throw new IllegalArgumentException("La transacción de Wompi no tiene el formato esperado");
        }
        Object monto = mapa.get("amount_in_cents");
        return new TransaccionWompi(
                texto(mapa.get("id")),
                texto(mapa.get("status")),
                texto(mapa.get("reference")),
                monto instanceof Number numero ? numero.longValue() : null,
                texto(mapa.get("currency")),
                texto(mapa.get("payment_method_type")));
    }

    /** Texto del valor, o {@code null}. */
    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
