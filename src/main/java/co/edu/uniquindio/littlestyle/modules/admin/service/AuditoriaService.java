package co.edu.uniquindio.littlestyle.modules.admin.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.admin.model.AccionAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.RegistroAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.ResultadoAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoEntidad;
import co.edu.uniquindio.littlestyle.modules.admin.repository.RegistroAuditoriaRepository;
import co.edu.uniquindio.littlestyle.shared.dto.PaginaResponse;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Escribe y verifica la pista de auditoria administrativa (US-14, RNF-08).
 * <p>
 * Cada registro se encadena con su antecesor: el hash de una accion es el SHA-256 del contenido del
 * registro y del hash del anterior, de modo que borrar, editar o reordenar registros en la base rompe la
 * cadena y se detecta al verificar la integridad.
 * <p>
 * El registro de exito se hace DENTRO de la misma transaccion de la accion administrada: si la accion
 * falla y la transaccion se revierte, el registro tambien se revierte (no queda auditar una accion que no
 * ocurrio). Los intentos fallidos o denegados usan {@link #registrarIntentoFallido} con transaccion
 * independiente (REQUIRES_NEW), para que la pista sobreviva al rollback del flujo principal.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    /** Hash semilla de la cadena: el primer registro de la tabla enlaza con este valor. */
    public static final String GENESIS = "0".repeat(64);

    private final RegistroAuditoriaRepository registroRepository;

    /** Resultado de la verificacion de integridad que consume el visor del admin. */
    public record ResultadoIntegridad(long totalRegistros, boolean integra,
                                      Long primerInconsistenteId, String detalle) {

        /** Resultado de una tabla vacia (trivialmente integra). */
        public static ResultadoIntegridad vacia() {
            return new ResultadoIntegridad(0, true, null, null);
        }
    }

    /**
     * Registra una accion administrativa EXITOSA dentro de la transaccion actual. Exige que el llamador
     * sea el administrador autenticado: si se invoca desde otro contexto es un error de configuracion y se
     * aborta la operacion.
     */
    @Transactional
    public RegistroAuditoria registrar(AccionAuditoria accion, TipoEntidad tipoEntidad, String entidadId,
                                       String valorAnterior, String valorNuevo, String motivo) {
        AuthenticatedUser admin = adminActual();
        RegistroAuditoria registro = registroConHash(RegistroAuditoria.builder()
                .fechaHora(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .adminId(admin.id())
                .adminEmail(admin.email())
                .rol(admin.rol().name())
                .accion(accion)
                .tipoEntidad(tipoEntidad)
                .entidadId(entidadId)
                .valorAnterior(valorAnterior)
                .valorNuevo(valorNuevo)
                .motivo(motivo)
                .ipOrigen(ipOrigenActual())
                .resultado(ResultadoAuditoria.EXITO)
                .hashAnterior(hashAnterior())
                .build());
        return registroRepository.save(registro);
    }

    /**
     * Registra un intento DENEGADO o fallido en una transaccion independiente, de modo que la pista quede
     * aunque la operacion principal haga rollback por el error de negocio que disparo este registro.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RegistroAuditoria registrarIntentoFallido(AccionAuditoria accion, TipoEntidad tipoEntidad,
                                                     String entidadId, String motivo) {
        AuthenticatedUser admin = adminActual();
        RegistroAuditoria registro = registroConHash(RegistroAuditoria.builder()
                .fechaHora(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .adminId(admin.id())
                .adminEmail(admin.email())
                .rol(admin.rol().name())
                .accion(accion)
                .tipoEntidad(tipoEntidad)
                .entidadId(entidadId)
                .motivo(motivo)
                .ipOrigen(ipOrigenActual())
                .resultado(ResultadoAuditoria.FALLO)
                .hashAnterior(hashAnterior())
                .build());
        return registroRepository.save(registro);
    }

    /**
     * Recorre la cadena completa recalculando cada hash sobre los datos almacenados: si alguien edita una
     * fila, cambia su orden o inserta una a mano, el hash recalculado deja de coincidir y la verificacion
     * reporta el primer id inconsistente con su detalle.
     */
    @Transactional(readOnly = true)
    public ResultadoIntegridad verificarIntegridad() {
        var registros = registroRepository.findAllByOrderByIdAsc();
        long total = registros.size();
        if (total == 0) {
            return ResultadoIntegridad.vacia();
        }
        String hashRecorrido = GENESIS;
        for (RegistroAuditoria registro : registros) {
            String hashRecalculado = calcularHash(registro, hashRecorrido);
            if (!hashRecalculado.equals(registro.getHash())
                    || !hashRecorrido.equals(registro.getHashAnterior())) {
                String detalle = "El registro con id " + registro.getId()
                        + " no coincide con la cadena: hash anterior esperado " + hashRecorrido
                        + ", almacenado " + registro.getHashAnterior();
                return new ResultadoIntegridad(total, false, registro.getId(), detalle);
            }
            hashRecorrido = registro.getHash();
        }
        return new ResultadoIntegridad(total, true, null, null);
    }

    /** Consulta paginada de auditoria con filtros de administrador, accion, entidad y rango temporal. */
    @Transactional(readOnly = true)
    public PaginaResponse<RegistroAuditoria> buscar(Long adminId, String adminEmail,
                                                     AccionAuditoria accion, TipoEntidad tipoEntidad,
                                                     String entidadId, Instant desde, Instant hasta,
                                                     ResultadoAuditoria resultado, int pagina, int tamano) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La fecha inicial no puede ser posterior a la fecha final", "desde");
        }
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), tamano < 1 ? 10 : Math.min(tamano, 50));
        String emailFiltro = adminEmail == null || adminEmail.isBlank() ? null : adminEmail.trim();
        return PaginaResponse.from(registroRepository.buscar(adminId, emailFiltro, accion, tipoEntidad,
                entidadId, desde, hasta, resultado, pageable));
    }

    /** Hash del último registro bajo el candado global, incluido el caso de la cadena vacía. */
    private String hashAnterior() {
        registroRepository.bloquearCadena();
        return registroRepository.buscarUltimoConBloqueo()
                .map(RegistroAuditoria::getHash)
                .orElse(GENESIS);
    }

    /** Calcula el hash SHA-256 del registro enlazado con su hash anterior y lo deja fijado. */
    private RegistroAuditoria registroConHash(RegistroAuditoria registro) {
        String hash = calcularHash(registro, registro.getHashAnterior());
        return registro.conHash(hash);
    }

    /** Resumen SHA-256 de todos los campos del registro, en orden estable, mas el hash del anterior. */
    String calcularHash(RegistroAuditoria r, String hashAnterior) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            actualizarCampo(sha256, String.valueOf(r.getFechaHora()));
            actualizarCampo(sha256, String.valueOf(r.getAdminId()));
            actualizarCampo(sha256, r.getAdminEmail());
            actualizarCampo(sha256, r.getRol());
            actualizarCampo(sha256, r.getAccion().name());
            actualizarCampo(sha256, r.getTipoEntidad().name());
            actualizarCampo(sha256, r.getEntidadId());
            actualizarCampo(sha256, r.getValorAnterior());
            actualizarCampo(sha256, r.getValorNuevo());
            actualizarCampo(sha256, r.getMotivo());
            actualizarCampo(sha256, r.getIpOrigen());
            actualizarCampo(sha256, r.getResultado().name());
            actualizarCampo(sha256, hashAnterior);
            return HexFormat.of().formatHex(sha256.digest());
        } catch (NoSuchAlgorithmException imposible) {
            // Todo JVM moderno soporta SHA-256; no hay camino realista a esta excepcion.
            throw new IllegalStateException("La JVM no soporta SHA-256", imposible);
        }
    }

    /** Codifica cada campo con marca de nulidad y longitud para evitar colisiones por separadores. */
    private static void actualizarCampo(MessageDigest digest, String valor) {
        if (valor == null) {
            digest.update((byte) 0);
            return;
        }
        byte[] bytes = valor.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) 1);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    /** El administrador autenticado en la peticion actual; si no lo hay, es un error de configuracion. */
    private AuthenticatedUser adminActual() {
        Optional<AuthenticatedUser> usuario = SecurityUtils.usuarioActual();
        AuthenticatedUser admin = usuario.orElseThrow(() ->
                new IllegalStateException("Auditoria sin usuario autenticado: falta el filtro de seguridad"));
        if (admin.id() == null || admin.email() == null || admin.rol() == null
                || admin.rol() != co.edu.uniquindio.littlestyle.modules.auth.model.Rol.ADMINISTRADOR) {
            throw new IllegalStateException(
                    "La auditoria solo se escribe desde acciones del rol ADMINISTRADOR");
        }
        return admin;
    }

    /** IP del cliente de la petición HTTP actual, igual que en el flujo de login. */
    private String ipOrigenActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRemoteAddr();
        }
        return null;
    }

}
