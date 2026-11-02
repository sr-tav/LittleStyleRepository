package co.edu.uniquindio.littlestyle.modules.reportes.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardDemandaDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardOverviewResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardSerieResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardVentasDiaResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.service.DashboardAnalyticsService;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.*;
import co.edu.uniquindio.littlestyle.modules.reportes.repository.ReporteBusquedaRepository;
import co.edu.uniquindio.littlestyle.modules.reportes.repository.ReporteInventarioRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConstructorDatosReporteTest {

    @Mock private DashboardAnalyticsService dashboard;
    @Mock private ReporteInventarioRepository inventarioRepository;
    @Mock private ReporteBusquedaRepository busquedaRepository;
    @InjectMocks private ConstructorDatosReporte constructor;

    @org.junit.jupiter.api.BeforeEach
    void prepararVentasSinProductosPorDefecto() {
        org.mockito.Mockito.lenient()
                .when(dashboard.obtenerProductosVendidosCompleto(
                        nullable(LocalDate.class), nullable(LocalDate.class), nullable(String.class),
                        nullable(Long.class)))
                .thenReturn(List.of());
    }

    private final AuthenticatedUser vendedor = new AuthenticatedUser(7L, "v@t.com", "Vendedor", Rol.VENDEDOR);

    private static DashboardOverviewResponse resumen(BigDecimal ventas, long pedidos, long unidades,
                                                     List<DashboardSerieResponse> categorias,
                                                     List<DashboardSerieResponse> tallas,
                                                     List<DashboardSerieResponse> productos) {
        return new DashboardOverviewResponse(ventas, pedidos, BigDecimal.ZERO, unidades, null, 300L, 5L,
                categorias, tallas, productos, List.of(), List.of());
    }

    @Test
    void ventasReutilizaElResumenDelDashboardConElAlcanceRecibido() {
        when(dashboard.obtenerResumen(any(), any(), isNull(), eq(7L))).thenReturn(resumen(
                new BigDecimal("1500000"), 12L, 40L,
                List.of(new DashboardSerieResponse("VESTIDOS", new BigDecimal("900000"), 20L)),
                List.of(), List.of()));
        when(dashboard.obtenerVentasPorDia(any(), any(), isNull(), eq(7L))).thenReturn(List.of());
        when(dashboard.obtenerProductosVendidosCompleto(any(), any(), isNull(), eq(7L))).thenReturn(
                List.of(new DashboardSerieResponse("Vestido floral", new BigDecimal("500000"), 10L)));

        ReporteDatos datos = constructor.construir(
                new SolicitudReporte(TipoReporte.VENTAS, FormatoReporte.PDF, null, null, null), 7L, vendedor);

        assertThat(datos.nombreArchivo()).startsWith("reporte-ventas_");
        assertThat(datos.filas()).anySatisfy(f -> assertThat(f.get("etiqueta")).isEqualTo("Ventas totales"));
        assertThat(datos.filas()).anySatisfy(f -> assertThat(f.get("etiqueta")).isEqualTo("Vestido floral"));
        assertThat(datos.filas()).noneSatisfy(f -> assertThat(f.get("seccion")).isEqualTo("Ventas por categoría"));
        assertThat(datos.filas()).hasSize(4);
        assertThat(datos.parametros()).containsEntry("GENERADO_POR", "Vendedor");
        assertThat(datos.parametros()).containsEntry("ALCANCE", "Tienda del vendedor");
        assertThat(datos.parametros()).containsEntry("TOTAL_VENTAS", "COP $ 1.500.000")
                .containsEntry("TOTAL_PEDIDOS", "12");
    }

    @Test
    void ventasIncluyeSerieDiariaConNivelDeBarraProporcional() {
        when(dashboard.obtenerResumen(any(), any(), isNull(), eq(7L))).thenReturn(resumen(
                new BigDecimal("300000"), 3, 3, List.of(), List.of(), List.of()));
        when(dashboard.obtenerVentasPorDia(any(), any(), isNull(), eq(7L))).thenReturn(List.of(
                new DashboardVentasDiaResponse(LocalDate.of(2026, 10, 1), 2, new BigDecimal("200000")),
                new DashboardVentasDiaResponse(LocalDate.of(2026, 10, 2), 1, new BigDecimal("100000"))));
        when(dashboard.obtenerProductosVendidosCompleto(any(), any(), isNull(), eq(7L))).thenReturn(List.of());

        ReporteDatos datos = constructor.construir(
                new SolicitudReporte(TipoReporte.VENTAS, FormatoReporte.PDF, null, null, null), 7L, vendedor);

        assertThat(datos.filas())
                .anySatisfy(f -> {
                    assertThat(f.get("etiqueta")).isEqualTo("01/10/2026");
                    assertThat(f.get("nivelBarra")).isEqualTo(10);
                })
                .anySatisfy(f -> {
                    assertThat(f.get("etiqueta")).isEqualTo("02/10/2026");
                    assertThat(f.get("nivelBarra")).isEqualTo(5);
                });
        assertThat(datos.filas()).noneSatisfy(f ->
                assertThat(f.get("seccion")).isEqualTo("Ventas por prenda"));
        assertThat(datos.tabla().filas()).anySatisfy(f -> assertThat(f.get(1)).isEqualTo(LocalDate.of(2026, 10, 1)));
    }

    @Test
    void ventasSinDatosIncluyeUnaFilaExplicativaEnVezDeFallar() {
        when(dashboard.obtenerResumen(any(), any(), isNull(), any())).thenReturn(resumen(
                BigDecimal.ZERO, 0, 0, List.of(), List.of(), List.of()));
        when(dashboard.obtenerVentasPorDia(any(), any(), isNull(), any())).thenReturn(List.of());
        when(dashboard.obtenerProductosVendidosCompleto(any(), any(), isNull(), any())).thenReturn(List.of());

        ReporteDatos datos = constructor.construir(
                new SolicitudReporte(TipoReporte.VENTAS, FormatoReporte.PDF, null, null, null), 7L, vendedor);

        assertThat(datos.filas()).anySatisfy(f ->
                assertThat(f.get("etiqueta")).isEqualTo("Sin ventas registradas en el período"));
    }

    @Test
    void demandaIncluyePrendasTallasYNoFormateaUnidadesComoDinero() {
        when(dashboard.obtenerDemandaDetalle(any(), any(), isNull(), eq(7L))).thenReturn(List.of(
                new DashboardDemandaDetalleResponse("Vestido floral", "4T", 12L, new BigDecimal("240000"))));
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
    void laTablaCrudaConservaTiposParaXlsxYCsv() {
        when(dashboard.obtenerResumen(any(), any(), isNull(), eq(7L))).thenReturn(resumen(
                new BigDecimal("1500000"), 12L, 40L, List.of(), List.of(), List.of()));
        when(dashboard.obtenerVentasPorDia(any(), any(), isNull(), eq(7L))).thenReturn(List.of());
        when(dashboard.obtenerProductosVendidosCompleto(any(), any(), isNull(), eq(7L))).thenReturn(
                List.of(new DashboardSerieResponse("Vestido floral", new BigDecimal("500000"), 10L)));

        ReporteDatos datos = constructor.construir(
                new SolicitudReporte(TipoReporte.VENTAS, FormatoReporte.XLSX, null, null, null), 7L, vendedor);

        assertThat(datos.tabla().columnas()).extracting(ColumnaReporte::tipo)
                .containsExactly(TipoColumna.TEXTO, TipoColumna.TEXTO, TipoColumna.ENTERO, TipoColumna.MONEDA);
        assertThat(datos.tabla().filas()).anySatisfy(f -> {
            assertThat(f.get(1)).isEqualTo("Ventas totales");
            assertThat(f.get(3)).isEqualTo(new BigDecimal("1500000"));
        });
    }

    @Test
    void elFiltroDeCategoriaSePropagaAlDashboardYAlInventario() {
        when(dashboard.obtenerResumen(any(), any(), eq("VESTIDOS"), eq(7L))).thenReturn(resumen(
                BigDecimal.ZERO, 0, 0, List.of(), List.of(), List.of()));
        when(dashboard.obtenerVentasPorDia(any(), any(), eq("VESTIDOS"), eq(7L))).thenReturn(List.of());
        when(dashboard.obtenerProductosVendidosCompleto(any(), any(), eq("VESTIDOS"), eq(7L)))
                .thenReturn(List.of());

        constructor.construir(
                new SolicitudReporte(TipoReporte.VENTAS, FormatoReporte.PDF, null, null, "vestidos"), 7L, vendedor);

        when(inventarioRepository.countFilasInventario(eq(EstadoPrenda.ACTIVA), eq(7L), eq(CategoriaPrenda.VESTIDOS)))
                .thenReturn(0L);
        when(inventarioRepository.filasInventario(eq(EstadoPrenda.ACTIVA), eq(7L), eq(CategoriaPrenda.VESTIDOS),
                any(Pageable.class))).thenReturn(List.of());
        ReporteDatos datosInventario = constructor.construir(
                new SolicitudReporte(TipoReporte.INVENTARIO, FormatoReporte.PDF, null, null, "VESTIDOS"), 7L, vendedor);
        assertThat((String) datosInventario.parametros().get("SUBTITULO")).contains("Vestidos");
    }

    @Test
    void ventasIncluyeLasVeinticincoPrendasVendidasYNoSoloElTopDiez() {
        List<DashboardSerieResponse> vendidos = java.util.stream.IntStream.rangeClosed(1, 25)
                .mapToObj(i -> new DashboardSerieResponse("Prenda " + i, BigDecimal.valueOf(i * 100L), 1L))
                .toList();
        when(dashboard.obtenerResumen(any(), any(), isNull(), eq(7L))).thenReturn(resumen(
                new BigDecimal("32500"), 25, 25, List.of(), List.of(), List.of()));
        when(dashboard.obtenerVentasPorDia(any(), any(), isNull(), eq(7L))).thenReturn(List.of());
        when(dashboard.obtenerProductosVendidosCompleto(any(), any(), isNull(), eq(7L))).thenReturn(vendidos);

        ReporteDatos datos = constructor.construir(
                new SolicitudReporte(TipoReporte.VENTAS, FormatoReporte.CSV, null, null, null), 7L, vendedor);

        assertThat(datos.filas()).filteredOn(f -> "Ventas por prenda".equals(f.get("seccion"))).hasSize(25);
        assertThat(datos.filas()).anySatisfy(f -> assertThat(f.get("etiqueta")).isEqualTo("Prenda 25"));
    }

    @Test
    void inventarioMarcaNivelesYCuentaStockBajo() {
        when(inventarioRepository.countFilasInventario(eq(EstadoPrenda.ACTIVA), eq(7L), isNull())).thenReturn(2L);
        when(inventarioRepository.filasInventario(eq(EstadoPrenda.ACTIVA), eq(7L), isNull(), any(Pageable.class)))
                .thenReturn(List.of(
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
    void inventarioMuyGrandeSeRechazaAntesDeTraerLasFilas() {
        when(inventarioRepository.countFilasInventario(any(), any(), any()))
                .thenReturn((long) ConstructorDatosReporte.MAX_FILAS + 1);

        assertThatThrownBy(() -> constructor.construir(
                new SolicitudReporte(TipoReporte.INVENTARIO, FormatoReporte.PDF, null, null, null), null,
                new AuthenticatedUser(1L, "a@t.com", "Admin", Rol.ADMINISTRADOR)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        verify(inventarioRepository, never()).filasInventario(any(), any(), any(), any());
    }

    @Test
    void inventarioQueCreceEntreElConteoYLaLecturaTambienSeRechaza() {
        List<InventarioFila> demasiadas = Collections.nCopies(
                ConstructorDatosReporte.MAX_FILAS + 1, new InventarioFila("P", "T", "VESTIDOS", "4T", 1, 1));
        when(inventarioRepository.countFilasInventario(any(), any(), any())).thenReturn(1L);
        when(inventarioRepository.filasInventario(any(), any(), any(), any(Pageable.class))).thenReturn(demasiadas);

        assertThatThrownBy(() -> constructor.construir(
                new SolicitudReporte(TipoReporte.INVENTARIO, FormatoReporte.PDF, null, null, null), null,
                new AuthenticatedUser(1L, "a@t.com", "Admin", Rol.ADMINISTRADOR)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void busquedasResumeTerminosYSinResultados() {
        LocalDateTime ultima = LocalDateTime.of(2026, 10, 20, 9, 30);
        when(busquedaRepository.contarTerminos(any(), any(), isNull(), eq(7L))).thenReturn(1L);
        when(busquedaRepository.resumenTerminos(any(), any(), isNull(), eq(7L), any(Pageable.class)))
                .thenReturn(List.<Object[]>of(new Object[]{"vestido rojo", 5L, 2L, ultima}));

        ReporteDatos datos = constructor.construir(
                new SolicitudReporte(TipoReporte.BUSQUEDAS, FormatoReporte.CSV, null, null, null), 7L, vendedor);

        assertThat(datos.filas()).hasSize(1);
        assertThat(datos.filas().getFirst()).satisfies(f -> {
            assertThat(f.get("termino")).isEqualTo("vestido rojo");
            assertThat(f.get("busquedas")).isEqualTo("5");
            assertThat(f.get("sinResultados")).isEqualTo("2");
            assertThat(f.get("ultima")).isEqualTo("20/10/2026 09:30");
        });
        assertThat(datos.parametros()).containsEntry("TOTAL_BUSQUEDAS", "5")
                .containsEntry("TOTAL_SIN_RESULTADOS", "2");
        assertThat(datos.tabla().filas().getFirst().get(3)).isEqualTo(LocalDate.of(2026, 10, 20));
    }

    @Test
    void busquedasSinDatosIncluyeUnaFilaExplicativa() {
        when(busquedaRepository.contarTerminos(any(), any(), isNull(), eq(7L))).thenReturn(0L);
        when(busquedaRepository.resumenTerminos(any(), any(), isNull(), eq(7L), any(Pageable.class)))
                .thenReturn(List.of());

        ReporteDatos datos = constructor.construir(
                new SolicitudReporte(TipoReporte.BUSQUEDAS, FormatoReporte.PDF, null, null, null), 7L, vendedor);

        assertThat(datos.filas()).hasSize(1);
        assertThat(datos.filas().getFirst().get("termino"))
                .isEqualTo("Sin búsquedas registradas en el período");
        assertThat(datos.parametros()).containsEntry("TOTAL_BUSQUEDAS", "0");
    }

    @Test
    void busquedasMuyGrandesSeRechazanAntesDeTraerLasFilas() {
        when(busquedaRepository.contarTerminos(any(), any(), isNull(), eq(7L)))
                .thenReturn((long) ConstructorDatosReporte.MAX_FILAS + 1);

        assertThatThrownBy(() -> constructor.construir(
                new SolicitudReporte(TipoReporte.BUSQUEDAS, FormatoReporte.PDF, null, null, null), 7L, vendedor))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        verify(busquedaRepository, never()).resumenTerminos(any(), any(), any(), any(), any());
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
