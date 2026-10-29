package co.edu.uniquindio.littlestyle.modules.dashboard.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardOverviewResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.DashboardAnalyticsRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardAnalyticsServiceTest {

    @Mock
    private DashboardAnalyticsRepository dashboardAnalyticsRepository;

    private DashboardAnalyticsService dashboardAnalyticsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dashboardAnalyticsService = new DashboardAnalyticsService(dashboardAnalyticsRepository);
    }

    @Test
    void resumenCalculaTotalesYSeriesDeDashboard() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 30);
        LocalDateTime inicio = startDate.atStartOfDay();
        LocalDateTime fin = endDate.plusDays(1).atStartOfDay();

        when(dashboardAnalyticsRepository.sumTotalVentas(inicio, fin, "VESTIDOS", 7L))
                .thenReturn(new BigDecimal("350000.00"));
        when(dashboardAnalyticsRepository.countPedidos(inicio, fin, "VESTIDOS", 7L)).thenReturn(42L);
        when(dashboardAnalyticsRepository.sumUnidadesVendidas(inicio, fin, "VESTIDOS", 7L))
                .thenReturn(new BigDecimal("120"));
        when(dashboardAnalyticsRepository.ventasPorCategoria(inicio, fin, "VESTIDOS", 7L)).thenReturn(List.of(
                new Object[] { "VESTIDOS", new BigDecimal("180000.00"), new BigDecimal("18") },
                new Object[] { "CONJUNTOS", new BigDecimal("170000.00"), new BigDecimal("24") }
        ));
        when(dashboardAnalyticsRepository.demandaPorTalla(inicio, fin, "VESTIDOS", 7L)).thenReturn(List.of(
                new Object[] { "M", new BigDecimal("18"), new BigDecimal("90000.00") },
                new Object[] { "L", new BigDecimal("16"), new BigDecimal("110000.00") }
        ));
        when(dashboardAnalyticsRepository.demandaPorPrendaTalla(inicio, fin, "VESTIDOS", 7L)).thenReturn(List.<Object[]>of(
                new Object[] { "Vestido floral", "4T", new BigDecimal("12"), new BigDecimal("240000.00") }
        ));
        when(dashboardAnalyticsRepository.productosMasVendidos(inicio, fin, "VESTIDOS", 7L)).thenReturn(List.of(
                new Object[] { "Buzo azul", new BigDecimal("12"), new BigDecimal("120000.00") },
                new Object[] { "Camiseta eco", new BigDecimal("10"), new BigDecimal("90000.00") }
        ));
        when(dashboardAnalyticsRepository.stockTotal("VESTIDOS", 7L)).thenReturn(new BigDecimal("300"));
        when(dashboardAnalyticsRepository.tallasBajoStock("VESTIDOS", 7L)).thenReturn(4L);

        DashboardOverviewResponse response = dashboardAnalyticsService.obtenerResumen(
                startDate, endDate, CategoriaPrenda.VESTIDOS, 7L);

        assertThat(response.totalVentas()).isEqualByComparingTo("350000.00");
        assertThat(response.totalPedidos()).isEqualTo(42L);
        assertThat(response.ticketPromedio()).isEqualByComparingTo("8333.33");
        assertThat(response.productosVendidos()).isEqualTo(120L);
        assertThat(response.stockTotal()).isEqualTo(300L);
        assertThat(response.stockBajo()).isEqualTo(4L);
        assertThat(response.ventasPorCategoria()).hasSize(2);
        assertThat(response.ventasPorCategoria().get(0).cantidad()).isEqualTo(18L);
        assertThat(response.demandaPorTalla().get(0).label()).isEqualTo("M");
        assertThat(response.demandaPorTalla().get(0).cantidad()).isEqualTo(18L);
        assertThat(response.demandaDetalle()).singleElement().satisfies(detalle -> {
            assertThat(detalle.prenda()).isEqualTo("Vestido floral");
            assertThat(detalle.talla()).isEqualTo("4T");
            assertThat(detalle.unidades()).isEqualTo(12L);
        });
        assertThat(response.busquedasMasFrecuentes()).isEmpty();
    }

    @Test
    void sinVentasDevuelveCerosYListasVaciasSinDatosDeEjemplo() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 30);
        LocalDateTime inicio = startDate.atStartOfDay();
        LocalDateTime fin = endDate.plusDays(1).atStartOfDay();

        when(dashboardAnalyticsRepository.sumTotalVentas(inicio, fin, null, null)).thenReturn(BigDecimal.ZERO);
        when(dashboardAnalyticsRepository.countPedidos(inicio, fin, null, null)).thenReturn(0L);
        when(dashboardAnalyticsRepository.sumUnidadesVendidas(inicio, fin, null, null)).thenReturn(BigDecimal.ZERO);
        when(dashboardAnalyticsRepository.ventasPorCategoria(inicio, fin, null, null)).thenReturn(List.of());
        when(dashboardAnalyticsRepository.demandaPorTalla(inicio, fin, null, null)).thenReturn(List.of());
        when(dashboardAnalyticsRepository.demandaPorPrendaTalla(inicio, fin, null, null)).thenReturn(List.of());
        when(dashboardAnalyticsRepository.productosMasVendidos(inicio, fin, null, null)).thenReturn(List.of());
        when(dashboardAnalyticsRepository.stockTotal(null, null)).thenReturn(BigDecimal.ZERO);
        when(dashboardAnalyticsRepository.tallasBajoStock(null, null)).thenReturn(0L);

        DashboardOverviewResponse response = dashboardAnalyticsService.obtenerResumen(startDate, endDate, null, null);

        assertThat(response.totalVentas()).isEqualByComparingTo("0");
        assertThat(response.productosVendidos()).isZero();
        assertThat(response.ventasPorCategoria()).isEmpty();
        assertThat(response.demandaDetalle()).isEmpty();
        assertThat(response.productosPopulares()).isEmpty();
    }

    @Test
    void elAlcanceYLaCategoriaSeEmpujanALasConsultas() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 30);
        LocalDateTime inicio = startDate.atStartOfDay();
        LocalDateTime fin = endDate.plusDays(1).atStartOfDay();

        when(dashboardAnalyticsRepository.sumTotalVentas(inicio, fin, null, 7L)).thenReturn(BigDecimal.ZERO);
        when(dashboardAnalyticsRepository.countPedidos(inicio, fin, null, 7L)).thenReturn(0L);
        when(dashboardAnalyticsRepository.sumUnidadesVendidas(inicio, fin, null, 7L)).thenReturn(BigDecimal.ZERO);
        when(dashboardAnalyticsRepository.ventasPorCategoria(inicio, fin, null, 7L)).thenReturn(List.of());
        when(dashboardAnalyticsRepository.demandaPorTalla(inicio, fin, null, 7L)).thenReturn(List.of());
        when(dashboardAnalyticsRepository.demandaPorPrendaTalla(inicio, fin, null, 7L)).thenReturn(List.of());
        when(dashboardAnalyticsRepository.productosMasVendidos(inicio, fin, null, 7L)).thenReturn(List.of());
        when(dashboardAnalyticsRepository.stockTotal(null, 7L)).thenReturn(BigDecimal.ZERO);
        when(dashboardAnalyticsRepository.tallasBajoStock(null, 7L)).thenReturn(0L);

        dashboardAnalyticsService.obtenerResumen(startDate, endDate, null, 7L);
        dashboardAnalyticsService.obtenerProductosMasConsultados(startDate, endDate, null, 7L);
        dashboardAnalyticsService.obtenerProductosMasVendidos(startDate, endDate, null, 7L);

        verify(dashboardAnalyticsRepository).sumTotalVentas(inicio, fin, null, 7L);
        verify(dashboardAnalyticsRepository).productosMasConsultados(inicio, fin, null, 7L);
        verify(dashboardAnalyticsRepository, atLeastOnce()).productosMasVendidos(inicio, fin, null, 7L);
    }

    @Test
    void fechaInicialPosteriorALaFinalLanzaExcepcion() {
        LocalDate startDate = LocalDate.of(2026, 10, 31);
        LocalDate endDate = LocalDate.of(2026, 10, 30);

        assertThatThrownBy(() -> dashboardAnalyticsService.obtenerResumen(startDate, endDate, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("fecha inicial");
    }
}
