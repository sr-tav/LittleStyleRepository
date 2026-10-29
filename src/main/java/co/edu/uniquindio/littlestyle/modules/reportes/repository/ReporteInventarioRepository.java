package co.edu.uniquindio.littlestyle.modules.reportes.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.InventarioFila;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReporteInventarioRepository extends JpaRepository<TallaPrenda, Long> {

    /** Foto actual del inventario de prendas activas. {@code vendedorId} nulo = global (solo administrador). */
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
                                         @Param("categoria") CategoriaPrenda categoria);
}