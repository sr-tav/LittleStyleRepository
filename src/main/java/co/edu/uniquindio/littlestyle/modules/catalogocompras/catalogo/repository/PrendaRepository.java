package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.MaterialTextil;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PrendaRepository extends JpaRepository<Prenda, Long> {

    @EntityGraph(attributePaths = "tallas")
    List<Prenda> findAllByVendedorIdOrderByFechaActualizacionDesc(Long vendedorId);

    Optional<Prenda> findByIdAndVendedorId(Long id, Long vendedorId);

    /**
     * Bloquea la fila de la prenda hasta el fin de la transacción, para que dos actualizaciones de stock
     * concurrentes no se pisen y la evaluación de alertas vea siempre el valor definitivo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Prenda p where p.id = :id and p.vendedor.id = :vendedorId")
    Optional<Prenda> bloquearPropia(@Param("id") Long id, @Param("vendedorId") Long vendedorId);

    @EntityGraph(attributePaths = {"tallas", "vendedor"})
    List<Prenda> findAllByEstado(EstadoPrenda estado);

    /**
     * Vitrina del cliente solo prendas publicadas, con filtros
     * opcionales aplicados en la base de datos y paginación. .
     */
    @Query("""
            select p from Prenda p
            where p.estado = :estado
              and (:categoria is null or p.categoria = :categoria)
              and (:precioMin is null or p.precio >= :precioMin)
              and (:precioMax is null or p.precio <= :precioMax)
              and (:texto is null or :texto = ''
                   or lower(p.nombre) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(p.marca, '')) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(p.descripcion, '')) like lower(concat('%', :texto, '%')))
              and (:disponible is null or :disponible = false
                   or exists (select 1 from TallaPrenda t where t.prenda = p and t.stock > 0))
              and (:genero is null or p.genero is null or p.genero = :genero
                   or p.genero = co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda.UNISEX)
              and (:material is null or exists (select 1 from Prenda p2 join p2.composicion c where p2 = p and c.material =  :material))
              and(:talla is null or :talla = '' or exists (select 1 from TallaPrenda  t where t.prenda = p and upper(t.talla) = upper(:talla) and t.stock > 0))
            """)
    Page<Prenda> buscarVitrina(@Param("estado") EstadoPrenda estado,
                               @Param("categoria") CategoriaPrenda categoria,
                               @Param("genero") GeneroPrenda genero,
                               @Param("precioMin") BigDecimal precioMin,
                               @Param("precioMax") BigDecimal precioMax,
                               @Param("texto") String texto,
                               @Param("disponible") Boolean disponible,
                               @Param("material") MaterialTextil material,
                               @Param("talla") String talla,
                               Pageable pageable);

    @EntityGraph(attributePaths = {"tallas", "vendedor"})
    Optional<Prenda> findByIdAndEstado(Long id, EstadoPrenda estado);

    @EntityGraph(attributePaths = "tallas")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Prenda p where p.id = :id and p.estado = :estado")
    Optional<Prenda> bloquearPorIdYEstado(@Param("id") Long id, @Param("estado") EstadoPrenda estado);

    @EntityGraph(attributePaths = {"tallas", "vendedor"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Prenda p where p.id = :id")
    Optional<Prenda> bloquearPorId(@Param("id") Long id);
}
