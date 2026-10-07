package co.edu.uniquindio.littlestyle.modules.auth.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Verifica que la cuenta dueña de un JWT siga activa. Permite que la suspensión de un usuario
 * invalide de inmediato los tokens que ya tenía emitidos, en lugar de esperar a que expiren.
 */
@Service
@RequiredArgsConstructor
public class EstadoCuentaService {

    private final UsuarioRepository usuarioRepository;

    /** {@code false} si la cuenta no está activa o ya no existe. */
    public boolean estaActiva(Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        return usuarioRepository.findEstadoById(usuarioId)
                .map(estado -> estado == EstadoUsuario.ACTIVO)
                .orElse(false);
    }
}
