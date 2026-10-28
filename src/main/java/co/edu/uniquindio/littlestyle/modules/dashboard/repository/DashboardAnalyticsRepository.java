package co.edu.uniquindio.littlestyle.modules.dashboard.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardSerieResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Consultas analíticas del dashboard de operación.
 *
 * <p>Un pedido puede mezclar prendas de varios vendedores y no guarda vendedor propio, así que todo lo que
 * depende del negocio se filtra por el vendedor de la prenda ({@code pr.vendedor.id}). {@code vendedorId} nulo
 * significa alcance global (solo administrador); {@code categoria} nula significa todas las categorías.
 * Los {@code sum} de una consulta sin filas devuelven {@code null}: el servicio los normaliza a cero.
 */
public interface DashboardAnalyticsRepository extends JpaRepository<Pedido, Long> {

    @Query("""
            select sum(li.subtotal)
            from LineaPedido li
              join li.pedido p
              join Prenda pr on pr.id = li.prendaId
            where p.fechaCreacion >= :inicio and p.fechaCreacion < :fin
              and p.estado in :estados
              and (:vendedorId is null or pr.vendedor.id = :vendedorId)
              and (:categoria is null or pr.categoria = :categoria)
            """)
    BigDecimal sumTotalVentas(@Param("inicio") LocalDateTime inicio,
                              @Param("fin") LocalDateTime fin,
                              @Param("estados") Collection<EstadoPedido> estados,
                              @Param("vendedorId") Long vendedorId,
                              @Param("categoria") CategoriaPrenda categoria);

    @Query("""
            select sum(li.cantidad)
            from LineaPedido li
              join li.pedido p
              join Prenda pr on pr.id = li.prendaId
            where p.fechaCreacion >= :inicio and p.fechaCreacion < :fin
              and p.estado in :estados
              and (:vendedorId is null or pr.vendedor.id = :vendedorId)
              and (:categoria is null or pr.categoria = :categoria)
            """)
    Long sumUnidadesVendidas(@Param("inicio") LocalDateTime inicio,
                             @Param("fin") LocalDateTime fin,
                             @Param("estados") Collection<EstadoPedido> estados,
                             @Param("vendedorId") Long vendedorId,
                             @Param("categoria") CategoriaPrenda categoria);

    @Query("""
            select count(distinct p.id)
            from LineaPedido li
              join li.pedido p
              join Prenda pr on pr.id = li.prendaId
            where p.fechaCreacion >= :inicio and p.fechaCreacion < :fin
              and p.estado in :estados
              and (:vendedorId is null or pr.vendedor.id = :vendedorId)
              and (:categoria is null or pr.categoria = :categoria)
            """)
    long countPedidos(@Param("inicio") LocalDateTime inicio,
                      @Param("fin") LocalDateTime fin,
                      @Param("estados") Collection<EstadoPedido> estados,
                      @Param("vendedorId") Long vendedorId,
                      @Param("categoria") CategoriaPrenda categoria);

    @Query("""
            select new co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardSerieResponse(
                     cast(pr.categoria as string), sum(li.subtotal), sum(li.cantidad))
            from LineaPedido li
              join li.pedido p
              join Prenda pr on pr.id = li.prendaId
            where p.fechaCreacion >= :inicio and p.fechaCreacion < :fin
              and p.estado in :estados
              and (:vendedorId is null or pr.vendedor.id = :vendedorId)
              and (:categoria is null or pr.categoria = :categoria)
            group by pr.categoria
            order by sum(li.subtotal) desc
            """)
    List<DashboardSerieResponse> ventasPorCategoria(@Param("inicio") LocalDateTime inicio,
                                                    @Param("fin") LocalDateTime fin,
                                                    @Param("estados") Collection<EstadoPedido> estados,
                                                    @Param("vendedorId") Long vendedorId,
                                                    @Param("categoria") CategoriaPrenda categoria);

