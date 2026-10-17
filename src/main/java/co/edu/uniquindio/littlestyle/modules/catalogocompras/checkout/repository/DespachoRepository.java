package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Despacho;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DespachoRepository extends JpaRepository<Despacho, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Despacho d where d.id = :id")
    Optional<Despacho> bloquearPorId(@Param("id") Long id);

    List<Despacho> findAllByPedidoIdOrderByIdAsc(Long pedidoId);

    List<Despacho> findAllByVendedorIdOrderByFechaActualizacionDesc(Long vendedorId);

    Optional<Despacho> findByIdAndVendedorId(Long id, Long vendedorId);

    Optional<Despacho> findByPedidoIdAndVendedorId(Long pedidoId, Long vendedorId);
}
