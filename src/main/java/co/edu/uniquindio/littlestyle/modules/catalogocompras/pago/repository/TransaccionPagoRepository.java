package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.EstadoTransaccionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.TransaccionPago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;

/** Acceso a las transacciones de pago. */
public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {

    /**
     * Transacción con bloqueo de escritura, para que el webhook y la verificación directa no la apliquen a la
     * vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TransaccionPago t where t.referencia = :referencia")
    Optional<TransaccionPago> bloquearPorReferencia(@Param("referencia") String referencia);

    /** Transacción por su referencia, sin bloqueo. */
    Optional<TransaccionPago> findByReferencia(String referencia);

    /** Transacción creada antes por el cliente con la misma clave de idempotencia. */
    Optional<TransaccionPago> findByClienteIdAndClaveIdempotencia(Long clienteId, String claveIdempotencia);

    /** Transacción más reciente del cliente en alguno de los estados dados. */
    Optional<TransaccionPago> findFirstByClienteIdAndEstadoInOrderByFechaCreacionDesc(
            Long clienteId, Collection<EstadoTransaccionPago> estados);
}
