package co.edu.uniquindio.littlestyle.modules.admin.controller;

import co.edu.uniquindio.littlestyle.modules.admin.model.AccionAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.RegistroAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.ResultadoAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoEntidad;
import co.edu.uniquindio.littlestyle.modules.admin.service.AuditoriaService;
import co.edu.uniquindio.littlestyle.shared.dto.PaginaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Visor de SOLO LECTURA de la pista de auditoria administrativa (US-14, RNF-08): no existe ningun
 * endpoint de escritura ni de borrado; el repositorio de auditoria tampoco los declara. Incluye la
 * verificacion de integridad de la cadena de hashes.
 */
@RestController
@RequestMapping("/api/admin/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AuditoriaAdminController {

    private final AuditoriaService auditoriaService;

    /** Consulta paginada de la pista de auditoria con filtros opcionales. */
    @GetMapping
    public PaginaResponse<RegistroAuditoria> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String adminEmail,
            @RequestParam(required = false) AccionAuditoria accion,
            @RequestParam(required = false) TipoEntidad tipoEntidad,
            @RequestParam(required = false) String entidadId,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta,
            @RequestParam(required = false) ResultadoAuditoria resultado) {
        return auditoriaService.buscar(adminId, adminEmail, accion, tipoEntidad, entidadId,
                desde, hasta, resultado, pagina, tamano);
    }

    /** Verifica la cadena de hashes y reporta si la pista esta integra. */
    @GetMapping("/integridad")
    public ResponseEntity<AuditoriaService.ResultadoIntegridad> integridad() {
        return ResponseEntity.ok(auditoriaService.verificarIntegridad());
    }
}
