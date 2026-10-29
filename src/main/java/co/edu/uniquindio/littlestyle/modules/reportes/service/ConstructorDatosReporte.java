package co.edu.uniquindio.littlestyle.modules.reportes.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.NivelStock;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardOverviewResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.service.DashboardAnalyticsService;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.InventarioFila;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ReporteDatos;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.SolicitudReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TipoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.repository.ReporteInventarioRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Arma los datos de cada reporte reutilizando las mismas agregaciones del dashboard (US-15).
 * El alcance llega resuelto desde {@code ReporteService}: {@code vendedorId == null} es global.
 */
@Component
@RequiredArgsConstructor
public class ConstructorDatosReporte {

    static final int MAX_FILAS = 5_000;
    private static final Locale CO = Locale.of("es", "CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", CO);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", CO);

    private final DashboardAnalyticsService dashboard;
    private final ReporteInventarioRepository inventarioRepository;

    @Transactional(readOnly = true)
    public ReporteDatos construir(SolicitudReporte s, Long vendedorId, AuthenticatedUser usuario) {
        CategoriaPrenda categoria = parsearCategoria(s.categoria());
        return switch (s.tipo()) {
            case VENTAS -> ventas(s, categoria, vendedorId, usuario);
            case DEMANDA -> demanda(s, categoria, vendedorId, usuario);
            case INVENTARIO -> inventario(categoria, vendedorId, usuario);
        };
    }

    private ReporteDatos ventas(SolicitudReporte s, CategoriaPrenda categoria, Long vendedorId, AuthenticatedUser u) {
        Periodo p = Periodo.de(s.startDate(), s.endDate());
        DashboardOverviewResponse r = dashboard.obtenerResumen(p.desde(), p.hasta(), categoria, vendedorId);

        List<Map<String, ?>> filas = new ArrayList<>();
        filas.add(fila("Indicadores clave", "Ventas totales", "", moneda(r.totalVentas())));
        filas.add(fila("Indicadores clave", "Pedidos", entero(r.totalPedidos()), ""));
        filas.add(fila("Indicadores clave", "Ticket promedio", "", moneda(r.ticketPromedio())));

        r.productosPopulares().forEach(x ->
                filas.add(fila("Ventas por prenda", x.label(), entero(x.cantidad()), moneda(x.value()))));
        if (r.productosPopulares().isEmpty()) {
            filas.add(fila("Resumen", "Sin ventas registradas en el período", "", ""));
        }
        validarLimite(filas);
        return datos(TipoReporte.VENTAS, "reporte-ventas_" + p.desde() + "_" + p.hasta(),
                subtitulo(p, categoria), vendedorId, u, filas);
    }

    private ReporteDatos demanda(SolicitudReporte s, CategoriaPrenda categoria, Long vendedorId, AuthenticatedUser u) {
        Periodo p = Periodo.de(s.startDate(), s.endDate());
        DashboardOverviewResponse r = dashboard.obtenerResumen(p.desde(), p.hasta(), categoria, vendedorId);

        List<Map<String, ?>> filas = new ArrayList<>();
        r.demandaDetalle().forEach(x ->
                filas.add(fila("Demanda por prenda y talla", x.prenda(), entero(x.unidades()),
                        "Talla " + x.talla() + " · " + moneda(x.valor()))));

        if (r.demandaDetalle().isEmpty()) {
            filas.add(fila("Resumen", "Sin actividad registrada en el período", "", ""));
        }
        validarLimite(filas);
        return datos(TipoReporte.DEMANDA, "reporte-demanda_" + p.desde() + "_" + p.hasta(),
                subtitulo(p, categoria), vendedorId, u, filas);
    }

