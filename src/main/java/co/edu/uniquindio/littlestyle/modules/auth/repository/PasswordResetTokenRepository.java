package co.edu.uniquindio.littlestyle.modules.auth.repository;

import co.edu.uniquindio.littlestyle.modules.auth.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repositorio de tokens temporales para recuperación de contraseña. */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByEmail(String email);
}

