package co.edu.uniquindio.littlestyle.modules.dashboard.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Consultas analíticas del dashboard de operación.
 *
 * <p>Todas las consultas aceptan dos filtros opcionales: {@code categoria} (nombre del enum
 * {@code CategoriaPrenda}) y {@code vendedorId}. Un {@code null} significa "sin filtro", de modo que
 * el administrador obtiene el consolidado global y el vendedor solo su tienda. El filtro por
 * vendedor se aplica siempre sobre la prenda vendida, no sobre el pedido, porque un pedido puede
 * agrupar prendas de varias tiendas.</p>
 */
public interface DashboardAnalyticsRepository extends JpaRepository<Pedido, Long> {

    @Query(value = """
            select coalesce(sum(li.subtotal), 0)
            from pedidos p
            join pedido_items li on li.pedido_id = p.id
            join prendas pr on pr.id = li.prenda_id
            where p.fecha_creacion >= :inicio
              and p.fecha_creacion < :fin
              and p.estado in ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
              and (:categoria is null or pr.categoria = :categoria)
              and (:vendedorId is null or pr.vendedor_id = :vendedorId)
            """, nativeQuery = true)
    BigDecimal sumTotalVentas(@Param("inicio") LocalDateTime inicio,
                              @Param("fin") LocalDateTime fin,
                              @Param("categoria") String categoria,
                              @Param("vendedorId") Long vendedorId);

    @Query(value = """
            select count(distinct p.id)
            from pedidos p
            join pedido_items li on li.pedido_id = p.id
            join prendas pr on pr.id = li.prenda_id
            where p.fecha_creacion >= :inicio
              and p.fecha_creacion < :fin
              and p.estado in ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
              and (:categoria is null or pr.categoria = :categoria)
              and (:vendedorId is null or pr.vendedor_id = :vendedorId)
            """, nativeQuery = true)
    long countPedidos(@Param("inicio") LocalDateTime inicio,
                      @Param("fin") LocalDateTime fin,
                      @Param("categoria") String categoria,
                      @Param("vendedorId") Long vendedorId);

    @Query(value = """
            select coalesce(sum(li.cantidad), 0)
            from pedidos p
            join pedido_items li on li.pedido_id = p.id
            join prendas pr on pr.id = li.prenda_id
            where p.fecha_creacion >= :inicio
              and p.fecha_creacion < :fin
              and p.estado in ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
              and (:categoria is null or pr.categoria = :categoria)
              and (:vendedorId is null or pr.vendedor_id = :vendedorId)
            """, nativeQuery = true)
    BigDecimal sumUnidadesVendidas(@Param("inicio") LocalDateTime inicio,
                                   @Param("fin") LocalDateTime fin,
                                   @Param("categoria") String categoria,
                                   @Param("vendedorId") Long vendedorId);

    @Query(value = """
            select pr.categoria,
                   coalesce(sum(li.subtotal), 0),
                   coalesce(sum(li.cantidad), 0)
            from pedidos p
            join pedido_items li on li.pedido_id = p.id
            join prendas pr on pr.id = li.prenda_id
            where p.fecha_creacion >= :inicio
              and p.fecha_creacion < :fin
              and p.estado in ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
              and (:categoria is null or pr.categoria = :categoria)
              and (:vendedorId is null or pr.vendedor_id = :vendedorId)
            group by pr.categoria
            order by sum(li.subtotal) desc
            """, nativeQuery = true)
    List<Object[]> ventasPorCategoria(@Param("inicio") LocalDateTime inicio,
                                      @Param("fin") LocalDateTime fin,
                                      @Param("categoria") String categoria,
                                      @Param("vendedorId") Long vendedorId);

    @Query(value = """
            select li.talla,
                   coalesce(sum(li.cantidad), 0),
                   coalesce(sum(li.subtotal), 0)
            from pedidos p
            join pedido_items li on li.pedido_id = p.id
            join prendas pr on pr.id = li.prenda_id
            where p.fecha_creacion >= :inicio
              and p.fecha_creacion < :fin
              and p.estado in ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
              and (:categoria is null or pr.categoria = :categoria)
              and (:vendedorId is null or pr.vendedor_id = :vendedorId)
            group by li.talla
            order by sum(li.cantidad) desc
            """, nativeQuery = true)
    List<Object[]> demandaPorTalla(@Param("inicio") LocalDateTime inicio,
                                   @Param("fin") LocalDateTime fin,
                                   @Param("categoria") String categoria,
                                   @Param("vendedorId") Long vendedorId);

