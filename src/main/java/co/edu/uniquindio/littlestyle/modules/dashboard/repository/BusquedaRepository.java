package co.edu.uniquindio.littlestyle.modules.dashboard.repository;

import co.edu.uniquindio.littlestyle.modules.dashboard.model.Busqueda;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de las búsquedas del catálogo; las lecturas analíticas viven en {@link DashboardAnalyticsRepository}. */
public interface BusquedaRepository extends JpaRepository<Busqueda, Long> {
}
