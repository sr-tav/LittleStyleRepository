package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.EstadoTransaccionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.TransaccionPago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TransaccionPago t where t.referencia = :referencia")
    Optional<TransaccionPago> bloquearPorReferencia(@Param("referencia") String referencia);

    Optional<TransaccionPago> findByReferencia(String referencia);

    long countByPedidoId(Long pedidoId);

    boolean existsByPedidoIdAndEstado(Long pedidoId, EstadoTransaccionPago estado);
}
