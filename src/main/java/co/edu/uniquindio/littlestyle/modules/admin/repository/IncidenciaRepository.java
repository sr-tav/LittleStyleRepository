package co.edu.uniquindio.littlestyle.modules.admin.repository;

import co.edu.uniquindio.littlestyle.modules.admin.model.EstadoIncidencia;
import co.edu.uniquindio.littlestyle.modules.admin.model.Incidencia;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoIncidencia;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    @Query("""
            select i from Incidencia i
            where (:estado is null or i.estado = :estado)
              and (:tipo is null or i.tipo = :tipo)
              and (:rol is null or i.reportanteRol = :rol)
              and (:texto is null or :texto = ''
                   or lower(i.asunto) like lower(concat('%', :texto, '%'))
                   or lower(i.descripcion) like lower(concat('%', :texto, '%'))
                   or lower(i.reportanteEmail) like lower(concat('%', :texto, '%')))
            """)
    Page<Incidencia> buscar(@Param("estado") EstadoIncidencia estado,
                            @Param("tipo") TipoIncidencia tipo,
                            @Param("rol") Rol rol,
                            @Param("texto") String texto,
                            Pageable pageable);

    Page<Incidencia> findAllByReportanteIdOrderByFechaCreacionDesc(Long reportanteId, Pageable pageable);

    long countByEstado(EstadoIncidencia estado);

    long countByEstadoAndFechaResolucionBetween(EstadoIncidencia estado,
                                                LocalDateTime desde, LocalDateTime hasta);

    List<Incidencia> findByEstadoAndFechaResolucionBetween(EstadoIncidencia estado,
                                                           LocalDateTime desde, LocalDateTime hasta);

    /** Evita duplicar la apelacion automatica cuando ya hay una del mismo tipo en curso. */
    boolean existsByReportanteIdAndTipoAndEstadoIn(Long reportanteId, TipoIncidencia tipo,
                                                   List<EstadoIncidencia> estados);

    /** Carga la incidencia con bloqueo de escritura para cerrarla o tomarla sin carreras. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Incidencia i where i.id = :id")
    Optional<Incidencia> findByIdForUpdate(@Param("id") Long id);
}
