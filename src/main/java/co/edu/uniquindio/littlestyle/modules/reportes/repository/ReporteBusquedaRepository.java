package co.edu.uniquindio.littlestyle.modules.reportes.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.BusquedaCatalogo;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consultas de solo lectura del reporte de búsquedas (US-16). No expone escritura: el registro de
 * búsquedas vive en {@code BusquedaCatalogoRepository}.
 *
 * <p>Alcance: {@code vendedorId == null} es el consolidado global del administrador. Para un
 * vendedor se limita a búsquedas <strong>sin resultados</strong> en categorías que él vende; ver
 * la decisión documentada en {@code docs/us-16/descripcion-funcional.md}. Vincular cada búsqueda
 * con las prendas concretas que devolvió exigiría persistir los ids de resultado (hasta 5 000 por
 * búsqueda) y haría el registro asíncrono demasiado costoso.</p>
 */
public interface ReporteBusquedaRepository extends Repository<BusquedaCatalogo, Long> {

    /**
     * Cuenta los términos distintos que cumplen el filtro. Se ejecuta antes de traer datos para
     * rechazar con 422 sin cargar más de {@code MAX_FILAS}.
     */
    @Query("""
            select count(distinct b.terminoNormalizado)
            from BusquedaCatalogo b
            where b.creadoEn >= :inicio
              and b.creadoEn < :fin
              and (:categoria is null or b.categoria = :categoria)
              and (:vendedorId is null or (b.resultadosCount = 0 and exists (
                    select 1 from Prenda p
                    where p.vendedor.id = :vendedorId and p.categoria = b.categoria)))
            """)
    long contarTerminos(@Param("inicio") LocalDateTime inicio,
                        @Param("fin") LocalDateTime fin,
                        @Param("categoria") CategoriaPrenda categoria,
                        @Param("vendedorId") Long vendedorId);

    /**
     * Resumen por término ordenado por volumen descendente. Devuelve columnas:
     * término original, búsquedas, búsquedas sin resultados y última fecha.
     */
    @Query("""
            select min(b.terminoOriginal), count(b),
                   sum(case when b.resultadosCount = 0 then 1 else 0 end),
                   max(b.creadoEn)
            from BusquedaCatalogo b
            where b.creadoEn >= :inicio
              and b.creadoEn < :fin
              and (:categoria is null or b.categoria = :categoria)
              and (:vendedorId is null or (b.resultadosCount = 0 and exists (
                    select 1 from Prenda p
                    where p.vendedor.id = :vendedorId and p.categoria = b.categoria)))
            group by b.terminoNormalizado
            order by count(b) desc, max(b.creadoEn) desc
            """)
    List<Object[]> resumenTerminos(@Param("inicio") LocalDateTime inicio,
                                   @Param("fin") LocalDateTime fin,
                                   @Param("categoria") CategoriaPrenda categoria,
                                   @Param("vendedorId") Long vendedorId,
                                   Pageable pageable);
}