    /** Detalle prenda + talla: permite responder qué modelos y tallas se piden más. */
    @Query(value = """
            select pr.nombre,
                   li.talla,
                   coalesce(sum(li.cantidad), 0),
                   coalesce(sum(li.subtotal), 0)
            from pedidos p
            join pedido_items li on li.pedido_id = p.id
            join prendas pr on pr.id = li.prenda_id
            where p.fecha_creacion >= :inicio
              and p.fecha_creacion < :fin
              and p.estado in ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
              and (:categoria is null or pr.categoria = :categoria)
              and (:vendedorId is null or pr.vendedor_id = :vendedorId)
            group by pr.id, pr.nombre, li.talla
            order by sum(li.cantidad) desc, pr.nombre asc
            """, nativeQuery = true)
    List<Object[]> demandaPorPrendaTalla(@Param("inicio") LocalDateTime inicio,
                                         @Param("fin") LocalDateTime fin,
                                         @Param("categoria") String categoria,
                                         @Param("vendedorId") Long vendedorId);

    @Query(value = """
            select pr.nombre,
                   count(*) as veces_pedida,
                   coalesce(sum(li.cantidad), 0) as unidades_vendidas
            from pedidos p
            join pedido_items li on li.pedido_id = p.id
            join prendas pr on pr.id = li.prenda_id
            where p.fecha_creacion >= :inicio
              and p.fecha_creacion < :fin
              and p.estado in ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
              and (:categoria is null or pr.categoria = :categoria)
              and (:vendedorId is null or pr.vendedor_id = :vendedorId)
            group by pr.id, pr.nombre
            order by count(*) desc, sum(li.cantidad) desc
            limit 10
            """, nativeQuery = true)
    List<Object[]> productosMasConsultados(@Param("inicio") LocalDateTime inicio,
                                           @Param("fin") LocalDateTime fin,
                                           @Param("categoria") String categoria,
                                           @Param("vendedorId") Long vendedorId);

    @Query(value = """
            select pr.nombre,
                   coalesce(sum(li.cantidad), 0),
                   coalesce(sum(li.subtotal), 0)
            from pedidos p
            join pedido_items li on li.pedido_id = p.id
            join prendas pr on pr.id = li.prenda_id
            where p.fecha_creacion >= :inicio
              and p.fecha_creacion < :fin
              and p.estado in ('PAGADO', 'REGISTRADO', 'EN_PREPARACION', 'PARCIALMENTE_ENVIADO', 'ENVIADO')
              and (:categoria is null or pr.categoria = :categoria)
              and (:vendedorId is null or pr.vendedor_id = :vendedorId)
            group by pr.id, pr.nombre
            order by sum(li.cantidad) desc
            """, nativeQuery = true)
    List<Object[]> productosMasVendidos(@Param("inicio") LocalDateTime inicio,
                                        @Param("fin") LocalDateTime fin,
                                        @Param("categoria") String categoria,
                                        @Param("vendedorId") Long vendedorId);

    @Query(value = """
            select coalesce(sum(t.stock), 0)
            from prenda_tallas t
            join prendas pr on pr.id = t.prenda_id
            where pr.estado = 'ACTIVA'
              and (:categoria is null or pr.categoria = :categoria)
              and (:vendedorId is null or pr.vendedor_id = :vendedorId)
            """, nativeQuery = true)
    BigDecimal stockTotal(@Param("categoria") String categoria,
                          @Param("vendedorId") Long vendedorId);

    @Query(value = """
            select count(*)
            from prenda_tallas t
            join prendas pr on pr.id = t.prenda_id
            where pr.estado = 'ACTIVA'
              and t.stock_minimo is not null
              and t.stock <= t.stock_minimo
              and (:categoria is null or pr.categoria = :categoria)
              and (:vendedorId is null or pr.vendedor_id = :vendedorId)
            """, nativeQuery = true)
    long tallasBajoStock(@Param("categoria") String categoria,
                         @Param("vendedorId") Long vendedorId);
}
