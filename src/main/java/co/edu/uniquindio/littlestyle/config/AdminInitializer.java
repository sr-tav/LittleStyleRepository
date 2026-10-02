package co.edu.uniquindio.littlestyle.config;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Crea la cuenta de administrador inicial, ya que los administradores no pueden registrarse
 * desde el formulario público. Solo se activa si {@code app.admin.email} está definido.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.admin", name = "email")
public class AdminInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.password}")
    private String password;

    @Override
    public void run(String... args) {
        String normalizado = email.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmail(normalizado)) {
            return;
        }
        usuarioRepository.save(Usuario.builder()
                .nombre("Administrador")
                .apellido("LittleStyle")
                .email(normalizado)
                .password(passwordEncoder.encode(password))
                .rol(Rol.ADMINISTRADOR)
                .estado(EstadoUsuario.ACTIVO)
                .build());
        log.info("Administrador inicial creado: {}", normalizado);
    }
}
