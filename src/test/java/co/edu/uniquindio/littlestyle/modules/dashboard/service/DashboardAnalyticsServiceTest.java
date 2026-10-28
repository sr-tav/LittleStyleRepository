package co.edu.uniquindio.littlestyle.modules.dashboard.service;

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

        when(dashboardAnalyticsRepository.sumTotalVentas(inicio, fin)).thenReturn(new BigDecimal("350000.00"));
        when(dashboardAnalyticsRepository.countPedidos(inicio, fin)).thenReturn(42L);
        when(dashboardAnalyticsRepository.ventasPorCategoria(inicio, fin)).thenReturn(List.of(
                new Object[] { "BUZOS", new BigDecimal("180000.00"), 18L },
                new Object[] { "CAMISETAS", new BigDecimal("170000.00"), 24L }
        ));
        when(dashboardAnalyticsRepository.demandaPorTalla(inicio, fin)).thenReturn(List.of(
                new Object[] { "M", 18L, new BigDecimal("90000.00") },
                new Object[] { "L", 16L, new BigDecimal("110000.00") }
        ));
        when(dashboardAnalyticsRepository.productosMasVendidos(inicio, fin)).thenReturn(List.of(
                new Object[] { "Buzo azul", 12L, new BigDecimal("120000.00") },
                new Object[] { "Camiseta eco", 10L, new BigDecimal("90000.00") }
        ));

        DashboardOverviewResponse response = dashboardAnalyticsService.obtenerResumen(startDate, endDate, 1L, 7L);

        assertThat(response.totalVentas()).isEqualByComparingTo("350000.00");
        assertThat(response.totalPedidos()).isEqualTo(42L);
        assertThat(response.ticketPromedio()).isEqualByComparingTo("8333.33");
        assertThat(response.ventasPorCategoria()).hasSize(2);
        assertThat(response.demandaPorTalla()).hasSize(2);
        assertThat(response.productosPopulares()).hasSize(2);
        assertThat(response.busquedasMasFrecuentes()).isEmpty();
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
