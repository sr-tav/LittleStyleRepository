package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.AlertaReabastecimiento;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

/** Acceso a las alertas de reabastecimiento. */
public interface AlertaReabastecimientoRepository extends JpaRepository<AlertaReabastecimiento, Long> {

    /** Alguna alerta activa de la prenda, sin importar la talla. */
    Optional<AlertaReabastecimiento> findFirstByPrendaIdAndFechaResolucionIsNull(Long prendaId);

    /** Alerta activa de una talla de la prenda. */
    Optional<AlertaReabastecimiento> findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(Long prendaId, String talla);

    Optional<AlertaReabastecimiento> findFirstByPrendaIdAndTallaAndColorAndFechaResolucionIsNull(Long prendaId, String talla, ColorPreferido color);

    /** Todas las alertas activas de la prenda. */
    List<AlertaReabastecimiento> findAllByPrendaIdAndFechaResolucionIsNull(Long prendaId);

    /** Alertas activas heredadas de antes del mínimo por talla. */
    List<AlertaReabastecimiento> findAllByPrendaIdAndTallaIsNullAndFechaResolucionIsNull(Long prendaId);

    /**
     * Alertas activas de las prendas de un vendedor, con la prenda cargada, de la más reciente a la más
     * antigua.
     */
    @EntityGraph(attributePaths = "prenda")
    List<AlertaReabastecimiento> findAllByPrendaVendedorIdAndFechaResolucionIsNullOrderByFechaEmisionDesc(
            Long vendedorId);

    /** Historial completo de alertas de una prenda en orden de emisión. */
    List<AlertaReabastecimiento> findAllByPrendaIdOrderByFechaEmisionAsc(Long prendaId);

    /** Borra todas las alertas (activas e historicas) de las prendas indicadas. */
    @Modifying(flushAutomatically = true)
    @Query("delete from AlertaReabastecimiento  a where a.prenda.id in :ids")
    int borrarPorPrendaIds(@Param("ids") Collection<Long> ids);
}
