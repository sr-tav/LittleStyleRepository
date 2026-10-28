package co.edu.uniquindio.littlestyle.modules.admin.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.admin.dto.PrendaAdminDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.admin.dto.PrendaAdminResponse;
import co.edu.uniquindio.littlestyle.modules.admin.model.AccionAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoEntidad;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.shared.dto.PaginaResponse;
import co.edu.uniquindio.littlestyle.shared.event.PrendaModeradaEvent;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * Moderacion reactiva de productos (US-14): el vendedor publica con libertad y es el administrador quien
 * retira (DESACTIVADA, con motivo obligatorio), aprueba o restaura despues. El eje de moderacion es
 * ortogonal al estado comercial (ACTIVA/INACTIVA), asi un rechazo no destruye el inventario ni el
 * historial del vendedor, y una restauracion vuelve a publicar la prenda tal como estaba.
 * <p>
 * Transiciones válidas (estadoModeracion actual &gt; operación): SIN_REVISAR &gt; aprobada/desactivada;
 * APROBADA &gt; desactivada; DESACTIVADA &gt; aprobada (restaurar). Repetir la misma operación es 409.
 */
@Service
@RequiredArgsConstructor
public class ModeracionProductoService {

    private final PrendaRepository prendaRepository;
    private final AuditoriaService auditoriaService;
    private final ApplicationEventPublisher eventPublisher;

