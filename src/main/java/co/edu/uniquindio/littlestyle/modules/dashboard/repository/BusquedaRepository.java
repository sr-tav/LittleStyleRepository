package co.edu.uniquindio.littlestyle.modules.dashboard.repository;

import co.edu.uniquindio.littlestyle.modules.dashboard.model.Busqueda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Persistencia de las búsquedas del catálogo; las lecturas analíticas viven en {@link DashboardAnalyticsRepository}. */
public interface BusquedaRepository extends JpaRepository<Busqueda, Long> {

    /** Borra las búsquedas estrictamente anteriores al límite y devuelve cuántas eliminó (retención). */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Busqueda b where b.fecha < :limite")
    int eliminarAnterioresA(@Param("limite") LocalDateTime limite);
}
