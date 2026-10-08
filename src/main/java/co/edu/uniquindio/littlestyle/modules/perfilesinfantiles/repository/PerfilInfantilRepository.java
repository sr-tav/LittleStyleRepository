package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.PerfilInfantil;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PerfilInfantilRepository extends JpaRepository<PerfilInfantil, Long> {

    @EntityGraph(attributePaths = "mediciones")
    List<PerfilInfantil> findAllByClienteIdOrderByFechaActualizacionDesc(Long clienteId);

    Optional<PerfilInfantil> findByIdAndClienteId(Long id, Long clienteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PerfilInfantil p where p.id = :perfilId and p.cliente.id = :clienteId")
    Optional<PerfilInfantil> findByIdAndClienteIdForUpdate(
            @Param("perfilId") Long perfilId, @Param("clienteId") Long clienteId);

    long countByClienteId(Long clienteId);
}
