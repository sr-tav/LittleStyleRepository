package co.edu.uniquindio.littlestyle.modules.dashboard.repository;

import co.edu.uniquindio.littlestyle.modules.dashboard.model.ProductoVista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Persistencia de las vistas de prendas; las lecturas analíticas viven en {@link DashboardAnalyticsRepository}. */
public interface ProductoVistaRepository extends JpaRepository<ProductoVista, Long> {

    /** Borra las vistas estrictamente anteriores al límite y devuelve cuántas eliminó (retención). */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ProductoVista v where v.fecha < :limite")
    int eliminarAnterioresA(@Param("limite") LocalDateTime limite);
}
