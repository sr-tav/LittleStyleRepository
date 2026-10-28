package co.edu.uniquindio.littlestyle.modules.admin.service;

import co.edu.uniquindio.littlestyle.modules.admin.dto.CrearIncidenciaRequest;
import co.edu.uniquindio.littlestyle.modules.admin.dto.IncidenciaResponse;
import co.edu.uniquindio.littlestyle.modules.admin.dto.ResolucionIncidenciaRequest;
import co.edu.uniquindio.littlestyle.modules.admin.model.AccionAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.EstadoIncidencia;
import co.edu.uniquindio.littlestyle.modules.admin.model.Incidencia;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoEntidad;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoIncidencia;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoReferenciaIncidencia;
import co.edu.uniquindio.littlestyle.modules.admin.repository.IncidenciaRepository;
import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.shared.dto.PaginaResponse;
import co.edu.uniquindio.littlestyle.shared.event.IncidenciaCerradaEvent;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ciclo de vida de las incidencias (US-14): las reporta el usuario (o las abre el sistema tras una
 * suspension: apelacion automatica) y las gestiona el administrador (tomar, resolver, rechazar).
 * <p>
 * Estados: ABIERTA &gt; EN_REVISION (tomar) &gt; RESUELTA o RECHAZADA (cierre, con respuesta obligatoria
 * que se envia por correo). Una incidencia ya cerrada no se reabre; si el cliente necesita decir algo mas,
 * reporta una nueva. El RNF-17 (mas del 90% de incidencias resueltas en menos de 24 horas) se mide con
 * {@link #tasaResolucion}, calculado en Java con {@link Duration} para no depender de funciones SQL
 * propias de MySQL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidenciaService {

    /** Ventana por defecto de la metrica RNF-17: ultimos 30 dias. */
    static final int DIAS_METRICA = 30;

    private final IncidenciaRepository incidenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PrendaRepository prendaRepository;
    private final AuditoriaService auditoriaService;
    private final ApplicationEventPublisher eventPublisher;

    /** Bandeja de incidencias del administrador, con filtros y paginacion. */
    @Transactional(readOnly = true)
    public PaginaResponse<IncidenciaResponse> listar(EstadoIncidencia estado, TipoIncidencia tipo,
                                                     Rol rol, String texto, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), normalizarTamano(tamano));
        String textoFiltrado = texto == null || texto.isBlank() ? null : texto.trim();
        return PaginaResponse.from(incidenciaRepository.buscar(estado, tipo, rol, textoFiltrado, pageable)
                .map(IncidenciaResponse::from));
    }

    /** Detalle de una incidencia para el administrador. */
    @Transactional(readOnly = true)
    public IncidenciaResponse obtener(Long id) {
        return incidenciaRepository.findById(id)
                .map(IncidenciaResponse::from)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "No existe una incidencia con id " + id));
    }

    /** El administrador toma una incidencia abierta y pasa a ocuparse de ella (EN_REVISION). */
    @Transactional
    public IncidenciaResponse tomar(Long id) {
        Incidencia incidencia = incidenciaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "No existe una incidencia con id " + id));
        Long adminId = SecurityUtils.usuarioActual()
                .orElseThrow(() -> new IllegalStateException("Sin usuario autenticado"))
                .id();
        if (incidencia.getEstado() != EstadoIncidencia.ABIERTA) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "La incidencia ya no esta abierta y no puede tomarse", "estado");
        }
        incidencia.setEstado(EstadoIncidencia.EN_REVISION);
        incidencia.setAdminId(adminId);
        incidenciaRepository.save(incidencia);
        auditoriaService.registrar(AccionAuditoria.TOMAR_INCIDENCIA, TipoEntidad.INCIDENCIA,
                String.valueOf(id), EstadoIncidencia.ABIERTA.name(),
                EstadoIncidencia.EN_REVISION.name(), null);
        return IncidenciaResponse.from(incidencia);
    }

    /** Resuelve una incidencia con respuesta obligatoria: el usuario recibe el cierre por correo. */
    @Transactional
    public IncidenciaResponse resolver(Long id, ResolucionIncidenciaRequest request) {
        return cerrar(id, request.respuesta(), EstadoIncidencia.RESUELTA, AccionAuditoria.RESOLVER_INCIDENCIA);
    }

    /** Rechaza una incidencia explicando por que: tambien se cuenta en la metrica del RNF-17. */
    @Transactional
    public IncidenciaResponse rechazar(Long id, ResolucionIncidenciaRequest request) {
        return cerrar(id, request.respuesta(), EstadoIncidencia.RECHAZADA, AccionAuditoria.RECHAZAR_INCIDENCIA);
    }

    /** Cierre compartido de resolver/rechazar: valida el estado, cierra, audita y publica el evento. */
    private IncidenciaResponse cerrar(Long id, String respuesta, EstadoIncidencia destino,
                                      AccionAuditoria accion) {
        Incidencia incidencia = incidenciaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "No existe una incidencia con id " + id));
        if (incidencia.getEstado() == EstadoIncidencia.RESUELTA
                || incidencia.getEstado() == EstadoIncidencia.RECHAZADA) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "La incidencia ya fue cerrada (" + incidencia.getEstado() + ")", "estado");
        }
        EstadoIncidencia anterior = incidencia.getEstado();
        incidencia.setEstado(destino);
        incidencia.setRespuesta(respuesta.trim());
        incidencia.setFechaResolucion(LocalDateTime.now());
        incidenciaRepository.save(incidencia);
        auditoriaService.registrar(accion, TipoEntidad.INCIDENCIA,
                String.valueOf(id), anterior != null ? anterior.name() : null, destino.name(),
                respuesta.trim());
        eventPublisher.publishEvent(new IncidenciaCerradaEvent(id, incidencia.getReportanteId(),
                incidencia.getReportanteEmail(), incidencia.getAsunto(),
                destino == EstadoIncidencia.RESUELTA, respuesta.trim()));
        return IncidenciaResponse.from(incidencia);
    }

    /** Reporta una incidencia desde la cuenta del usuario autenticado (cliente o vendedor). */
    @Transactional
    public IncidenciaResponse crear(CrearIncidenciaRequest request) {
        var actual = SecurityUtils.usuarioActual()
                .orElseThrow(() -> new IllegalStateException("Sin usuario autenticado"));
        Rol rol = actual.rol();
        Long usuarioId = actual.id();
        String email = actual.email();
        if (rol == Rol.ADMINISTRADOR) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "El administrador no reporta incidencias; gestione la bandeja del panel", "tipo");
        }
        if (request.tipo() == TipoIncidencia.APELACION_SUSPENSION) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Las apelaciones de suspension se abren automaticamente; reporte su caso con el tipo "
                            + "SOPORTE_GENERAL", "tipo");
        }
        String referenciaDetalle = request.referenciaDetalle();
        Long referenciaId = null;
        TipoReferenciaIncidencia referenciaTipo = null;
        if (request.tipo() == TipoIncidencia.APELACION_PRODUCTO) {
            if (rol != Rol.VENDEDOR) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "Solo el vendedor de la prenda puede apelar una moderación", "tipo");
            }
            Prenda prenda = validarReferenciaPropia(usuarioId, request.referenciaId());
            if (prenda.getEstadoModeracion() != EstadoModeracion.DESACTIVADA) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "Solo puede apelar una prenda desactivada por moderación", "referenciaId");
            }
            referenciaId = request.referenciaId();
            referenciaTipo = TipoReferenciaIncidencia.PRENDA;
            if (referenciaDetalle == null || referenciaDetalle.isBlank()) {
                referenciaDetalle = prenda.getNombre();
            }
        } else if (request.tipo() == TipoIncidencia.REPORTE_PRODUCTO) {
            if (rol != Rol.CLIENTE) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "Solo un cliente puede reportar una prenda", "tipo");
            }
            if (request.referenciaId() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "El reporte de producto necesita el id de la prenda", "referenciaId");
            }
            Prenda prenda = prendaRepository.findById(request.referenciaId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                            "No existe la prenda " + request.referenciaId(), "referenciaId"));
            referenciaId = prenda.getId();
            referenciaTipo = TipoReferenciaIncidencia.PRENDA;
            if (referenciaDetalle == null || referenciaDetalle.isBlank()) {
                referenciaDetalle = prenda.getNombre();
            }
        }
        Incidencia incidencia = Incidencia.builder()
                .reportanteId(usuarioId)
                .reportanteEmail(email)
                .reportanteRol(rol)
                .tipo(request.tipo())
                .referenciaId(referenciaId)
                .referenciaTipo(referenciaTipo)
                .referenciaDetalle(referenciaDetalle)
                .asunto(request.asunto().trim())
                .descripcion(request.descripcion().trim())
                .estado(EstadoIncidencia.ABIERTA)
                .fechaCreacion(LocalDateTime.now())
                .build();
        return IncidenciaResponse.from(incidenciaRepository.save(incidencia));
    }

    /** Historial paginado de incidencias del usuario autenticado. */
    @Transactional(readOnly = true)
    public PaginaResponse<IncidenciaResponse> listarPropias(int pagina, int tamano) {
        Long usuarioId = SecurityUtils.usuarioActual()
                .orElseThrow(() -> new IllegalStateException("Sin usuario autenticado"))
                .id();
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), normalizarTamano(tamano));
        return PaginaResponse.from(
                incidenciaRepository.findAllByReportanteIdOrderByFechaCreacionDesc(usuarioId, pageable)
                        .map(IncidenciaResponse::from));
    }

    /**
     * Abre la apelacion automatica cuando el administrador suspende o bloquea una cuenta: el usuario no
     * puede iniciar sesion para reportarla, asi que el sistema la abre por el con el motivo como base.
     * No duplica si la persona ya tenia una apelacion en curso.
     */
    @Transactional
    public void abrirApelacionAutomatica(Long usuarioId, EstadoUsuario nuevoEstado, String motivoAdmin) {
        boolean tieneApelacionAbierta = incidenciaRepository.existsByReportanteIdAndTipoAndEstadoIn(
                usuarioId, TipoIncidencia.APELACION_SUSPENSION,
                List.of(EstadoIncidencia.ABIERTA, EstadoIncidencia.EN_REVISION));
        if (tieneApelacionAbierta) {
            log.info("Apelacion duplicada omitida para la cuenta {}", usuarioId);
            return;
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "No existe una cuenta con id " + usuarioId));
        Incidencia incidencia = Incidencia.builder()
                .reportanteId(usuarioId)
                .reportanteEmail(usuario.getEmail())
                .reportanteRol(usuario.getRol())
                .tipo(TipoIncidencia.APELACION_SUSPENSION)
                .asunto(nuevoEstado == EstadoUsuario.BLOQUEADO
                        ? "Apelacion: cuenta bloqueada y pide revision del admin"
                        : "Apelacion: cuenta suspendida y pide revision del admin")
                .descripcion(nuevoEstado == EstadoUsuario.BLOQUEADO
                        ? "Su cuenta fue bloqueada. Motivo del administrador: " + motivoAdmin
                        + ". Responda a esta incidencia si considera que el bloqueo corresponde."
                        : "Su cuenta fue suspendida. Motivo del administrador: " + motivoAdmin
                        + ". El administrador respondera a esta apelacion.")
                .estado(EstadoIncidencia.ABIERTA)
                .fechaCreacion(LocalDateTime.now())
                .build();
        incidenciaRepository.save(incidencia);
    }

    /**
     * Metrica RNF-17: porcentaje de incidencias cerradas en la ventana [desde, hasta] que se resolvieron
     * en menos de 24 horas desde su creacion. Sin incidencias cerradas devuelve 0.0; el conteo va por
     * fechaResolucion, de modo que una ventana vacia no rompe.
     */
    @Transactional(readOnly = true)
    public double tasaResolucion(LocalDateTime desde, LocalDateTime hasta) {
        List<Incidencia> resueltas = incidenciaRepository
                .findByEstadoAndFechaResolucionBetween(EstadoIncidencia.RESUELTA, desde, hasta);
        List<Incidencia> rechazadas = incidenciaRepository
                .findByEstadoAndFechaResolucionBetween(EstadoIncidencia.RECHAZADA, desde, hasta);
        long total = resueltas.size() + rechazadas.size();
        if (total == 0) {
            return 0.0;
        }
        long dentroDe24h = resueltas.stream()  // el RNF exige resoluciones de menos de 24 horas
                .filter(i -> Duration.between(i.getFechaCreacion(), i.getFechaResolucion())
                        .compareTo(Duration.ofHours(24)) < 0)
                .count();
        return Math.round(dentroDe24h * 1000.0 / total) / 10.0;
    }

    /** La prenda apelada tiene que existir y ser del reportante: no se apelan productos ajenos. */
    private Prenda validarReferenciaPropia(Long usuarioId, Long prendaId) {
        if (prendaId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La apelacion de producto necesita el id del producto", "referenciaId");
        }
        return prendaRepository.findByIdAndVendedorId(prendaId, usuarioId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "No existe un producto " + prendaId + " en su tienda", "referenciaId"));
    }

    /** Limita el tamano de pagina al maximo permitido y usa 10 cuando es invalido. */
    private int normalizarTamano(int tamano) {
        if (tamano < 1) {
            return 10;
        }
        return Math.min(tamano, AdminUsuarioService.TAMANO_MAXIMO);
    }
}
