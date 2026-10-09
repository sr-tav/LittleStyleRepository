package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @EntityGraph(attributePaths = {"tallas", "composicion", "colores", "imagenes", "venedor"})
    @Query("select p from Prenda p where p.estado = :estado")
    List<Prenda> findAllByEstadoConGrafico(@Param("estado") EstadoPrenda estado);

    @EntityGraph(attributePaths = {"tallas", "vendedor"})
    Optional<Prenda> findByIdAndEstado(Long id, EstadoPrenda estado);
}