    /** Bandeja paginada de prendas para moderar, con filtros opcionales; la mas recientemente modificada va de primero. */
    @Transactional(readOnly = true)
    public PaginaResponse<PrendaAdminResponse> listar(String vendedorEmail, EstadoPrenda estadoPrenda,
                                                      EstadoModeracion estadoModeracion,
                                                      String texto, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), normalizarTamano(tamano));
        String vendedorFiltro = vendedorEmail == null || vendedorEmail.isBlank() ? null : vendedorEmail.trim();
        String textoFiltrado = texto == null || texto.isBlank() ? null : texto.trim();
        return PaginaResponse.from(prendaRepository.buscarModeracion(
                        vendedorFiltro, estadoPrenda, estadoModeracion, textoFiltrado, pageable)
                .map(PrendaAdminResponse::from));
    }

    /** Detalle completo de una prenda para decidir la moderacion, con composicion, tallas e imagenes. */
    @Transactional(readOnly = true)
    public PrendaAdminDetalleResponse obtenerDetalle(Long id) {
        Prenda prenda = prendaRepository.buscarDetalleAdmin(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "No existe una prenda con id " + id));
        return PrendaAdminDetalleResponse.from(prenda);
    }

    /** Aprueba una prenda pendiente o rechazada; la deja publicable junto con las demas condiciones. */
    @Transactional
    public PrendaAdminResponse aprobar(Long id) {
        return auditarFallo(id, AccionAuditoria.APROBAR_PRODUCTO, () -> aprobarInterno(id));
    }

    private PrendaAdminResponse aprobarInterno(Long id) {
        Prenda prenda = cargarConBloqueo(id);
        EstadoModeracion anterior = prenda.getEstadoModeracion();
        if (anterior == EstadoModeracion.APROBADA) {
            throw rechazo409("La prenda ya esta aprobada");
        }
        marcarDecisionAdmin(prenda, null);
        prenda.setEstadoModeracion(EstadoModeracion.APROBADA);
        prenda.setMotivoModeracion(null);
        prendaRepository.save(prenda);
        AccionAuditoria accion = anterior == EstadoModeracion.DESACTIVADA
                ? AccionAuditoria.RESTAURAR_PRODUCTO
                : AccionAuditoria.APROBAR_PRODUCTO;
        auditoriaService.registrar(accion, TipoEntidad.PRENDA, String.valueOf(id),
                anterior.name(), EstadoModeracion.APROBADA.name(), null);
        eventPublisher.publishEvent(new PrendaModeradaEvent(id, prenda.getVendedor().getId(),
                EstadoModeracion.APROBADA, null));
        return PrendaAdminResponse.from(prenda);
    }

    /** Restaura una prenda rechazada: el equivalente de aprobarla cuando venia de un rechazo. */
    @Transactional
    public PrendaAdminResponse restaurar(Long id) {
        return auditarFallo(id, AccionAuditoria.RESTAURAR_PRODUCTO, () -> restaurarInterno(id));
    }

    private PrendaAdminResponse restaurarInterno(Long id) {
        Prenda prenda = cargarConBloqueo(id);
        if (prenda.getEstadoModeracion() != EstadoModeracion.DESACTIVADA) {
            throw rechazo409("Solo se puede restaurar una prenda desactivada");
        }
        marcarDecisionAdmin(prenda, null);
        prenda.setEstadoModeracion(EstadoModeracion.APROBADA);
        prenda.setMotivoModeracion(null);
        prendaRepository.save(prenda);
        auditoriaService.registrar(AccionAuditoria.RESTAURAR_PRODUCTO, TipoEntidad.PRENDA,
                String.valueOf(id), EstadoModeracion.DESACTIVADA.name(),
                EstadoModeracion.APROBADA.name(), null);
        eventPublisher.publishEvent(new PrendaModeradaEvent(id, prenda.getVendedor().getId(),
                EstadoModeracion.APROBADA, null));
        return PrendaAdminResponse.from(prenda);
    }

    /** Rechaza una prenda que esta pendiente o aprobada; el motivo queda visible para el vendedor. */
    @Transactional
    public PrendaAdminResponse rechazar(Long id, String motivo) {
        return auditarFallo(id, AccionAuditoria.DESACTIVAR_PRODUCTO, () -> rechazarInterno(id, motivo));
    }

    private PrendaAdminResponse rechazarInterno(Long id, String motivo) {
        if (motivo == null || motivo.isBlank() || motivo.trim().length() < 10
                || motivo.trim().length() > 500) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El motivo de desactivación es obligatorio y debe tener entre 10 y 500 caracteres", "motivo");
        }
        Prenda prenda = cargarConBloqueo(id);
        if (prenda.getEstadoModeracion() == EstadoModeracion.DESACTIVADA) {
            throw rechazo409("La prenda ya esta desactivada");
        }
        EstadoModeracion anterior = prenda.getEstadoModeracion();
        marcarDecisionAdmin(prenda, motivo.trim());
        prenda.setEstadoModeracion(EstadoModeracion.DESACTIVADA);
        prenda.setMotivoModeracion(motivo.trim());
        prendaRepository.save(prenda);
        auditoriaService.registrar(AccionAuditoria.DESACTIVAR_PRODUCTO, TipoEntidad.PRENDA,
                String.valueOf(id), anterior.name(), EstadoModeracion.DESACTIVADA.name(), motivo.trim());
        eventPublisher.publishEvent(new PrendaModeradaEvent(id, prenda.getVendedor().getId(),
                EstadoModeracion.DESACTIVADA, motivo.trim()));
        return PrendaAdminResponse.from(prenda);
    }

    /** Conserva en la pista los intentos de moderación que terminan en un error de negocio. */
    private PrendaAdminResponse auditarFallo(Long id, AccionAuditoria accion,
                                             Supplier<PrendaAdminResponse> operacion) {
        try {
            return operacion.get();
        } catch (BusinessException ex) {
            auditoriaService.registrarIntentoFallido(accion, TipoEntidad.PRENDA, String.valueOf(id),
                    "Intento de moderación fallido: " + ex.getMessage());
            throw ex;
        }
    }

    /** Carga la prenda con bloqueo de fila, sin importar su estado (asi se moderan rechazadas incluidas). */
    private Prenda cargarConBloqueo(Long id) {
        return prendaRepository.bloquearPorId(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "No existe una prenda con id " + id));
    }

    /** Excepcion de conflicto reutilizada por las transiciones invalidas. */
    private BusinessException rechazo409(String mensaje) {
        return new BusinessException(HttpStatus.CONFLICT, mensaje, "estadoModeracion");
    }

    /** Registra en la prenda la fecha y el admin responsables de su ultima decision de moderacion. */
    private void marcarDecisionAdmin(Prenda prenda, String motivo) {
        AuthenticatedUser admin = SecurityUtils.usuarioActual()
                .orElseThrow(() -> new IllegalStateException(
                        "La moderacion requiere un administrador autenticado"));
        if (admin.id() == null
                || admin.rol() != co.edu.uniquindio.littlestyle.modules.auth.model.Rol.ADMINISTRADOR) {
            throw new IllegalStateException("La moderacion solo puede ejecutarse como administrador");
        }
        prenda.setFechaModeracion(java.time.LocalDateTime.now());
        prenda.setAdminModeracionId(admin.id());
        prenda.setMotivoModeracion(motivo);
    }

    /** Limita el tamano de pagina al maximo permitido y usa 10 cuando es invalido. */
    private int normalizarTamano(int tamano) {
        if (tamano < 1) {
            return 10;
        }
        return Math.min(tamano, AdminUsuarioService.TAMANO_MAXIMO);
    }
}
