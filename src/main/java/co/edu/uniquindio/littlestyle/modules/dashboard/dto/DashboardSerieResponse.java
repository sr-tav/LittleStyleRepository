package co.edu.uniquindio.littlestyle.modules.dashboard.dto;

import java.math.BigDecimal;

/**
 * Serie de datos para un gráfico del dashboard analítico.
 *
 * @param label    nombre del grupo (prenda, categoría, talla o término buscado)
 * @param value    monto vendido en COP; en métricas de conteo (vistas, búsquedas) es igual a {@code cantidad}
 * @param cantidad unidades vendidas, vistas o búsquedas según la serie
 */
public record DashboardSerieResponse(
        String label,
        BigDecimal value,
        Long cantidad
) {

    /** Serie de solo conteo: el valor coincide con la cantidad. */
    public DashboardSerieResponse(String label, Long cantidad) {
        this(label, BigDecimal.valueOf(cantidad == null ? 0L : cantidad), cantidad == null ? 0L : cantidad);
    }
}