    @Query("""
            select new co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardSerieResponse(
                     li.talla, sum(li.subtotal), sum(li.cantidad))
            from LineaPedido li
              join li.pedido p
              join Prenda pr on pr.id = li.prendaId
            where p.fechaCreacion >= :inicio and p.fechaCreacion < :fin
              and p.estado in :estados
              and (:vendedorId is null or pr.vendedor.id = :vendedorId)
              and (:categoria is null or pr.categoria = :categoria)
            group by li.talla
            order by sum(li.cantidad) desc
            """)
    List<DashboardSerieResponse> demandaPorTalla(@Param("inicio") LocalDateTime inicio,
                                                 @Param("fin") LocalDateTime fin,
                                                 @Param("estados") Collection<EstadoPedido> estados,
                                                 @Param("vendedorId") Long vendedorId,
                                                 @Param("categoria") CategoriaPrenda categoria);

    @Query("""
            select new co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardSerieResponse(
                     pr.nombre, sum(li.subtotal), sum(li.cantidad))
            from LineaPedido li
              join li.pedido p
              join Prenda pr on pr.id = li.prendaId
            where p.fechaCreacion >= :inicio and p.fechaCreacion < :fin
              and p.estado in :estados
              and (:vendedorId is null or pr.vendedor.id = :vendedorId)
              and (:categoria is null or pr.categoria = :categoria)
            group by pr.id, pr.nombre
            order by sum(li.cantidad) desc, pr.nombre asc
            """)
    List<DashboardSerieResponse> productosMasVendidos(@Param("inicio") LocalDateTime inicio,
                                                      @Param("fin") LocalDateTime fin,
                                                      @Param("estados") Collection<EstadoPedido> estados,
                                                      @Param("vendedorId") Long vendedorId,
                                                      @Param("categoria") CategoriaPrenda categoria,
                                                      Pageable limite);

    /** Vistas reales de la ficha de cada prenda (no ventas). */
    @Query("""
            select new co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardSerieResponse(
                     pr.nombre, count(v))
            from ProductoVista v
              join Prenda pr on pr.id = v.prendaId
            where v.fecha >= :inicio and v.fecha < :fin
              and (:vendedorId is null or pr.vendedor.id = :vendedorId)
              and (:categoria is null or pr.categoria = :categoria)
            group by pr.id, pr.nombre
            order by count(v) desc, pr.nombre asc
            """)
    List<DashboardSerieResponse> productosMasConsultados(@Param("inicio") LocalDateTime inicio,
                                                         @Param("fin") LocalDateTime fin,
                                                         @Param("vendedorId") Long vendedorId,
                                                         @Param("categoria") CategoriaPrenda categoria,
                                                         Pageable limite);

    /**
     * Términos más buscados en el catálogo. Es una señal de demanda de toda la plataforma: una búsqueda no
     * pertenece a ningún vendedor, así que no se filtra por vendedor ni por categoría.
     */
    @Query("""
            select new co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardSerieResponse(
                     b.termino, count(b))
            from Busqueda b
            where b.fecha >= :inicio and b.fecha < :fin
            group by b.termino
            order by count(b) desc, b.termino asc
            """)
    List<DashboardSerieResponse> busquedasMasFrecuentes(@Param("inicio") LocalDateTime inicio,
                                                        @Param("fin") LocalDateTime fin,
                                                        Pageable limite);

    /** Unidades en inventario de prendas activas (foto actual, no depende del rango de fechas). */
    @Query("""
            select sum(t.stock)
            from TallaPrenda t
              join t.prenda pr
            where pr.estado = :estadoPrenda
              and (:vendedorId is null or pr.vendedor.id = :vendedorId)
              and (:categoria is null or pr.categoria = :categoria)
            """)
    Long sumStockTotal(@Param("estadoPrenda") EstadoPrenda estadoPrenda,
                       @Param("vendedorId") Long vendedorId,
                       @Param("categoria") CategoriaPrenda categoria);

    /**
     * Tallas activas con stock en o bajo su mínimo de reabastecimiento. Una talla sin mínimo configurado no
     * alerta (ver {@code TallaPrenda.stockMinimo}).
     */
    @Query("""
            select count(t)
            from TallaPrenda t
              join t.prenda pr
            where pr.estado = :estadoPrenda
              and t.stockMinimo is not null
              and t.stock <= t.stockMinimo
              and (:vendedorId is null or pr.vendedor.id = :vendedorId)
              and (:categoria is null or pr.categoria = :categoria)
            """)
    long countStockBajo(@Param("estadoPrenda") EstadoPrenda estadoPrenda,
                        @Param("vendedorId") Long vendedorId,
                        @Param("categoria") CategoriaPrenda categoria);
}