    private ReporteDatos inventario(CategoriaPrenda categoria, Long vendedorId, AuthenticatedUser u) {
        List<InventarioFila> origen = inventarioRepository.filasInventario(EstadoPrenda.ACTIVA, vendedorId, categoria);
        if (origen.size() > MAX_FILAS) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El inventario supera los " + entero(MAX_FILAS) + " registros. Filtra por categoría.", "categoria");
        }
        long unidades = 0;
        long bajos = 0;
        List<Map<String, ?>> filas = new ArrayList<>();
        for (InventarioFila f : origen) {
            unidades += f.stock();
            boolean bajo = f.stockMinimo() != null && f.stock() <= f.stockMinimo();
            if (bajo) {
                bajos++;
            }
            String nivel = f.stockMinimo() == null ? "Sin mínimo"
                    : legible(NivelStock.calcular(f.stock(), f.stockMinimo()).name());
            Map<String, String> m = new HashMap<>();
            m.put("producto", txt(f.producto()));
            m.put("tienda", txt(f.tienda()));
            m.put("categoria", legible(f.categoria()));
            m.put("talla", txt(f.talla()));
            m.put("stock", entero(f.stock()));
            m.put("minimo", f.stockMinimo() == null ? "—" : entero(f.stockMinimo()));
            m.put("nivel", nivel);
            filas.add(m);
        }
        String cat = categoria == null ? "Todas las categorías" : legible(categoria.name());
        String sub = "Existencias activas por prenda y talla · %s".formatted(cat);
        return datos(TipoReporte.INVENTARIO, "reporte-inventario_" + LocalDate.now(), sub, vendedorId, u, filas,
                Map.of("TOTAL_UNIDADES", entero(unidades), "TOTAL_BAJOS", entero(bajos)));
    }

    // ---- utilidades ----

    private static ReporteDatos datos(TipoReporte tipo, String nombre, String subtitulo,
                                      Long vendedorId, AuthenticatedUser u, List<Map<String, ?>> filas) {
        return datos(tipo, nombre, subtitulo, vendedorId, u, filas, Map.of());
    }

    private static ReporteDatos datos(TipoReporte tipo, String nombre, String subtitulo,
                                      Long vendedorId, AuthenticatedUser u, List<Map<String, ?>> filas,
                                      Map<String, Object> extra) {
        Map<String, Object> params = new HashMap<>();
        params.put("TITULO", tipo.getTitulo());
        params.put("SUBTITULO", subtitulo);
        params.put("ALCANCE", vendedorId == null ? "Consolidado global" : "Tienda del vendedor");
        params.put("GENERADO_EN", LocalDateTime.now().format(FECHA_HORA));
        params.put("GENERADO_POR", u.nombre() != null ? u.nombre() : u.email());
        params.put("TOTAL_REGISTROS", entero(filas.size()));
        params.putAll(extra);
        return new ReporteDatos(tipo, tipo.getTitulo(), nombre, params, filas);
    }

    private static String subtitulo(Periodo p, CategoriaPrenda categoria) {
        String cat = categoria == null ? "Todas las categorías" : legible(categoria.name());
        return "Del %s al %s · %s".formatted(p.desde().format(FECHA), p.hasta().format(FECHA), cat);
    }

    private static Map<String, ?> fila(String seccion, String etiqueta, String cantidad, String valor) {
        return Map.of("seccion", txt(seccion), "etiqueta", txt(etiqueta), "cantidad", txt(cantidad), "valor", txt(valor));
    }

    static CategoriaPrenda parsearCategoria(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return CategoriaPrenda.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La categoría no es válida", "categoria");
        }
    }

    static String txt(String v) {
        return Objects.toString(v, "");
    }

    static String legible(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return "";
        }
        String t = codigo.replace('_', ' ').toLowerCase(CO);
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    /** Formato explícito de pesos colombianos con separador de miles. */
    static String moneda(BigDecimal v) {
        return "COP $ " + NumberFormat.getIntegerInstance(CO).format(v == null ? BigDecimal.ZERO : v);
    }

    private static void validarLimite(List<Map<String, ?>> filas) {
        if (filas.size() > MAX_FILAS) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El reporte supera los " + entero(MAX_FILAS) + " registros. Filtra por categoría o período.",
                    "categoria");
        }
    }

    static String entero(Number n) {
        return NumberFormat.getIntegerInstance(CO).format(n == null ? 0 : n);
    }

    record Periodo(LocalDate desde, LocalDate hasta) {
        static Periodo de(LocalDate d, LocalDate h) {
            LocalDate desde = d != null ? d : LocalDate.now().minusDays(30);
            LocalDate hasta = h != null ? h : LocalDate.now();
            if (desde.isAfter(hasta)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "La fecha inicial no puede ser posterior a la final", "startDate");
            }
            return new Periodo(desde, hasta);
        }
    }
}
