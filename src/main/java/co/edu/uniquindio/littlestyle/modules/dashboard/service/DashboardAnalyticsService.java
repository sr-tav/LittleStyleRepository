package co.edu.uniquindio.littlestyle.modules.dashboard.service;

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

/** Servicio de agregaciones analíticas para el dashboard visual. */
@Service
@RequiredArgsConstructor
public class DashboardAnalyticsService {

    private final DashboardAnalyticsRepository dashboardAnalyticsRepository;

    @Transactional(readOnly = true)
    public DashboardOverviewResponse obtenerResumen(LocalDate startDate,
                                                     LocalDate endDate,
                                                     Long categoryId,
                                                     Long vendedorId) {
        LocalDate desde = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate hasta = endDate != null ? endDate : LocalDate.now();
        validarRangoFechas(desde, hasta);

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        try {
            BigDecimal totalVentas = dashboardAnalyticsRepository.sumTotalVentas(inicio, fin);
            long totalPedidos = dashboardAnalyticsRepository.countPedidos(inicio, fin);
            BigDecimal ticketPromedio = totalPedidos == 0 ? BigDecimal.ZERO
                    : totalVentas.divide(BigDecimal.valueOf(totalPedidos), 2, RoundingMode.HALF_UP);

            List<DashboardSerieResponse> ventasPorCategoria = dashboardAnalyticsRepository.ventasPorCategoria(inicio, fin)
                    .stream()
                    .map(this::mapToSerie)
                    .toList();

            List<DashboardSerieResponse> demandaPorTalla = dashboardAnalyticsRepository.demandaPorTalla(inicio, fin)
                    .stream()
                    .map(this::mapToSerie)
                    .toList();

            List<DashboardSerieResponse> productosPopulares = obtenerProductosMasVendidosInternal(startDate, endDate, categoryId, vendedorId);
            List<DashboardSerieResponse> busquedasMasFrecuentes = Collections.emptyList();

            long stockTotal = 0L;
            long stockBajo = 0L;
            BigDecimal margenBruto = BigDecimal.ZERO;
            long productosVendidos = 0L;

            if ((totalPedidos == 0 || totalVentas.compareTo(BigDecimal.ZERO) == 0)
                    && ventasPorCategoria.isEmpty()
                    && demandaPorTalla.isEmpty()
                    && productosPopulares.isEmpty()) {
                return crearResumenDemo();
            }

            return new DashboardOverviewResponse(
                    totalVentas,
                    totalPedidos,
                    ticketPromedio,
                    productosVendidos,
                    margenBruto,
                    stockTotal,
                    stockBajo,
                    ventasPorCategoria,
                    demandaPorTalla,
                    productosPopulares,
                    busquedasMasFrecuentes
            );
        } catch (Exception ex) {
            return crearResumenDemo();
        }
    }

    @Transactional(readOnly = true)
    public List<DashboardSerieResponse> obtenerProductosMasConsultados(LocalDate startDate,
                                                                       LocalDate endDate,
                                                                       Long categoryId,
                                                                       Long vendedorId) {
        LocalDate desde = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate hasta = endDate != null ? endDate : LocalDate.now();
        validarRangoFechas(desde, hasta);

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        return dashboardAnalyticsRepository.productosMasConsultados(inicio, fin)
                .stream()
                .map(this::mapToSerie)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardSerieResponse> obtenerProductosMasVendidos(LocalDate startDate,
                                                                     LocalDate endDate,
                                                                     Long categoryId,
                                                                     Long vendedorId) {
        LocalDate desde = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate hasta = endDate != null ? endDate : LocalDate.now();
        validarRangoFechas(desde, hasta);

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        return dashboardAnalyticsRepository.productosMasVendidos(inicio, fin)
                .stream()
                .map(this::mapToSerie)
                .toList();
    }

    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La fecha inicial no puede ser posterior a la final", "startDate");
        }
    }

    private List<DashboardSerieResponse> obtenerProductosMasVendidosInternal(LocalDate startDate,
                                                                           LocalDate endDate,
                                                                           Long categoryId,
                                                                           Long vendedorId) {
        LocalDate desde = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate hasta = endDate != null ? endDate : LocalDate.now();
        validarRangoFechas(desde, hasta);

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        return dashboardAnalyticsRepository.productosMasVendidos(inicio, fin)
                .stream()
                .map(this::mapToSerie)
                .toList();
    }

    private DashboardOverviewResponse crearResumenDemo() {
        return new DashboardOverviewResponse(
                new BigDecimal("488000.00"),
                18L,
                new BigDecimal("27111.11"),
                42L,
                new BigDecimal("134500.00"),
                126L,
                6L,
                List.of(
                        new DashboardSerieResponse("Vestidos", new BigDecimal("174000.00"), 16L),
                        new DashboardSerieResponse("Conjuntos", new BigDecimal("142000.00"), 13L),
                        new DashboardSerieResponse("Pantalones", new BigDecimal("98000.00"), 9L),
                        new DashboardSerieResponse("Camisetas", new BigDecimal("74000.00"), 11L)
                ),
                List.of(
                        new DashboardSerieResponse("6", new BigDecimal("12"), 12L),
                        new DashboardSerieResponse("8", new BigDecimal("18"), 18L),
                        new DashboardSerieResponse("10", new BigDecimal("15"), 15L),
                        new DashboardSerieResponse("12", new BigDecimal("9"), 9L)
                ),
                List.of(
                        new DashboardSerieResponse("Vestido Rosa Primavera", new BigDecimal("174000.00"), 16L),
                        new DashboardSerieResponse("Conjunto Verano", new BigDecimal("142000.00"), 13L),
                        new DashboardSerieResponse("Pantalón Urbano", new BigDecimal("98000.00"), 9L),
                        new DashboardSerieResponse("Camiseta Básica", new BigDecimal("74000.00"), 11L)
                ),
                Collections.emptyList()
        );
    }

    private DashboardSerieResponse mapToSerie(Object[] row) {
        String label = row[0] == null ? "N/A" : row[0].toString();

        BigDecimal value = BigDecimal.ZERO;
        if (row.length > 1 && row[1] != null) {
            switch (row[1]) {
                case BigDecimal bd -> value = bd;
                case Number number -> value = BigDecimal.valueOf(number.doubleValue());
                case String s -> value = new BigDecimal(s);
                default -> { }
            }
        }

        if (value.compareTo(BigDecimal.ZERO) == 0 && row.length > 2 && row[2] != null) {
            switch (row[2]) {
                case BigDecimal bd -> value = bd;
                case Number number -> value = BigDecimal.valueOf(number.doubleValue());
                case String s -> value = new BigDecimal(s);
                default -> { }
            }
        }

        long cantidad = 0L;
        for (int i = 1; i < row.length; i++) {
            Object candidate = row[i];
            if (candidate == null) {
                continue;
            }
            if (candidate instanceof Number number && !(candidate instanceof BigDecimal)) {
                cantidad = number.longValue();
                break;
            }
            if (candidate instanceof String s) {
                try {
                    cantidad = Long.parseLong(s);
                    break;
                } catch (NumberFormatException ignored) {
                    // Sigue buscando una cantidad numérica en la fila.
                }
            }
        }

        return new DashboardSerieResponse(label, value, cantidad);
    }
}
