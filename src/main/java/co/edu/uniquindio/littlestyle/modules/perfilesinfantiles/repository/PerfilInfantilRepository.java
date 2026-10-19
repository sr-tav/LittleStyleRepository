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

/** Acceso a los perfiles infantiles. */
public interface PerfilInfantilRepository extends JpaRepository<PerfilInfantil, Long> {

    /** Perfiles del cliente con sus mediciones, del modificado más recientemente al más antiguo. */
    @EntityGraph(attributePaths = "mediciones")
    List<PerfilInfantil> findAllByClienteIdOrderByFechaActualizacionDesc(Long clienteId);

    /** Perfil si pertenece al cliente. */
    Optional<PerfilInfantil> findByIdAndClienteId(Long id, Long clienteId);

    /** Perfil del cliente con bloqueo de escritura, para registrar mediciones sin carreras. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PerfilInfantil p where p.id = :perfilId and p.cliente.id = :clienteId")
    Optional<PerfilInfantil> findByIdAndClienteIdForUpdate(
            @Param("perfilId") Long perfilId, @Param("clienteId") Long clienteId);

    /** Número de perfiles del cliente. */
    long countByClienteId(Long clienteId);
}
