package co.edu.uniquindio.littlestyle.modules.admin.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.shared.correo.CorreoService;
import co.edu.uniquindio.littlestyle.shared.event.CuentaEstadoCambiadaEvent;
import co.edu.uniquindio.littlestyle.shared.event.PrendaModeradaEvent;
import co.edu.uniquindio.littlestyle.shared.event.IncidenciaCerradaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notificaciones post-commit de la administracion (US-14). La apelacion automatica se crea durante la
 * transaccion del cambio de estado; este listener deja asincrono unicamente el correo. Los datos se
 * releen en contexto nuevo para evitar perezosos desanclados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionAdministracionListener {

    private final UsuarioRepository usuarioRepository;
    private final PrendaRepository prendaRepository;
    private final CorreoService correoService;

    /** Correo del cambio de cuenta. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void alCambiarEstadoCuenta(CuentaEstadoCambiadaEvent evento) {
        try {
            Usuario usuario = usuarioRepository.findById(evento.usuarioId()).orElse(null);
            if (usuario == null) {
                return;
            }
            switch (evento.estadoNuevo()) {
                case SUSPENDIDO -> {
                    correoService.enviarEstadoCuenta(usuario.getEmail(), usuario.getNombre(),
                            "suspendida", evento.motivo());
                }
                case BLOQUEADO -> {
                    correoService.enviarEstadoCuenta(usuario.getEmail(), usuario.getNombre(),
                            "bloqueada", evento.motivo());
                }
                case ACTIVO -> correoService.enviarEstadoCuenta(usuario.getEmail(), usuario.getNombre(),
                        "habilitada de nuevo", evento.motivo());
                default -> log.info("Cambio de cuenta {} no genera aviso", evento.estadoNuevo());
            }
        } catch (RuntimeException ex) {
            log.error("No se pudo enviar el correo por el cambio de estado de la cuenta {}",
                    evento.usuarioId(), ex);
        }
    }

    /** Correo al vendedor con el resultado de la moderacion de su producto. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void alModerarPrenda(PrendaModeradaEvent evento) {
        try {
            prendaRepository.buscarParaNotificacion(evento.prendaId()).ifPresent(prenda ->
                    notificarVendedor(prenda, evento));
        } catch (RuntimeException ex) {
            log.error("No se pudo notificar al vendedor por la moderacion de la prenda {}",
                    evento.prendaId(), ex);
        }
    }

    /** Envio del correo con la tolerancia de datos incompletos (prenda anonimizada o vendedor borrado). */
    private void notificarVendedor(Prenda prenda, PrendaModeradaEvent evento) {
        Usuario vendedor = prenda.getVendedor();
        if (vendedor == null) {
            return;
        }
        String resultado = switch (evento.resultado()) {
            case DESACTIVADA -> "desactivado";
            case APROBADA -> "aprobado y publicado";
            case SIN_REVISAR -> "quedo pendiente de revision";
        };
        correoService.enviarResultadoModeracion(vendedor.getEmail(), vendedor.getNombreTienda(),
                prenda.getNombre(), resultado, evento.motivo());
    }

    /** Correo al usuario reportante con la respuesta de cierre de su incidencia. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void alCerrarIncidencia(IncidenciaCerradaEvent evento) {
        String resultado = evento.resuelta() ? "resuelta" : "rechazada";
        correoService.enviarRespuestaIncidencia(evento.reportanteEmail(), nombreDe(evento.reportanteId()),
                evento.asunto(), resultado, evento.respuesta());
    }

    /** Nombre del reportante para el saludo del correo; vacio si la cuenta ya no existe. */
    private String nombreDe(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(Usuario::getNombre)
                .orElse("");
    }
}
