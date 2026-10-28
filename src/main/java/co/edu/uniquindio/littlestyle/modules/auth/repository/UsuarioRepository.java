package co.edu.uniquindio.littlestyle.modules.auth.repository;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Acceso a los usuarios de la plataforma. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Busca un usuario por su correo (ya normalizado en minúsculas). */
    Optional<Usuario> findByEmail(String email);

    /** Indica si el correo ya está registrado. */
    boolean existsByEmail(String email);

    /**
     * Carga el usuario con bloqueo de escritura para que dos cambios simultáneos a la misma cuenta no se
     * pisen.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdForUpdate(@Param("id") Long id);

    /** Solo lee la columna de estado; se consulta en cada petición autenticada. */
    @Query("select u.estado from Usuario u where u.id = :id")
    Optional<EstadoUsuario> findEstadoById(@Param("id") Long id);

    /**
     * Bandeja de gestion de cuentas del administrador (US-14): filtra por rol, estado y texto libre
     * (nombre, apellido, correo o tienda).
     */
    @Query("""
            select u from Usuario u
            where (:rol is null or u.rol = :rol)
              and (:estado is null or u.estado = :estado)
              and (:texto is null or :texto = ''
                   or lower(u.nombre) like lower(concat('%', :texto, '%'))
                   or lower(u.apellido) like lower(concat('%', :texto, '%'))
                   or lower(u.email) like lower(concat('%', :texto, '%'))
                   or lower(coalesce(u.nombreTienda, '')) like lower(concat('%', :texto, '%')))
            order by u.fechaRegistro desc, u.id desc
            """)
    Page<Usuario> buscarAdmin(@Param("rol") Rol rol, @Param("estado") EstadoUsuario estado,
                              @Param("texto") String texto, Pageable pageable);

    /** Cuenta cuentas por estado, para el resumen (KPIs) del administrador. */
    long countByEstado(EstadoUsuario estado);
}
