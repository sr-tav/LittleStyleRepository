package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.MaterialTextil;
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

/** Acceso a las prendas del catálogo. */
public interface PrendaRepository extends JpaRepository<Prenda, Long> {

    /** Prendas del vendedor con sus tallas, de la modificada más recientemente a la más antigua. */
    @EntityGraph(attributePaths = "tallas")
    List<Prenda> findAllByVendedorIdOrderByFechaActualizacionDesc(Long vendedorId);

    /** Prenda si pertenece al vendedor. */
    Optional<Prenda> findByIdAndVendedorId(Long id, Long vendedorId);

    /**
     * Bloquea la fila de la prenda hasta el fin de la transacción, para que dos actualizaciones de stock
     * concurrentes no se pisen y la evaluación de alertas vea siempre el valor definitivo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Prenda p where p.id = :id and p.vendedor.id = :vendedorId")
    Optional<Prenda> bloquearPropia(@Param("id") Long id, @Param("vendedorId") Long vendedorId);

    /** Ids de las prendas del vendedor. */
    @Query("select p.id from Prenda p where p.vendedor.id = :vendedorId")
    List<Long> findIdsByVendedorId(@Param("vendedorId") Long vendedorId);

    /**
     * Prendas en un estado o mas, con tallas y vendedor cargados. Reserva la regla de visibilidad
     * publica (US-14): se devuelven prendas {@code ACTIVA}, con moderacion distinta de DESACTIVADA y
     * vendedor ACTIVO. SIN_REVISAR sigue visible porque la moderacion es posterior a la publicacion.
     */
    @EntityGraph(attributePaths = {"tallas", "vendedor"})
    @Query("""
            select p from Prenda p
            where p.estado = :estado
              and (p.estadoModeracion is null or
                   p.estadoModeracion <> co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion.DESACTIVADA)
              and p.vendedor.estado = co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario.ACTIVO
            """)
    List<Prenda> findAllByEstado(@Param("estado") EstadoPrenda estado);

    /**
     * Vitrina del cliente solo prendas publicadas, con filtros
     * opcionales aplicados en la base de datos y paginación. .
     * La paginación se hace en DOS pasos (RNF-02): primero esta consulta de
     * identificadores SIN fetch de colecciones, y luego {@link #buscarPorIds}
     * trae las tallas. Hacerlo en una sola consulta con
     * {@code @EntityGraph("tallas")} + {@code Pageable} hace que Hibernate
     * pagine en memoria (HHH90003004) y rompa el total.
     */
    @Query("""
            select p.id from Prenda p
            where p.estado = :estado
              and (p.estadoModeracion is null or
                   p.estadoModeracion <> co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion.DESACTIVADA)
              and p.vendedor.estado = co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario.ACTIVO
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
              and (:materiales is null or exists (select 1 from Prenda p2 join p2.composicion c where p2 = p and c.material in :materiales))
              and(:talla is null or :talla = '' or exists (select 1 from TallaPrenda  t where t.prenda = p and upper(t.talla) = upper(:talla) and t.stock > 0))
              and(:edadMeses is null or exists (select 1 from TallaPrenda t where t.prenda = p and (t.edadMinMeses is null or t.edadMinMeses <= :edadMeses) and (t.edadMaxMeses is null or t.edadMaxMeses >= :edadMeses)))
            """)
    Page<Long> buscarIdsVitrina(@Param("estado") EstadoPrenda estado,
                                @Param("categoria") CategoriaPrenda categoria,
                                @Param("genero") GeneroPrenda genero,
                                @Param("precioMin") BigDecimal precioMin,
                                @Param("precioMax") BigDecimal precioMax,
                                @Param("texto") String texto,
                                @Param("disponible") Boolean disponible,
                                @Param("materiales") List<MaterialTextil> materiales,
                                @Param("talla") String talla,
                                @Param("edadMeses") Integer edadMeses,
                                Pageable pageable);

    /**
     * Variante para "Solo mi talla sugerida" (US-07): mismos filtros más la
     * intersección con los identificadores recomendados. Devuelve ids (paso 1
     * de la paginación en dos pasos); el paso 2 es {@link #buscarPorIds}.
     */
    @Query("""
            select p.id from Prenda p
            where p.estado = :estado
              and p.id in :ids
              and (p.estadoModeracion is null or
                   p.estadoModeracion <> co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion.DESACTIVADA)
              and p.vendedor.estado = co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario.ACTIVO
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
              and (:materiales is null or exists (select 1 from Prenda p2 join p2.composicion c where p2 = p and c.material in :materiales))
              and(:talla is null or :talla = '' or exists (select 1 from TallaPrenda  t where t.prenda = p and upper(t.talla) = upper(:talla) and t.stock > 0))
              and(:edadMeses is null or exists (select 1 from TallaPrenda t where t.prenda = p and (t.edadMinMeses is null or t.edadMinMeses <= :edadMeses) and (t.edadMaxMeses is null or t.edadMaxMeses >= :edadMeses)))
            """)
    Page<Long> buscarIdsVitrinaPorIds(@Param("estado") EstadoPrenda estado,
                                      @Param("ids") List<Long> ids,
                                      @Param("categoria") CategoriaPrenda categoria,
                                      @Param("genero") GeneroPrenda genero,
                                      @Param("precioMin") BigDecimal precioMin,
                                      @Param("precioMax") BigDecimal precioMax,
                                      @Param("texto") String texto,
                                      @Param("disponible") Boolean disponible,
                                      @Param("materiales") List<MaterialTextil> materiales,
                                      @Param("talla") String talla,
                                      @Param("edadMeses") Integer edadMeses,
                                      Pageable pageable);

