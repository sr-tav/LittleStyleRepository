package co.edu.uniquindio.littlestyle.modules.auth.repository;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
