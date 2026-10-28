package co.edu.uniquindio.littlestyle.modules.dashboard.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardOverviewResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardSerieResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.DashboardAnalyticsRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Reglas del servicio con el repositorio mockeado: nada de datos demo y alcance por vendedor/categoría. */
@ExtendWith(MockitoExtension.class)
class DashboardAnalyticsServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 30);
    private static final LocalDateTime INICIO = DESDE.atStartOfDay();
    private static final LocalDateTime FIN = HASTA.plusDays(1).atStartOfDay();

    @Mock
    private DashboardAnalyticsRepository repository;

    private DashboardAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new DashboardAnalyticsService(repository);
        // Listas vacías por defecto; cada test sobreescribe lo que le interesa.
        lenient().when(repository.ventasPorCategoria(any(), any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(repository.demandaPorTalla(any(), any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(repository.productosMasVendidos(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        lenient().when(repository.productosMasConsultados(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        lenient().when(repository.busquedasMasFrecuentes(any(), any(), any(Pageable.class))).thenReturn(List.of());
    }

    @Test
    void resumenCalculaTotalesInventarioYSeriesReales() {
        when(repository.sumTotalVentas(INICIO, FIN, DashboardAnalyticsService.ESTADOS_VENTA, 7L,
                CategoriaPrenda.BUZOS)).thenReturn(new BigDecimal("350000.00"));
        when(repository.countPedidos(INICIO, FIN, DashboardAnalyticsService.ESTADOS_VENTA, 7L,
                CategoriaPrenda.BUZOS)).thenReturn(42L);
        when(repository.sumUnidadesVendidas(INICIO, FIN, DashboardAnalyticsService.ESTADOS_VENTA, 7L,
                CategoriaPrenda.BUZOS)).thenReturn(55L);
        when(repository.sumStockTotal(EstadoPrenda.ACTIVA, 7L, CategoriaPrenda.BUZOS)).thenReturn(120L);
        when(repository.countStockBajo(EstadoPrenda.ACTIVA, 7L, CategoriaPrenda.BUZOS)).thenReturn(4L);
        when(repository.productosMasVendidos(eq(INICIO), eq(FIN), any(), eq(7L), eq(CategoriaPrenda.BUZOS),
                any(Pageable.class)))
                .thenReturn(List.of(new DashboardSerieResponse("Buzo azul", new BigDecimal("120000"), 12L)));
        when(repository.busquedasMasFrecuentes(eq(INICIO), eq(FIN), any(Pageable.class)))
                .thenReturn(List.of(new DashboardSerieResponse("buzo", 9L)));

        DashboardOverviewResponse r = service.obtenerResumen(DESDE, HASTA, "buzos", 7L);

        assertThat(r.totalVentas()).isEqualByComparingTo("350000.00");
        assertThat(r.totalPedidos()).isEqualTo(42L);
        assertThat(r.ticketPromedio()).isEqualByComparingTo("8333.33");
        assertThat(r.productosVendidos()).isEqualTo(55L);
        assertThat(r.stockTotal()).isEqualTo(120L);
        assertThat(r.stockBajo()).isEqualTo(4L);
        assertThat(r.margenBruto()).isNull();
        assertThat(r.productosPopulares()).extracting(DashboardSerieResponse::label).containsExactly("Buzo azul");
        assertThat(r.busquedasMasFrecuentes()).extracting(DashboardSerieResponse::label).containsExactly("buzo");
    }

    @Test
    void sinVentasNiPedidosDevuelveCerosYNoCifrasInventadas() {
        // Los sum de una consulta vacía llegan como null desde la base.
        when(repository.sumTotalVentas(any(), any(), any(), any(), any())).thenReturn(null);
        when(repository.sumUnidadesVendidas(any(), any(), any(), any(), any())).thenReturn(null);
        when(repository.sumStockTotal(any(), any(), any())).thenReturn(null);

        DashboardOverviewResponse r = service.obtenerResumen(DESDE, HASTA, null, 7L);

        assertThat(r.totalVentas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.totalPedidos()).isZero();
        assertThat(r.ticketPromedio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.productosVendidos()).isZero();
        assertThat(r.stockTotal()).isZero();
        assertThat(r.stockBajo()).isZero();
        assertThat(r.ventasPorCategoria()).isEmpty();
        assertThat(r.productosPopulares()).isEmpty();
        assertThat(r.productosMasConsultados()).isEmpty();
        assertThat(r.busquedasMasFrecuentes()).isEmpty();
    }

    @Test
    void unaFallaDeBaseDeDatosSubeEnLugarDeMostrarDatosFalsos() {
        when(repository.sumTotalVentas(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("BD caída"));

        assertThatThrownBy(() -> service.obtenerResumen(DESDE, HASTA, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("BD caída");
    }

    @Test
    void sinVendedorSeConsultaAlcanceGlobal() {
        service.obtenerResumen(DESDE, HASTA, null, null);

        verify(repository).sumTotalVentas(INICIO, FIN, DashboardAnalyticsService.ESTADOS_VENTA, null, null);
    }

    @Test
    void lasVentasContabilizanTodoPedidoPagadoYNoLosPendientesNiCancelados() {
        assertThat(DashboardAnalyticsService.ESTADOS_VENTA).containsExactlyInAnyOrder(
                co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.PAGADO,
                co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.REGISTRADO,
                co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.EN_PREPARACION,
                co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.PARCIALMENTE_ENVIADO,
                co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.ENVIADO);
    }

    @Test
    void categoriaInvalidaLanza400SinConsultarLaBase() {
        assertThatThrownBy(() -> service.obtenerResumen(DESDE, HASTA, "Zapatos", 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("categoría");
        verifyNoInteractions(repository);
    }

    @Test
    void fechaInicialPosteriorALaFinalLanzaExcepcion() {
        assertThatThrownBy(() -> service.obtenerResumen(HASTA, DESDE, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("fecha inicial");
    }

    @Test
    void masConsultadosUsaVistasYRespetaVendedorYCategoria() {
        when(repository.productosMasConsultados(eq(INICIO), eq(FIN), eq(3L), eq(CategoriaPrenda.VESTIDOS),
                any(Pageable.class)))
                .thenReturn(List.of(new DashboardSerieResponse("Vestido Rosa", 8L)));

        List<DashboardSerieResponse> r = service.obtenerProductosMasConsultados(DESDE, HASTA, "VESTIDOS", 3L);

        assertThat(r).hasSize(1);
        assertThat(r.get(0).cantidad()).isEqualTo(8L);
        assertThat(r.get(0).value()).isEqualByComparingTo("8");
    }

    @Test
    void masVendidosRespetaVendedorYCategoria() {
        service.obtenerProductosMasVendidos(DESDE, HASTA, null, 3L);

        verify(repository).productosMasVendidos(eq(INICIO), eq(FIN), eq(DashboardAnalyticsService.ESTADOS_VENTA),
                eq(3L), isNull(), any(Pageable.class));
    }
}
