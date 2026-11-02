package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.BusquedaCatalogo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Escritura del registro de búsquedas del catálogo (US-16). */
public interface BusquedaCatalogoRepository extends JpaRepository<BusquedaCatalogo, Long> {

    @Modifying
    @Transactional
    @Query("delete from BusquedaCatalogo b where b.creadoEn < :limite")
    int eliminarAnterioresA(@Param("limite") LocalDateTime limite);
}
