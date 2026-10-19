package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.ClaveIdempotenciaPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/** Acceso a las claves de idempotencia de creación de pedidos. */
public interface ClaveIdempotenciaRepository extends JpaRepository<ClaveIdempotenciaPedido, String> {

    /** Clave registrada por ese cliente. */
    Optional<ClaveIdempotenciaPedido> findByClaveAndClienteId(String clave, Long clienteId);

    /** Borra las claves vencidas y devuelve cuántas eliminó. */
    @Modifying
    @Query("delete from ClaveIdempotenciaPedido c where c.expira <= :ahora")
    int eliminarVencidas(@Param("ahora") LocalDateTime ahora);
}
