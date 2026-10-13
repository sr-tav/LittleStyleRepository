package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto;

import java.util.List;
import java.util.Map;

/**
 * Evento enviado por Wompi al webhook. {@code data} se conserva como mapa porque la firma se
 * calcula con las rutas que indica {@code signature.properties} (por ejemplo, "transaction.id").
 */
public record EventoWompiRequest(
        String event,
        Map<String, Object> data,
        String environment,
        FirmaEvento signature,
        Long timestamp
) {
    public record FirmaEvento(List<String> properties, String checksum) {
    }
}
