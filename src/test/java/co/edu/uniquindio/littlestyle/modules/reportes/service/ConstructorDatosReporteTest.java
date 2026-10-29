package co.edu.uniquindio.littlestyle.modules.reportes.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardDemandaDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardOverviewResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardSerieResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.service.DashboardAnalyticsService;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.*;
import co.edu.uniquindio.littlestyle.modules.reportes.repository.ReporteInventarioRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConstructorDatosReporteTest {

    @Mock private DashboardAnalyticsService dashboard;
    @Mock private ReporteInventarioRepository inventarioRepository;
    @InjectMocks private ConstructorDatosReporte constructor;

    private final AuthenticatedUser vendedor = new AuthenticatedUser(7L, "v@t.com", "Vendedor", Rol.VENDEDOR);

    private static DashboardOverviewResponse resumen(BigDecimal ventas, long pedidos, long unidades,
                                                     List<DashboardSerieResponse> categorias,
                                                     List<DashboardSerieResponse> tallas,
                                                     List<DashboardSerieResponse> productos,
                                                     List<DashboardDemandaDetalleResponse> detalle) {
        return new DashboardOverviewResponse(ventas, pedidos, BigDecimal.ZERO, unidades, null, 300L, 5L,
                categorias, tallas, productos, List.of(), detalle);
    }

    @Test
    void ventasReutilizaElResumenDelDashboardConElAlcanceRecibido() {
        when(dashboard.obtenerResumen(any(), any(), isNull(), eq(7L))).thenReturn(resumen(
                new BigDecimal("1500000"), 12L, 40L,
                List.of(new DashboardSerieResponse("VESTIDOS", new BigDecimal("900000"), 20L)),
                List.of(),
                List.of(new DashboardSerieResponse("Vestido floral", new BigDecimal("500000"), 10L)),
                List.of()));

        ReporteDatos datos = constructor.construir(
                new SolicitudReporte(TipoReporte.VENTAS, FormatoReporte.PDF, null, null, null), 7L, vendedor);

        assertThat(datos.nombreArchivo()).startsWith("reporte-ventas_");
        assertThat(datos.filas()).anySatisfy(f -> assertThat(f.get("etiqueta")).isEqualTo("Ventas totales"));
        assertThat(datos.filas()).anySatisfy(f -> assertThat(f.get("etiqueta")).isEqualTo("Vestido floral"));
        assertThat(datos.filas()).noneSatisfy(f -> assertThat(f.get("seccion")).isEqualTo("Ventas por categoría"));
        assertThat(datos.filas()).hasSize(4);
        assertThat(datos.parametros()).containsEntry("GENERADO_POR", "Vendedor");
        assertThat(datos.parametros()).containsEntry("ALCANCE", "Tienda del vendedor");
    }

    @Test
    void ventasSinDatosIncluyeUnaFilaExplicativaEnVezDeFallar() {
        when(dashboard.obtenerResumen(any(), any(), isNull(), any())).thenReturn(resumen(
                BigDecimal.ZERO, 0, 0, List.of(), List.of(), List.of(), List.of()));

        ReporteDatos datos = constructor.construir(
                new SolicitudReporte(TipoReporte.VENTAS, FormatoReporte.PDF, null, null, null), 7L, vendedor);

        assertThat(datos.filas()).anySatisfy(f ->
                assertThat(f.get("etiqueta")).isEqualTo("Sin ventas registradas en el período"));
    }

    @Test
    void demandaIncluyePrendasTallasYNoFormateaUnidadesComoDinero() {
        when(dashboard.obtenerResumen(any(), any(), isNull(), eq(7L))).thenReturn(resumen(
                BigDecimal.ZERO, 0, 24L, List.of(),
                List.of(new DashboardSerieResponse("4T", new BigDecimal("240000"), 12L)),
                List.of(),
                List.of(new DashboardDemandaDetalleResponse("Vestido floral", "4T", 12L, new BigDecimal("240000")))));
        ReporteDatos datos = constructor.construir(
                new SolicitudReporte(TipoReporte.DEMANDA, FormatoReporte.PDF, null, null, null), 7L, vendedor);

        assertThat(datos.filas()).anySatisfy(f -> {
            assertThat(f.get("seccion")).isEqualTo("Demanda por prenda y talla");
            assertThat(f.get("etiqueta")).isEqualTo("Vestido floral");
            assertThat(f.get("cantidad")).isEqualTo("12");
            assertThat(f.get("valor")).isEqualTo("Talla 4T · COP $ 240.000");
        });
        assertThat(datos.filas()).hasSize(1);
        assertThat((String) datos.parametros().get("SUBTITULO")).doesNotContain("Mi tienda");
        assertThat(datos.parametros()).containsEntry("ALCANCE", "Tienda del vendedor");
    }

    @Test
    void elFiltroDeCategoriaSePropagaAlDashboardYAlInventario() {
        when(dashboard.obtenerResumen(any(), any(), eq(CategoriaPrenda.VESTIDOS), eq(7L))).thenReturn(resumen(
                BigDecimal.ZERO, 0, 0, List.of(), List.of(), List.of(), List.of()));

        constructor.construir(
                new SolicitudReporte(TipoReporte.VENTAS, FormatoReporte.PDF, null, null, "vestidos"), 7L, vendedor);

        when(inventarioRepository.filasInventario(eq(EstadoPrenda.ACTIVA), eq(7L), eq(CategoriaPrenda.VESTIDOS)))
                .thenReturn(List.of());
        ReporteDatos datosInventario = constructor.construir(
                new SolicitudReporte(TipoReporte.INVENTARIO, FormatoReporte.PDF, null, null, "VESTIDOS"), 7L, vendedor);
        assertThat((String) datosInventario.parametros().get("SUBTITULO")).contains("Vestidos");
    }

    @Test
    void inventarioMarcaNivelesYCuentaStockBajo() {
        when(inventarioRepository.filasInventario(eq(EstadoPrenda.ACTIVA), eq(7L), isNull())).thenReturn(List.of(
                new InventarioFila("Vestido", "Mi tienda", "VESTIDOS", "4T", 2, 5),
                new InventarioFila("Pijama", "Mi tienda", "PIJAMAS", "6T", 50, null)));

        ReporteDatos datos = constructor.construir(
                new SolicitudReporte(TipoReporte.INVENTARIO, FormatoReporte.PDF, null, null, null), 7L, vendedor);

        assertThat(datos.filas()).hasSize(2);
        assertThat(datos.filas().get(1).get("nivel")).isEqualTo("Sin mínimo");
        assertThat((String) datos.parametros().get("SUBTITULO"))
                .isEqualTo("Existencias activas por prenda y talla · Todas las categorías");
        assertThat(datos.parametros()).containsEntry("TOTAL_UNIDADES", "52").containsEntry("TOTAL_BAJOS", "1");
    }

    @Test
    void inventarioMuyGrandeSeRechazaConUnMensajeClaro() {
        List<InventarioFila> demasiadas = Collections.nCopies(
                ConstructorDatosReporte.MAX_FILAS + 1, new InventarioFila("P", "T", "VESTIDOS", "4T", 1, 1));
        when(inventarioRepository.filasInventario(any(), any(), any())).thenReturn(demasiadas);

        assertThatThrownBy(() -> constructor.construir(
                new SolicitudReporte(TipoReporte.INVENTARIO, FormatoReporte.PDF, null, null, null), null,
                new AuthenticatedUser(1L, "a@t.com", "Admin", Rol.ADMINISTRADOR)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void fechasInvertidasDevuelven400() {
        assertThatThrownBy(() -> constructor.construir(new SolicitudReporte(TipoReporte.DEMANDA, FormatoReporte.PDF,
                LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 1), null), 7L, vendedor))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void categoriaInvalidaDevuelve400() {
        assertThatThrownBy(() -> constructor.construir(new SolicitudReporte(TipoReporte.VENTAS, FormatoReporte.PDF,
                null, null, "NO_EXISTE"), 7L, vendedor))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getMessage()).contains("categoría");
                });
    }

    @Test
    void monedaUsaFormatoDePesosColombianos() {
        assertThat(ConstructorDatosReporte.moneda(new BigDecimal("12000"))).isEqualTo("COP $ 12.000");
        assertThat(ConstructorDatosReporte.moneda(new BigDecimal("1234567"))).isEqualTo("COP $ 1.234.567");
    }
}
