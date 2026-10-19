package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Evento enviado por Wompi al webhook. {@code data} se conserva como mapa porque la firma se
 * calcula con las rutas que indica {@code signature.properties} (por ejemplo, "transaction.id").
 * {@code timestamp} se recibe sin tipo fijo (número o texto) para no rechazar el evento si Wompi
 * cambia el formato; la firma usa su valor tal como llegó.
 */
public record EventoWompiRequest(
        String event,
        Map<String, Object> data,
        String environment,
        FirmaEvento signature,
        Object timestamp
) {
    /** Rutas de {@code data} que entran en el checksum y el checksum enviado por Wompi. */
    public record FirmaEvento(List<String> properties, String checksum) {
    }

    /** Texto del timestamp tal como se concatena en el checksum, o {@code null} si no vino. */
    public String timestampTexto() {
        if (timestamp == null) {
            return null;
        }
        if (timestamp instanceof Number numero && !(numero instanceof BigDecimal)) {
            return numero instanceof Double || numero instanceof Float
                    ? new BigDecimal(numero.toString()).toPlainString()
                    : numero.toString();
        }
        if (timestamp instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        String texto = timestamp.toString().trim();
        return texto.isEmpty() ? null : texto;
    }
}
