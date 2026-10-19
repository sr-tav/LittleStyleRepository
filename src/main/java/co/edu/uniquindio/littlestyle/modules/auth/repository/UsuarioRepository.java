package co.edu.uniquindio.littlestyle.modules.auth.repository;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import jakarta.persistence.LockModeType;
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
}
