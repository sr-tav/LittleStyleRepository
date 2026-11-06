package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Acceso a los pedidos. */
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Pedido con bloqueo de escritura, para cambiar su estado o confirmar su pago sin carreras. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> bloquearPorId(@Param("id") Long id);

    /**
     * Bloquea la fila del cliente para que dos compras simultáneas suyas no reciban el mismo consecutivo;
     * debe llamarse dentro de la transacción que crea el pedido.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :clienteId")
    Optional<Usuario> bloquearCliente(@Param("clienteId") Long clienteId);

    /** Mayor consecutivo de pedido del cliente; 0 si todavía no tiene pedidos numerados. */
    @Query("select coalesce(max(p.numeroCliente), 0) from Pedido p where p.cliente.id = :clienteId")
    int ultimoNumeroDelCliente(@Param("clienteId") Long clienteId);

    /** Pedido si pertenece al cliente. */
    Optional<Pedido> findByIdAndClienteId(Long id, Long clienteId);

    /** Pedidos del cliente, del más reciente al más antiguo. */
    List<Pedido> findAllByClienteIdOrderByFechaCreacionDesc(Long clienteId);

    /**
     * Pedidos que contienen al menos una línea de las prendas del vendedor,
     * para que prepare sus despachos. No filtra por estado: el vendedor ve
     * el ciclo completo de lo que debe entregar.
     */
    @Query("""
            select distinct p from Pedido p
            join p.items l
            join Prenda pr on pr.id = l.prendaId
            where pr.vendedor.id = :vendedorId
            order by p.fechaCreacion desc, p.id desc
            """)
    List<Pedido> findAllConLineasDelVendedor(@Param("vendedorId") Long vendedorId);

}
