package co.edu.uniquindio.littlestyle.modules.dashboard.dto;

import java.math.BigDecimal;

/** Detalle de demanda por prenda y talla: qué se pide, en qué talla y cuánto representa. */
public record DashboardDemandaDetalleResponse(
        String prenda,
        String talla,
        long unidades,
        BigDecimal valor
) {
}
