package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.ClaveIdempotenciaPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ClaveIdempotenciaRepository extends JpaRepository<ClaveIdempotenciaPedido, String> {

    Optional<ClaveIdempotenciaPedido> findByClaveAndClienteId(String clave, Long clienteId);

    @Modifying
    @Query("delete from ClaveIdempotenciaPedido c where c.expira <= :ahora")
    int eliminarVencidas(@Param("ahora") LocalDateTime ahora);
}
