package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.BusquedaCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;

/** Escritura del registro de búsquedas del catálogo (US-16). */
public interface BusquedaCatalogoRepository extends JpaRepository<BusquedaCatalogo, Long> {
}
