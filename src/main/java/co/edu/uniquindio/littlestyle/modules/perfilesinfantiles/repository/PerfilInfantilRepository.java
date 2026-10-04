package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.PerfilInfantil;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PerfilInfantilRepository extends JpaRepository<PerfilInfantil, Long> {

    @EntityGraph(attributePaths = "mediciones")
    List<PerfilInfantil> findAllByClienteIdOrderByFechaActualizacionDesc(Long clienteId);

    Optional<PerfilInfantil> findByIdAndClienteId(Long id, Long clienteId);

    long countByClienteId(Long clienteId);
}
