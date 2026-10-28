package co.edu.uniquindio.littlestyle.modules.dashboard.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardOverviewResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardSerieResponse;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.DashboardAnalyticsRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Agregaciones analíticas del dashboard (US-15).
 *
 * <p>Nunca fabrica cifras: sin datos devuelve ceros y listas vacías, y si la base falla la excepción sube
 * para que el cliente vea un error y no un dashboard "normal" con números inventados.
 *
 * <p>{@code vendedorId} limita todo al negocio de ese vendedor; nulo es alcance global y solo debe pasarlo
 * quien ya verificó que el usuario es administrador (ver {@code DashboardController}).
 */
@Service
@RequiredArgsConstructor
public class DashboardAnalyticsService {

    /** Una venta cuenta desde que el pago se confirma: cualquier estado posterior, salvo cancelado. */
    static final Set<EstadoPedido> ESTADOS_VENTA = Set.of(
            EstadoPedido.PAGADO,
            EstadoPedido.REGISTRADO,
            EstadoPedido.EN_PREPARACION,
            EstadoPedido.PARCIALMENTE_ENVIADO,
            EstadoPedido.ENVIADO);

    static final int TAMANO_TOP = 10;
    private static final int DIAS_POR_DEFECTO = 30;

    private final DashboardAnalyticsRepository repository;

    @Transactional(readOnly = true)
    public DashboardOverviewResponse obtenerResumen(LocalDate startDate, LocalDate endDate,
                                                    String categoria, Long vendedorId) {
        Rango rango = Rango.de(startDate, endDate);
        CategoriaPrenda cat = parsearCategoria(categoria);

        BigDecimal totalVentas = ceroSiNulo(repository.sumTotalVentas(
                rango.inicio, rango.fin, ESTADOS_VENTA, vendedorId, cat));
        long totalPedidos = repository.countPedidos(rango.inicio, rango.fin, ESTADOS_VENTA, vendedorId, cat);
        long productosVendidos = ceroSiNulo(repository.sumUnidadesVendidas(
                rango.inicio, rango.fin, ESTADOS_VENTA, vendedorId, cat));
        BigDecimal ticketPromedio = totalPedidos == 0
                ? BigDecimal.ZERO
                : totalVentas.divide(BigDecimal.valueOf(totalPedidos), 2, RoundingMode.HALF_UP);

        return new DashboardOverviewResponse(
                totalVentas,
                totalPedidos,
                ticketPromedio,
                productosVendidos,
                null, // el catálogo no guarda costos: no hay margen que calcular
                ceroSiNulo(repository.sumStockTotal(EstadoPrenda.ACTIVA, vendedorId, cat)),
                repository.countStockBajo(EstadoPrenda.ACTIVA, vendedorId, cat),
                repository.ventasPorCategoria(rango.inicio, rango.fin, ESTADOS_VENTA, vendedorId, cat),
                repository.demandaPorTalla(rango.inicio, rango.fin, ESTADOS_VENTA, vendedorId, cat),
                repository.productosMasVendidos(rango.inicio, rango.fin, ESTADOS_VENTA, vendedorId, cat, top()),
                repository.productosMasConsultados(rango.inicio, rango.fin, vendedorId, cat, top()),
                repository.busquedasMasFrecuentes(rango.inicio, rango.fin, top()));
    }

    @Transactional(readOnly = true)
    public List<DashboardSerieResponse> obtenerProductosMasConsultados(LocalDate startDate, LocalDate endDate,
                                                                       String categoria, Long vendedorId) {
        Rango rango = Rango.de(startDate, endDate);
        return repository.productosMasConsultados(rango.inicio, rango.fin, vendedorId,
                parsearCategoria(categoria), top());
    }

    @Transactional(readOnly = true)
    public List<DashboardSerieResponse> obtenerProductosMasVendidos(LocalDate startDate, LocalDate endDate,
                                                                    String categoria, Long vendedorId) {
        Rango rango = Rango.de(startDate, endDate);
        return repository.productosMasVendidos(rango.inicio, rango.fin, ESTADOS_VENTA, vendedorId,
                parsearCategoria(categoria), top());
    }

    private static Pageable top() {
        return PageRequest.of(0, TAMANO_TOP);
    }

    /** Convierte el filtro de categoría; nulo si no se envió y 400 si no existe. */
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

    private static BigDecimal ceroSiNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static long ceroSiNulo(Long valor) {
        return valor == null ? 0L : valor;
    }

    /** Rango de fechas [inicio, fin) listo para consultar; por defecto, los últimos 30 días. */
    private record Rango(LocalDateTime inicio, LocalDateTime fin) {

        static Rango de(LocalDate startDate, LocalDate endDate) {
            LocalDate desde = startDate != null ? startDate : LocalDate.now().minusDays(DIAS_POR_DEFECTO);
            LocalDate hasta = endDate != null ? endDate : LocalDate.now();
            if (desde.isAfter(hasta)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "La fecha inicial no puede ser posterior a la final", "startDate");
            }
            return new Rango(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
        }
    }
}
