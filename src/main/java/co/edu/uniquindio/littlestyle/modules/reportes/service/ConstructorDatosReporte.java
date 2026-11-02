package co.edu.uniquindio.littlestyle.modules.reportes.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.NivelStock;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardOverviewResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardVentasDiaResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.service.DashboardAnalyticsService;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.BusquedaResumenFila;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ColumnaReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.InventarioFila;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ReporteDatos;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.SolicitudReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TablaReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TipoColumna;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TipoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.repository.ReporteBusquedaRepository;
import co.edu.uniquindio.littlestyle.modules.reportes.repository.ReporteInventarioRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Arma los datos de cada reporte reutilizando las mismas agregaciones del dashboard (US-15).
 * El alcance llega resuelto desde {@code ReporteService}: {@code vendedorId == null} es global.
 *
 * <p>Produce dos vistas de la misma información: {@code filas} ya formateadas para el PDF y
 * {@code tabla}, con valores crudos y tipados para XLSX/CSV. El formato "COP $ ..." se aplica
 * únicamente al construir las filas del PDF; la tabla conserva {@link BigDecimal}.</p>
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
    private final ReporteBusquedaRepository busquedaRepository;

    @Transactional(readOnly = true)
    public ReporteDatos construir(SolicitudReporte s, Long vendedorId, AuthenticatedUser usuario) {
        CategoriaPrenda categoria = parsearCategoria(s.categoria());
        return switch (s.tipo()) {
            case VENTAS -> ventas(s, categoria, vendedorId, usuario);
            case DEMANDA -> demanda(s, categoria, vendedorId, usuario);
            case INVENTARIO -> inventario(categoria, vendedorId, usuario);
            case BUSQUEDAS -> busquedas(s, categoria, vendedorId, usuario);
        };
    }

    private ReporteDatos ventas(SolicitudReporte s, CategoriaPrenda categoria, Long vendedorId, AuthenticatedUser u) {
        Periodo p = Periodo.de(s.startDate(), s.endDate());
        DashboardOverviewResponse r = dashboard.obtenerResumen(p.desde(), p.hasta(), categoria, vendedorId);
        List<DashboardVentasDiaResponse> diarias =
                dashboard.obtenerVentasPorDia(p.desde(), p.hasta(), categoria, vendedorId);

        List<Map<String, ?>> filas = new ArrayList<>();
        List<List<Object>> crudas = new ArrayList<>();

        agregar(filas, crudas,
                fila("Indicadores clave", "Ventas totales", "", moneda(r.totalVentas())),
                "Indicadores clave", "Ventas totales", null, r.totalVentas());
        agregar(filas, crudas,
                fila("Indicadores clave", "Pedidos", entero(r.totalPedidos()), ""),
                "Indicadores clave", "Pedidos", r.totalPedidos(), null);
        agregar(filas, crudas,
                fila("Indicadores clave", "Ticket promedio", "", moneda(r.ticketPromedio())),
                "Indicadores clave", "Ticket promedio", null, r.ticketPromedio());

        long maximoDiario = diarias.stream()
                .map(DashboardVentasDiaResponse::total)
                .filter(Objects::nonNull)
                .mapToLong(BigDecimal::longValue)
                .max()
                .orElse(0L);
        for (DashboardVentasDiaResponse d : diarias) {
            Map<String, Object> m = new HashMap<>(fila(
                    "Ventas por día", d.fecha().format(FECHA), entero(d.pedidos()), moneda(d.total())));
            // Ancho de la barra en 10 tramos (0-10): la plantilla repite un carácter de bloque
            // U+2588 por tramo, así el ancho total es proporcional al total del día.
            long totalDia = d.total() == null ? 0L : d.total().longValue();
            m.put("nivelBarra", maximoDiario == 0 ? 0
                    : Math.max(0, Math.min(10, Math.round(10.0f * totalDia / maximoDiario))));
            agregar(filas, crudas, m,
                    "Ventas por día", d.fecha(), d.pedidos(), d.total());
        }

        r.productosPopulares().forEach(x -> agregar(filas, crudas,
                fila("Ventas por prenda", x.label(), entero(x.cantidad()), moneda(x.value())),
                "Ventas por prenda", x.label(), x.cantidad(), x.value()));

        if (r.productosPopulares().isEmpty()) {
            agregar(filas, crudas,
                    fila("Resumen", "Sin ventas registradas en el período", "", ""),
                    "Resumen", "Sin ventas registradas en el período", null, null);
        }
        validarLimite(filas);

        TablaReporte tabla = tabla(
                new ColumnaReporte("Sección", TipoColumna.TEXTO),
                new ColumnaReporte("Concepto", TipoColumna.TEXTO),
                new ColumnaReporte("Cantidad", TipoColumna.ENTERO),
                new ColumnaReporte("Valor", TipoColumna.MONEDA), crudas);
        return datos(TipoReporte.VENTAS, "reporte-ventas_" + p.desde() + "_" + p.hasta(),
                subtitulo(p, categoria), vendedorId, u, filas, tabla,
                Map.of("TOTAL_VENTAS", moneda(r.totalVentas()),
                        "TOTAL_PEDIDOS", entero(r.totalPedidos()),
                        "TICKET_PROMEDIO", moneda(r.ticketPromedio())));
    }

    private ReporteDatos demanda(SolicitudReporte s, CategoriaPrenda categoria, Long vendedorId, AuthenticatedUser u) {
        Periodo p = Periodo.de(s.startDate(), s.endDate());
        DashboardOverviewResponse r = dashboard.obtenerResumen(p.desde(), p.hasta(), categoria, vendedorId);

        List<Map<String, ?>> filas = new ArrayList<>();
        List<List<Object>> crudas = new ArrayList<>();
        r.demandaDetalle().forEach(x -> {
            Map<String, Object> m = new HashMap<>();
            m.put("seccion", "Demanda por prenda y talla");
            m.put("etiqueta", txt(x.prenda()));
            m.put("cantidad", entero(x.unidades()));
            m.put("valor", "Talla " + txt(x.talla()) + " · " + moneda(x.valor()));
            m.put("prenda", txt(x.prenda()));
            m.put("talla", txt(x.talla()));
            m.put("unidades", entero(x.unidades()));
            m.put("unidadesNum", x.unidades());
            m.put("valorMoneda", moneda(x.valor()));
            m.put("valorNum", x.valor() == null ? BigDecimal.ZERO : x.valor());
            agregar(filas, crudas, m, x.prenda(), x.talla(), x.unidades(), x.valor());
        });

        if (r.demandaDetalle().isEmpty()) {
            agregar(filas, crudas,
                    fila("Resumen", "Sin actividad registrada en el período", "", ""),
                    "Resumen", "Sin actividad registrada en el período", null, null);
        }
        validarLimite(filas);

        TablaReporte tabla = tabla(
                new ColumnaReporte("Prenda", TipoColumna.TEXTO),
                new ColumnaReporte("Talla", TipoColumna.TEXTO),
                new ColumnaReporte("Unidades", TipoColumna.ENTERO),
                new ColumnaReporte("Valor", TipoColumna.MONEDA), crudas);
        return datos(TipoReporte.DEMANDA, "reporte-demanda_" + p.desde() + "_" + p.hasta(),
                subtitulo(p, categoria), vendedorId, u, filas, tabla);
    }

    private ReporteDatos inventario(CategoriaPrenda categoria, Long vendedorId, AuthenticatedUser u) {
        // 5.1: el límite se valida ANTES de traer las filas a memoria.
        long total = inventarioRepository.countFilasInventario(EstadoPrenda.ACTIVA, vendedorId, categoria);
        if (total > MAX_FILAS) {
            throw limiteSuperado("inventario");
        }
        List<InventarioFila> origen = inventarioRepository.filasInventario(
                EstadoPrenda.ACTIVA, vendedorId, categoria, PageRequest.of(0, MAX_FILAS + 1));
        if (origen.size() > MAX_FILAS) {
            throw limiteSuperado("inventario");
        }

        long unidades = 0;
        long bajos = 0;
        List<Map<String, ?>> filas = new ArrayList<>();
        List<List<Object>> crudas = new ArrayList<>();
        for (InventarioFila f : origen) {
            unidades += f.stock();
            boolean bajo = f.stockMinimo() != null && f.stock() <= f.stockMinimo();
            if (bajo) {
                bajos++;
            }
            String nivel = f.stockMinimo() == null ? "Sin mínimo"
                    : legible(NivelStock.calcular(f.stock(), f.stockMinimo()).name());
            Map<String, Object> m = new HashMap<>();
            m.put("producto", txt(f.producto()));
            m.put("tienda", txt(f.tienda()));
            m.put("categoria", legible(f.categoria()));
            m.put("talla", txt(f.talla()));
            m.put("stock", entero(f.stock()));
            m.put("minimo", f.stockMinimo() == null ? "—" : entero(f.stockMinimo()));
            m.put("nivel", nivel);
            agregar(filas, crudas, m, f.producto(), f.tienda(), legible(f.categoria()), f.talla(),
                    f.stock(), f.stockMinimo(), nivel);
        }
        String cat = categoria == null ? "Todas las categorías" : legible(categoria.name());
        String sub = "Existencias activas por prenda y talla · %s".formatted(cat);
        TablaReporte tabla = tabla(
                new ColumnaReporte("Prenda", TipoColumna.TEXTO),
                new ColumnaReporte("Tienda", TipoColumna.TEXTO),
                new ColumnaReporte("Categoría", TipoColumna.TEXTO),
                new ColumnaReporte("Talla", TipoColumna.TEXTO),
                new ColumnaReporte("Stock", TipoColumna.ENTERO),
                new ColumnaReporte("Mínimo", TipoColumna.ENTERO),
                new ColumnaReporte("Nivel", TipoColumna.TEXTO), crudas);
        return datos(TipoReporte.INVENTARIO, "reporte-inventario_" + LocalDate.now(), sub, vendedorId, u, filas,
                tabla, Map.of("TOTAL_UNIDADES", entero(unidades), "TOTAL_BAJOS", entero(bajos)));
    }

    /**
     * Reporte de intención de búsqueda. No confundir con DEMANDA: aquí se mide qué buscaron los
     * usuarios y cuántas veces no encontraron nada, no cuántas unidades se vendieron.
     */
    private ReporteDatos busquedas(SolicitudReporte s, CategoriaPrenda categoria, Long vendedorId, AuthenticatedUser u) {
        Periodo p = Periodo.de(s.startDate(), s.endDate());
        LocalDateTime inicio = p.desde().atStartOfDay();
        LocalDateTime fin = p.hasta().plusDays(1).atStartOfDay();

        long terminos = busquedaRepository.contarTerminos(inicio, fin, categoria, vendedorId);
        if (terminos > MAX_FILAS) {
            throw limiteSuperado("búsquedas");
        }
        List<Object[]> resumen = busquedaRepository.resumenTerminos(
                inicio, fin, categoria, vendedorId, PageRequest.of(0, MAX_FILAS + 1));
        if (resumen.size() > MAX_FILAS) {
            throw limiteSuperado("búsquedas");
        }

        long totalBusquedas = 0;
        long totalSinResultados = 0;
        List<Map<String, ?>> filas = new ArrayList<>();
        List<List<Object>> crudas = new ArrayList<>();
        for (Object[] fila : resumen) {
            BusquedaResumenFila b = new BusquedaResumenFila(
                    txt((String) fila[0]), largo(fila[1]), largo(fila[2]), fechaHora(fila[3]));
            totalBusquedas += b.busquedas();
            totalSinResultados += b.sinResultados();
            Map<String, Object> m = new HashMap<>();
            m.put("termino", b.termino());
            m.put("busquedas", entero(b.busquedas()));
            m.put("sinResultados", entero(b.sinResultados()));
            m.put("ultima", b.ultimaBusqueda() == null ? "—" : b.ultimaBusqueda().format(FECHA_HORA));
            agregar(filas, crudas, m, b.termino(), b.busquedas(), b.sinResultados(),
                    b.ultimaBusqueda() == null ? null : b.ultimaBusqueda().toLocalDate());
        }
        if (resumen.isEmpty()) {
            Map<String, Object> vacio = new HashMap<>();
            vacio.put("seccion", "Resumen");
            vacio.put("etiqueta", "Sin búsquedas registradas en el período");
            vacio.put("termino", "Sin búsquedas registradas en el período");
            vacio.put("busquedas", "");
            vacio.put("sinResultados", "");
            vacio.put("ultima", "—");
            agregar(filas, crudas, vacio,
                    "Sin búsquedas registradas en el período", null, null, null);
        }

        TablaReporte tabla = tabla(
                new ColumnaReporte("Término", TipoColumna.TEXTO),
                new ColumnaReporte("Búsquedas", TipoColumna.ENTERO),
                new ColumnaReporte("Sin resultados", TipoColumna.ENTERO),
                new ColumnaReporte("Última búsqueda", TipoColumna.FECHA), crudas);
        return datos(TipoReporte.BUSQUEDAS, "reporte-busquedas_" + p.desde() + "_" + p.hasta(),
                subtitulo(p, categoria), vendedorId, u, filas, tabla,
                Map.of("TOTAL_BUSQUEDAS", entero(totalBusquedas),
                        "TOTAL_SIN_RESULTADOS", entero(totalSinResultados)));
    }

    // ---- utilidades ----

    private static ReporteDatos datos(TipoReporte tipo, String nombre, String subtitulo,
                                      Long vendedorId, AuthenticatedUser u, List<Map<String, ?>> filas,
                                      TablaReporte tabla) {
        return datos(tipo, nombre, subtitulo, vendedorId, u, filas, tabla, Map.of());
    }

    private static ReporteDatos datos(TipoReporte tipo, String nombre, String subtitulo,
                                      Long vendedorId, AuthenticatedUser u, List<Map<String, ?>> filas,
                                      TablaReporte tabla, Map<String, Object> extra) {
        Map<String, Object> params = new HashMap<>();
        params.put("TITULO", tipo.getTitulo());
        params.put("SUBTITULO", subtitulo);
        params.put("ALCANCE", vendedorId == null ? "Consolidado global" : "Tienda del vendedor");
        params.put("GENERADO_EN", LocalDateTime.now().format(FECHA_HORA));
        params.put("GENERADO_POR", u.nombre() != null ? u.nombre() : u.email());
        params.put("TOTAL_REGISTROS", entero(filas.size()));
        params.putAll(extra);
        return new ReporteDatos(tipo, tipo.getTitulo(), nombre, params, filas, tabla);
    }

    private static String subtitulo(Periodo p, CategoriaPrenda categoria) {
        String cat = categoria == null ? "Todas las categorías" : legible(categoria.name());
        return "Del %s al %s · %s".formatted(p.desde().format(FECHA), p.hasta().format(FECHA), cat);
    }

    private static Map<String, ?> fila(String seccion, String etiqueta, String cantidad, String valor) {
        return Map.of("seccion", txt(seccion), "etiqueta", txt(etiqueta), "cantidad", txt(cantidad), "valor", txt(valor));
    }

    private static void agregar(List<Map<String, ?>> filas, List<List<Object>> crudas,
                                Map<String, ?> filaPdf, Object... filaCruda) {
        filas.add(filaPdf);
        crudas.add(Arrays.asList(filaCruda));
    }

    private static TablaReporte tabla(ColumnaReporte c1, ColumnaReporte c2, ColumnaReporte c3,
                                      ColumnaReporte c4, List<List<Object>> filas) {
        return new TablaReporte(List.of(c1, c2, c3, c4), filas);
    }

    private static TablaReporte tabla(ColumnaReporte c1, ColumnaReporte c2, ColumnaReporte c3,
                                      ColumnaReporte c4, ColumnaReporte c5, ColumnaReporte c6,
                                      ColumnaReporte c7, List<List<Object>> filas) {
        return new TablaReporte(List.of(c1, c2, c3, c4, c5, c6, c7), filas);
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

    /** Formato explícito de pesos colombianos con separador de miles (solo capa PDF). */
    static String moneda(BigDecimal v) {
        return "COP $ " + NumberFormat.getIntegerInstance(CO).format(v == null ? BigDecimal.ZERO : v);
    }

    private static void validarLimite(List<Map<String, ?>> filas) {
        if (filas.size() > MAX_FILAS) {
            throw limiteSuperado("reporte");
        }
    }

    private static BusinessException limiteSuperado(String nombre) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                "El " + nombre + " supera los " + entero(MAX_FILAS) + " registros. Filtra por categoría o período.",
                "categoria");
    }

    static String entero(Number n) {
        return NumberFormat.getIntegerInstance(CO).format(n == null ? 0 : n);
    }

    private static long largo(Object valor) {
        return switch (valor) {
            case null -> 0L;
            case Number n -> n.longValue();
            case String s -> Long.parseLong(s.trim());
            default -> 0L;
        };
    }

    private static LocalDateTime fechaHora(Object valor) {
        return switch (valor) {
            case null -> null;
            case LocalDateTime d -> d;
            case java.sql.Timestamp t -> t.toLocalDateTime();
            case java.sql.Date d -> d.toLocalDate().atStartOfDay();
            case LocalDate d -> d.atStartOfDay();
            case String s -> LocalDateTime.parse(s);
            default -> LocalDateTime.parse(valor.toString());
        };
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
