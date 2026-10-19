package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.EstadoTransaccionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.TransaccionPago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TransaccionPago t where t.id = :id")
    Optional<TransaccionPago> bloquearPorId(@Param("id") Long id);

    Optional<TransaccionPago> findByReferencia(String referencia);

    /** Transacción creada antes por el cliente con la misma clave de idempotencia. */
    Optional<TransaccionPago> findByClienteIdAndClaveIdempotencia(Long clienteId, String claveIdempotencia);

    /** Transacción más reciente del cliente en alguno de los estados dados. */
    Optional<TransaccionPago> findFirstByClienteIdAndEstadoInOrderByFechaCreacionDesc(
            Long clienteId, Collection<EstadoTransaccionPago> estados);

    /** Identificadores de las compras a medio hacer que superan la antigüedad máxima configurada. */
    @Query("select t.id from TransaccionPago t where t.estado in :estados and t.fechaCreacion <= :limite")
    List<Long> buscarIdsVencidos(@Param("estados") Collection<EstadoTransaccionPago> estados,
                                @Param("limite") LocalDateTime limite);
}
