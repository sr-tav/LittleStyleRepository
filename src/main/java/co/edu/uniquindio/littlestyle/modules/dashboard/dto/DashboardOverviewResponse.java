package co.edu.uniquindio.littlestyle.modules.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/** Resumen ejecutivo del dashboard analítico para administrador/vendedor. */
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
        List<DashboardSerieResponse> busquedasMasFrecuentes,
        List<DashboardDemandaDetalleResponse> demandaDetalle
) {
}