    /** Prenda si esta en el estado indicado y es visible (US-14: no desactivada y vendedor ACTIVO). */
    @EntityGraph(attributePaths = {"tallas", "vendedor"})
    @Query("""
            select p from Prenda p
            where p.id = :id
              and p.estado = :estado
              and (p.estadoModeracion is null or
                   p.estadoModeracion <> co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion.DESACTIVADA)
              and p.vendedor.estado = co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario.ACTIVO
            """)
    Optional<Prenda> findByIdAndEstado(@Param("id") Long id, @Param("estado") EstadoPrenda estado);

    /**
     * Paso 2 de la paginación en dos pasos: trae las tallas de la página ya
     * calculada. La composición e imágenes quedan con {@code @BatchSize}
     * (traer varias colecciones List a la vez daría MultipleBagFetchException).
     */
    @EntityGraph(attributePaths = {"tallas"})
    @Query("""
            select p from Prenda p
            where p.id in :ids
              and p.estado = co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda.ACTIVA
              and (p.estadoModeracion is null or
                   p.estadoModeracion <> co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion.DESACTIVADA)
              and p.vendedor.estado = co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario.ACTIVO
            """)
    List<Prenda> buscarPorIds(@Param("ids") List<Long> ids);
    /** Prenda en el estado indicado con bloqueo de escritura, para validar y mover stock sin carreras. */
    /** Prenda en el estado indicado y publicable (US-14), con bloqueo de escritura para validar stock. */
    @EntityGraph(attributePaths = "tallas")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Prenda p
            where p.id = :id
              and p.estado = :estado
              and (p.estadoModeracion is null or
                   p.estadoModeracion <> co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion.DESACTIVADA)
              and p.vendedor.estado = co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario.ACTIVO
            """)
    Optional<Prenda> bloquearPorIdYEstado(@Param("id") Long id, @Param("estado") EstadoPrenda estado);

    /** Prenda con bloqueo de escritura, sin importar su estado (pagos y reposición de stock). */
    /** Prenda con bloqueo de escritura, sin importar su estado (pagos y reposicion de stock). */
    @EntityGraph(attributePaths = {"tallas", "vendedor"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Prenda p where p.id = :id")
    Optional<Prenda> bloquearPorId(@Param("id") Long id);

    /**
     * Bandeja de moderacion del administrador (US-14): SIN_REVISAR primero, con productos reportados
     * priorizados dentro de su estado y despues por fecha reciente. A diferencia de la vitrina, aqui NO se exige visibilidad
     * publica: el admin debe ver tambien las RECHAZADA y las de vendedores suspendidos.
     */
    @Query("""
            select p from Prenda p join fetch p.vendedor v
            where (:vendedorEmail is null or lower(v.email) like lower(concat('%', :vendedorEmail, '%')))
              and (:estadoPrenda is null or p.estado = :estadoPrenda)
              and (:estadoModeracion is null or p.estadoModeracion = :estadoModeracion)
              and (:texto is null or :texto = ''
                   or lower(p.nombre) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(p.marca, '')) like lower(concat('%', :texto, '%')))
            order by case when p.estadoModeracion =
                         co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion.SIN_REVISAR
                         then 0 else 1 end,
                     case when (select count(i) from co.edu.uniquindio.littlestyle.modules.admin.model.Incidencia i
                                where i.referenciaId = p.id
                                  and i.tipo = co.edu.uniquindio.littlestyle.modules.admin.model.TipoIncidencia.REPORTE_PRODUCTO
                                  and i.estado in (co.edu.uniquindio.littlestyle.modules.admin.model.EstadoIncidencia.ABIERTA,
                                                   co.edu.uniquindio.littlestyle.modules.admin.model.EstadoIncidencia.EN_REVISION)) > 0
                         then 0 else 1 end,
                     p.fechaActualizacion desc, p.id desc
            """)
    Page<Prenda> buscarModeracion(@Param("vendedorEmail") String vendedorEmail,
                                  @Param("estadoPrenda") EstadoPrenda estadoPrenda,
                                  @Param("estadoModeracion") EstadoModeracion estadoModeracion,
                                  @Param("texto") String texto,
                                  Pageable pageable);

    /** Prenda con todas sus colecciones e info de vendedor, para el detalle de moderacion. */
    @EntityGraph(attributePaths = {"vendedor"})
    @Query("select p from Prenda p where p.id = :id")
    Optional<Prenda> buscarDetalleAdmin(@Param("id") Long id);

    /** Carga la prenda con el vendedor para notificaciones post-commit asincronas. */
    @Query("select p from Prenda p join fetch p.vendedor where p.id = :id")
    Optional<Prenda> buscarParaNotificacion(@Param("id") Long id);

    /** KPIs del resumen: prendas por estado de moderacion y publicadas (ACTIVA + APROBADA). */
    long countByEstadoModeracion(EstadoModeracion estadoModeracion);

    long countByEstadoAndEstadoModeracion(EstadoPrenda estado, EstadoModeracion estadoModeracion);
}
