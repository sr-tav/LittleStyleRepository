package co.edu.uniquindio.littlestyle.modules.dashboard.repository;

import co.edu.uniquindio.littlestyle.modules.dashboard.model.ProductoVista;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistencia de las vistas de prendas; las lecturas analíticas viven en {@link DashboardAnalyticsRepository}. */
public interface ProductoVistaRepository extends JpaRepository<ProductoVista, Long> {
}
