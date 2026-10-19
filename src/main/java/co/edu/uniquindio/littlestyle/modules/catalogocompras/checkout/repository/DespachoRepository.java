package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Despacho;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Acceso a los despachos de los pedidos. */
public interface DespachoRepository extends JpaRepository<Despacho, Long> {

    /** Despacho con bloqueo de escritura, para avanzar su estado sin carreras. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Despacho d where d.id = :id")
    Optional<Despacho> bloquearPorId(@Param("id") Long id);

    /** Despachos de un pedido en orden de creación. */
    List<Despacho> findAllByPedidoIdOrderByIdAsc(Long pedidoId);

    /** Despachos de un vendedor, del actualizado más recientemente al más antiguo. */
    List<Despacho> findAllByVendedorIdOrderByFechaActualizacionDesc(Long vendedorId);

    /** Despacho si pertenece al vendedor. */
    Optional<Despacho> findByIdAndVendedorId(Long id, Long vendedorId);

    /** Despacho del vendedor dentro de un pedido. */
    Optional<Despacho> findByPedidoIdAndVendedorId(Long pedidoId, Long vendedorId);
}
