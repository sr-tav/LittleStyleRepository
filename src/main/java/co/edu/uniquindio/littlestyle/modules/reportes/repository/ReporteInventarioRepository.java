package co.edu.uniquindio.littlestyle.modules.reportes.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.InventarioFila;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Consultas de solo lectura del inventario para reportes. Extiende {@link Repository} (no
 * {@code JpaRepository}) a propósito: este módulo no necesita guardar, borrar ni exponer
 * operaciones de escritura sobre {@link TallaPrenda}.
 */
public interface ReporteInventarioRepository extends Repository<TallaPrenda, Long> {

    /**
     * Cuenta las filas que tendría el reporte. Se ejecuta antes de cargar datos para rechazar
     * con 422 los inventarios que superan el límite sin traerlos a memoria.
     */
    @Query("""
            select count(t)
            from TallaPrenda t
              join t.prenda pr
              join pr.vendedor v
            where pr.estado = :estado
              and (:vendedorId is null or v.id = :vendedorId)
              and (:categoria is null or pr.categoria = :categoria)
            """)
    long countFilasInventario(@Param("estado") EstadoPrenda estado,
                              @Param("vendedorId") Long vendedorId,
                              @Param("categoria") CategoriaPrenda categoria);

    /**
     * Foto actual del inventario de prendas activas. {@code vendedorId} nulo = global (solo
     * administrador). El {@link Pageable} limita la consulta a {@code MAX_FILAS + 1} como defensa
     * ante cambios concurrentes entre el conteo y la lectura.
     */
    @Query("""
            select new co.edu.uniquindio.littlestyle.modules.reportes.dto.InventarioFila(
                     pr.nombre, coalesce(v.nombreTienda, v.nombre), cast(pr.categoria as string),
                     t.talla, t.stock, t.stockMinimo)
            from TallaPrenda t
              join t.prenda pr
              join pr.vendedor v
            where pr.estado = :estado
              and (:vendedorId is null or v.id = :vendedorId)
              and (:categoria is null or pr.categoria = :categoria)
            order by v.id, pr.nombre, t.talla
            """)
    List<InventarioFila> filasInventario(@Param("estado") EstadoPrenda estado,
                                         @Param("vendedorId") Long vendedorId,
                                         @Param("categoria") CategoriaPrenda categoria,
                                         Pageable pageable);
}
