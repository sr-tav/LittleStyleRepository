package co.edu.uniquindio.littlestyle.modules.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumen ejecutivo del dashboard analítico para administrador/vendedor.
 *
 * <p>{@code margenBruto} es {@code null} porque el catálogo no registra el costo de las prendas: sin costo no
 * hay margen que calcular y mostrar un 0 sería un dato falso.
 */
public record DashboardOverviewResponse(
        BigDecimal totalVentas,
        long totalPedidos,
        BigDecimal ticketPromedio,
        long productosVendidos,
        BigDecimal margenBruto,
        long stockTotal,
        long stockBajo,
        List<DashboardSerieResponse> ventasPorCategoria,
        List<DashboardSerieResponse> demandaPorTalla,
        List<DashboardSerieResponse> productosPopulares,
        List<DashboardSerieResponse> productosMasConsultados,
        List<DashboardSerieResponse> busquedasMasFrecuentes
) {
}

