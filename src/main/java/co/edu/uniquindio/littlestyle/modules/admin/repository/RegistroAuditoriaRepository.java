package co.edu.uniquindio.littlestyle.modules.admin.repository;

import co.edu.uniquindio.littlestyle.modules.admin.model.AccionAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.RegistroAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.ResultadoAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoEntidad;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de solo anexado para {@link RegistroAuditoria} (US-14, RNF-08): extiende {@link Repository}
 * y NO expone {@code delete} ni métodos de actualización, de modo que ningún servicio pueda alterar un
 * registro persistido. La actualización o el borrado directo por BD se niega con triggers en MySQL; en H2
 * la protección es la de este contrato más la verificación de integridad del hash.
 */
public interface RegistroAuditoriaRepository extends Repository<RegistroAuditoria, Long> {

    /** Persiste un nuevo registro; existe para que solo se anexe, nunca se actualice ni borre. */
    <S extends RegistroAuditoria> S save(S registro);

    /** Toma el candado global antes de leer el ultimo hash, incluso cuando todavía no hay registros. */
    @Query(value = "SELECT id FROM auditoria_admin_candado WHERE id = 1 FOR UPDATE", nativeQuery = true)
    Integer bloquearCadena();

    /** Consulta con filtros opcionales para el visor del administrador (paginado en servidor). */
    @Query("""
            select r from RegistroAuditoria r
            where (:adminId is null or r.adminId = :adminId)
              and (:adminEmail is null or lower(r.adminEmail) like lower(concat('%', :adminEmail, '%')))
              and (:accion is null or r.accion = :accion)
              and (:tipoEntidad is null or r.tipoEntidad = :tipoEntidad)
              and (:entidadId is null or r.entidadId = :entidadId)
              and (:desde is null or r.fechaHora >= :desde)
              and (:hasta is null or r.fechaHora <= :hasta)
              and (:resultado is null or r.resultado = :resultado)
            """)
    Page<RegistroAuditoria> buscar(@Param("adminId") Long adminId,
                                   @Param("adminEmail") String adminEmail,
                                   @Param("accion") AccionAuditoria accion,
                                   @Param("tipoEntidad") TipoEntidad tipoEntidad,
                                   @Param("entidadId") String entidadId,
                                   @Param("desde") Instant desde,
                                   @Param("hasta") Instant hasta,
                                   @Param("resultado") ResultadoAuditoria resultado,
                                   Pageable pageable);

    /**
     * Lee el ultimo registro con bloqueo de escritura para que dos acciones administrativas concurrentes
     * no enlacen la misma fila como "anterior": el segundo admin espera y enlaza con el hash del primero.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from RegistroAuditoria r
            where r.id = (select max(r2.id) from RegistroAuditoria r2)
            """)
    Optional<RegistroAuditoria> buscarUltimoConBloqueo();

    /** Todos los registros en orden de encadenamiento, para verificacion de integridad. */
    List<RegistroAuditoria> findAllByOrderByIdAsc();

}
