package co.edu.uniquindio.littlestyle.modules.dashboard.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Consultas analíticas del dashboard de operación. */
public interface DashboardAnalyticsRepository extends JpaRepository<Pedido, Long> {

    @Query(value = """
            select coalesce(sum(li.subtotal), 0)
            from pedidos p
            join pedido_items li on li.pedido_id = p.id
            where p.fecha_creacion >= :inicio
              and p.fecha_creacion < :fin
              and p.estado in ('PAGADO', 'ENVIADO')
            """, nativeQuery = true)
    BigDecimal sumTotalVentas(@Param("inicio") LocalDateTime inicio,
                              @Param("fin") LocalDateTime fin);

    @Query(value = """
            select count(distinct p.id)
            from pedidos p
            join pedido_items li on li.pedido_id = p.id
            where p.fecha_creacion >= :inicio
              and p.fecha_creacion < :fin
              and p.estado in ('PAGADO', 'ENVIADO')
            """, nativeQuery = true)
    long countPedidos(@Param("inicio") LocalDateTime inicio,
                      @Param("fin") LocalDateTime fin);

    @Query(value = """
            select pr.categoria,
                   coalesce(sum(li.subtotal), 0),
                   coalesce(sum(li.cantidad), 0)
            from pedidos p
            join pedido_items li on li.pedido_id = p.id
            join prendas pr on pr.id = li.prenda_id
            where p.fecha_creacion >= :inicio
              and p.fecha_creacion < :fin
              and p.estado in ('PAGADO', 'ENVIADO')
            group by pr.categoria
            order by sum(li.subtotal) desc
            """, nativeQuery = true)
    List<Object[]> ventasPorCategoria(@Param("inicio") LocalDateTime inicio,
                                      @Param("fin") LocalDateTime fin);

    @Query(value = """
            select li.talla,
                   coalesce(sum(li.cantidad), 0),
                   coalesce(sum(li.subtotal), 0)
            from pedidos p
            join pedido_items li on li.pedido_id = p.id
            where p.fecha_creacion >= :inicio
              and p.fecha_creacion < :fin
              and p.estado in ('PAGADO', 'ENVIADO')
            group by li.talla
            order by sum(li.cantidad) desc
            """, nativeQuery = true)
    List<Object[]> demandaPorTalla(@Param("inicio") LocalDateTime inicio,
                                   @Param("fin") LocalDateTime fin);

    @Query(value = """
            select pr.nombre,
                   count(*) as veces_vista,
                   coalesce(sum(li.cantidad), 0) as unidades_vendidas
            from pedidos p
            join pedido_items li on li.pedido_id = p.id
            join prendas pr on pr.id = li.prenda_id
            where p.fecha_creacion >= :inicio
              and p.fecha_creacion < :fin
              and p.estado in ('PAGADO', 'ENVIADO')
            group by pr.id, pr.nombre
            order by sum(li.cantidad) desc
            limit 10
            """, nativeQuery = true)
    List<Object[]> productosMasConsultados(@Param("inicio") LocalDateTime inicio,
                                           @Param("fin") LocalDateTime fin);

    @Query(value = """
            select pr.nombre,
                   coalesce(sum(li.cantidad), 0),
                   coalesce(sum(li.subtotal), 0)
            from pedidos p
            join pedido_items li on li.pedido_id = p.id
            join prendas pr on pr.id = li.prenda_id
            where p.fecha_creacion >= :inicio
              and p.fecha_creacion < :fin
              and p.estado in ('PAGADO', 'ENVIADO')
            group by pr.id, pr.nombre
            order by sum(li.cantidad) desc
            limit 10
            """, nativeQuery = true)
    List<Object[]> productosMasVendidos(@Param("inicio") LocalDateTime inicio,
                                       @Param("fin") LocalDateTime fin);
}
