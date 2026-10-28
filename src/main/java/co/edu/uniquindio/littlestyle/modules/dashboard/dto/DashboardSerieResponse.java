package co.edu.uniquindio.littlestyle.modules.dashboard.dto;

import java.math.BigDecimal;

/** Serie de datos para un gráfico del dashboard analítico. */
public record DashboardSerieResponse(
        String label,
        BigDecimal value,
        Long cantidad
) {
}

