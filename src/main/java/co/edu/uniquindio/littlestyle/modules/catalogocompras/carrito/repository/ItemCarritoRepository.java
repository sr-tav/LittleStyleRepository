package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.model.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

/** Aceso a las lineas de los carritos */
public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {

    /** Quita de todos los carritos las lineas de las prendas indicadas */
    @Modifying(flushAutomatically = true)
    @Query("delete from ItemCarrito  i where i.prendaId in :ids")
    int borrarPorPrendaIds(@Param("ids")Collection<Long> ids);
}
