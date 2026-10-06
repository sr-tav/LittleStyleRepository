package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.AlertaReabastecimiento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertaReabastecimientoRepository extends JpaRepository<AlertaReabastecimiento, Long> {

    Optional<AlertaReabastecimiento> findFirstByPrendaIdAndFechaResolucionIsNull(Long prendaId);

    @EntityGraph(attributePaths = "prenda")
    List<AlertaReabastecimiento> findAllByPrendaVendedorIdAndFechaResolucionIsNullOrderByFechaEmisionDesc(
            Long vendedorId);

    List<AlertaReabastecimiento> findAllByPrendaIdOrderByFechaEmisionAsc(Long prendaId);
}
