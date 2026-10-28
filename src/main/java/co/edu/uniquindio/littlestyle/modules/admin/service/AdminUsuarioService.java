package co.edu.uniquindio.littlestyle.modules.admin.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.admin.dto.UsuarioAdminResponse;
import co.edu.uniquindio.littlestyle.modules.admin.model.AccionAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoEntidad;
import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.shared.dto.PaginaResponse;
import co.edu.uniquindio.littlestyle.shared.event.CuentaEstadoCambiadaEvent;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion administrativa de cuentas (US-14): habilitar, suspender y bloquear clientes o vendedores con
 * auditoria y aviso al usuario (via eventos, que a su vez abren la apelacion automatica y envian correo).
 * <p>
 * Matriz de transiciones (estado actual &gt; nuevo):
 * <ul>
 *   <li>{@code ACTIVO} &gt; SUSPENDIDO o BLOQUEADO.</li>
 *   <li>{@code SUSPENDIDO} &gt; ACTIVO o BLOQUEADO.</li>
 *   <li>{@code BLOQUEADO} &gt; ACTIVO o SUSPENDIDO (el admin decide tras revisar la apelacion).</li>
 *   <li>{@code ELIMINADO} no es destino posible (lo decide el titular al eliminar su cuenta) y una cuenta
 *   eliminada no se administra.</li>
 * </ul>
 * Restricciones: el administrador no actua sobre cuentas administradoras ni sobre su propia cuenta.
 */
@Service
@RequiredArgsConstructor
public class AdminUsuarioService {

    /** Tamano maximo de pagina que acepta el API (RNF-11: degradacion de respuestas grandes). */
    static final int TAMANO_MAXIMO = 50;

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final IncidenciaService incidenciaService;
    private final ApplicationEventPublisher eventPublisher;

    /** Bandeja paginada de cuentas con filtros opcionales por rol, estado y texto libre. */
    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioAdminResponse> listar(Rol rol, EstadoUsuario estado,
                                                       String texto, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), normalizarTamano(tamano));
        String textoFiltrado = texto == null || texto.isBlank() ? null : texto.trim();
        return PaginaResponse.from(usuarioRepository.buscarAdmin(rol, estado, textoFiltrado, pageable)
                .map(UsuarioAdminResponse::from));
    }

    /** Detalle de una cuenta en el formato de la bandeja admin. */
    @Transactional(readOnly = true)
    public UsuarioAdminResponse obtener(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioAdminResponse::from)
                .orElseThrow(() -> cuentaNoEncontrada(id));
    }

    /** Cambia el estado de una cuenta segun la matriz, con auditoria en la misma transaccion y evento. */
    @Transactional
    public UsuarioAdminResponse cambiarEstado(Long id, EstadoUsuario estadoDestino, String motivo) {
        EstadoUsuario destino = estadoDestinoValido(estadoDestino);
        if (motivo == null || motivo.isBlank() || motivo.trim().length() < 10
                || motivo.trim().length() > 500) {
            auditoriaService.registrarIntentoFallido(accionDe(destino), TipoEntidad.USUARIO,
                    String.valueOf(id), "Cambio de estado rechazado: el motivo debe tener entre 10 y 500 caracteres");
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El motivo es obligatorio y debe tener entre 10 y 500 caracteres", "motivo");
        }
        String motivoValidado = motivo.trim();
        try {
            Usuario usuario = usuarioRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> cuentaNoEncontrada(id));
            validarTransicion(usuario, destino);
            EstadoUsuario anterior = usuario.getEstado();
            usuario.setEstado(destino);
            usuarioRepository.save(usuario);
            if (destino == EstadoUsuario.SUSPENDIDO || destino == EstadoUsuario.BLOQUEADO) {
                incidenciaService.abrirApelacionAutomatica(id, destino, motivoValidado);
            }
            auditoriaService.registrar(accionDe(destino), TipoEntidad.USUARIO,
                    String.valueOf(id), anterior.name(), destino.name(), motivoValidado);
            eventPublisher.publishEvent(new CuentaEstadoCambiadaEvent(id, anterior, destino, motivoValidado));
            return UsuarioAdminResponse.from(usuario);
        } catch (BusinessException ex) {
            // Intento denegado o fallido: queda en la pista con transaccion aparte y se relanza el 400/403/409.
            auditoriaService.registrarIntentoFallido(accionDe(destino), TipoEntidad.USUARIO,
                    String.valueOf(id), "Intento " + destino.name() + " rechazado: " + ex.getMessage());
            throw ex;
        }
    }

    /** 404 con mensaje claro; es la misma respuesta para toda la bandeja. */
    private BusinessException cuentaNoEncontrada(Long id) {
        return new BusinessException(HttpStatus.NOT_FOUND, "No existe una cuenta con id " + id);
    }

    /** Rechaza un destino nulo o ELIMINADO: son errores del emisor, antes de tocar la cuenta. */
    private EstadoUsuario estadoDestinoValido(EstadoUsuario estado) {
        if (estado == null || estado == EstadoUsuario.ELIMINADO) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Solo puede activar, suspender o bloquear la cuenta", "estado");
        }
        return estado;
    }

    /** Reglas de negocio del cambio de estado: rol, propiedad, operabilidad y no-ops. */
    private void validarTransicion(Usuario usuario, EstadoUsuario destino) {
        Long adminId = SecurityUtils.usuarioActual()
                .map(AuthenticatedUser::id)
                .orElse(null);
        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "No se puede cambiar el estado de una cuenta administradora", "estado");
        }
        if (adminId != null && adminId.equals(usuario.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "No puede cambiar el estado de su propia cuenta", "estado");
        }
        if (usuario.getEstado() == EstadoUsuario.ELIMINADO) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "La cuenta fue eliminada por su titular y no se administra", "estado");
        }
        if (usuario.getEstado() == destino) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "La cuenta ya esta en estado " + destino.name(), "estado");
        }
    }

    /** Accion de auditoria segun el estado destino. */
    private AccionAuditoria accionDe(EstadoUsuario destino) {
        return switch (destino) {
            case ACTIVO -> AccionAuditoria.HABILITAR_USUARIO;
            case SUSPENDIDO -> AccionAuditoria.SUSPENDER_USUARIO;
            case BLOQUEADO -> AccionAuditoria.BLOQUEAR_USUARIO;
            default -> AccionAuditoria.ACCION_FALLIDA;
        };
    }

    /** Limita el tamano de pagina al maximo permitido y usa 10 cuando es invalido. */
    private int normalizarTamano(int tamano) {
        if (tamano < 1) {
            return 10;
        }
        return Math.min(tamano, TAMANO_MAXIMO);
    }
}
