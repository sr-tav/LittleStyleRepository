package co.edu.uniquindio.littlestyle.modules.auth.repository;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Solo lee la columna de estado; se consulta en cada petición autenticada. */
    @Query("select u.estado from Usuario u where u.id = :id")
    Optional<EstadoUsuario> findEstadoById(@Param("id") Long id);
}
