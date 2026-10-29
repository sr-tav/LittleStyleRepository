package co.edu.uniquindio.littlestyle.modules.dashboard.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardDemandaDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardOverviewResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardSerieResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.DashboardAnalyticsRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Servicio de agregaciones analíticas para el dashboard visual.
 *
 * <p>El alcance lo decide el llamador: {@code vendedorId == null} es el consolidado global del
 * administrador; un id filtra la tienda del vendedor. La categoría también es opcional. Todos los
 * indicadores salen de datos reales: si no hay ventas, el dashboard muestra ceros y listas vacías
 * en lugar de cifras de ejemplo.</p>
 */
@Service
@RequiredArgsConstructor
public class DashboardAnalyticsService {

    private final DashboardAnalyticsRepository dashboardAnalyticsRepository;

    @Transactional(readOnly = true)
    public DashboardOverviewResponse obtenerResumen(LocalDate startDate,
                                                    LocalDate endDate,
                                                    CategoriaPrenda categoria,
                                                    Long vendedorId) {
        Rango rango = Rango.de(startDate, endDate);
        String categoriaFiltro = categoria == null ? null : categoria.name();

        BigDecimal totalVentas = nuloCero(dashboardAnalyticsRepository
                .sumTotalVentas(rango.inicio(), rango.fin(), categoriaFiltro, vendedorId));
        long totalPedidos = dashboardAnalyticsRepository
                .countPedidos(rango.inicio(), rango.fin(), categoriaFiltro, vendedorId);
        BigDecimal ticketPromedio = totalPedidos == 0 ? BigDecimal.ZERO
                : totalVentas.divide(BigDecimal.valueOf(totalPedidos), 2, RoundingMode.HALF_UP);
        long productosVendidos = dashboardAnalyticsRepository
                .sumUnidadesVendidas(rango.inicio(), rango.fin(), categoriaFiltro, vendedorId)
                .longValue();

        List<DashboardSerieResponse> ventasPorCategoria = dashboardAnalyticsRepository
                .ventasPorCategoria(rango.inicio(), rango.fin(), categoriaFiltro, vendedorId)
                .stream()
                .map(this::mapVentasPorCategoria)
                .toList();

        List<DashboardSerieResponse> demandaPorTalla = dashboardAnalyticsRepository
                .demandaPorTalla(rango.inicio(), rango.fin(), categoriaFiltro, vendedorId)
                .stream()
                .map(this::mapDemandaPorTalla)
                .toList();

        List<DashboardSerieResponse> productosPopulares = dashboardAnalyticsRepository
                .productosMasVendidos(rango.inicio(), rango.fin(), categoriaFiltro, vendedorId)
                .stream()
                .map(this::mapProducto)
                .toList();

        List<DashboardDemandaDetalleResponse> demandaDetalle = dashboardAnalyticsRepository
                .demandaPorPrendaTalla(rango.inicio(), rango.fin(), categoriaFiltro, vendedorId)
                .stream()
                .map(this::mapDemandaDetalle)
                .toList();

        long stockTotal = dashboardAnalyticsRepository.stockTotal(categoriaFiltro, vendedorId).longValue();
        long stockBajo = dashboardAnalyticsRepository.tallasBajoStock(categoriaFiltro, vendedorId);

        return new DashboardOverviewResponse(
                totalVentas,
                totalPedidos,
                ticketPromedio,
                productosVendidos,
                BigDecimal.ZERO,
                stockTotal,
                stockBajo,
                ventasPorCategoria,
                demandaPorTalla,
                productosPopulares,
                Collections.emptyList(),
                demandaDetalle
        );
    }

    @Transactional(readOnly = true)
    public List<DashboardSerieResponse> obtenerProductosMasConsultados(LocalDate startDate,
                                                                       LocalDate endDate,
                                                                       CategoriaPrenda categoria,
                                                                       Long vendedorId) {
        Rango rango = Rango.de(startDate, endDate);
        return dashboardAnalyticsRepository
                .productosMasConsultados(rango.inicio(), rango.fin(), filtro(categoria), vendedorId)
                .stream()
                .map(this::mapProducto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardSerieResponse> obtenerProductosMasVendidos(LocalDate startDate,
                                                                     LocalDate endDate,
                                                                     CategoriaPrenda categoria,
                                                                     Long vendedorId) {
        Rango rango = Rango.de(startDate, endDate);
        return dashboardAnalyticsRepository
                .productosMasVendidos(rango.inicio(), rango.fin(), filtro(categoria), vendedorId)
                .stream()
                .map(this::mapProducto)
                .toList();
    }

    private static String filtro(CategoriaPrenda categoria) {
        return categoria == null ? null : categoria.name();
    }

    private DashboardSerieResponse mapVentasPorCategoria(Object[] fila) {
        return new DashboardSerieResponse(texto(fila[0]), decimal(fila[1]), largo(fila[2]));
    }

    private DashboardSerieResponse mapDemandaPorTalla(Object[] fila) {
        return new DashboardSerieResponse(texto(fila[0]), decimal(fila[2]), largo(fila[1]));
    }

    private DashboardSerieResponse mapProducto(Object[] fila) {
        return new DashboardSerieResponse(texto(fila[0]), decimal(fila[2]), largo(fila[1]));
    }

    private DashboardDemandaDetalleResponse mapDemandaDetalle(Object[] fila) {
        return new DashboardDemandaDetalleResponse(texto(fila[0]), texto(fila[1]), largo(fila[2]), decimal(fila[3]));
    }

    private static String texto(Object valor) {
        return valor == null ? "Sin dato" : valor.toString();
    }

    private static BigDecimal decimal(Object valor) {
        return switch (valor) {
            case null -> BigDecimal.ZERO;
            case BigDecimal bd -> bd;
            case Number n -> BigDecimal.valueOf(n.doubleValue());
            case String s -> new BigDecimal(s);
            default -> BigDecimal.ZERO;
        };
    }

    private static long largo(Object valor) {
        return switch (valor) {
            case null -> 0L;
            case Number n -> n.longValue();
            case String s -> Long.parseLong(s.trim());
            default -> 0L;
        };
    }

    private static BigDecimal nuloCero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    /** Rango ya validado y convertido a los límites {@code [inicio, fin)} que usan las consultas. */
    private record Rango(LocalDateTime inicio, LocalDateTime fin) {

        static Rango de(LocalDate startDate, LocalDate endDate) {
            LocalDate desde = startDate != null ? startDate : LocalDate.now().minusDays(30);
            LocalDate hasta = endDate != null ? endDate : LocalDate.now();
            if (desde.isAfter(hasta)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "La fecha inicial no puede ser posterior a la final", "startDate");
            }
            return new Rango(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
        }
    }
}
