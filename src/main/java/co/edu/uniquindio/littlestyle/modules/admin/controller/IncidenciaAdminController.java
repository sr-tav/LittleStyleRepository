package co.edu.uniquindio.littlestyle.modules.admin.controller;

import co.edu.uniquindio.littlestyle.modules.admin.dto.IncidenciaResponse;
import co.edu.uniquindio.littlestyle.modules.admin.dto.ResolucionIncidenciaRequest;
import co.edu.uniquindio.littlestyle.modules.admin.model.EstadoIncidencia;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoIncidencia;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.admin.service.AuditoriaService;
import co.edu.uniquindio.littlestyle.modules.admin.service.IncidenciaService;
import co.edu.uniquindio.littlestyle.shared.dto.PaginaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bandeja de incidencias del administrador (US-14): tomar, resolver y rechazar con respuesta
 * obligatoria; el cierre dispara el correo al usuario. Cada accion queda auditada en la misma
 * transaccion del servicio.
 */
@RestController
@RequestMapping("/api/admin/incidencias")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class IncidenciaAdminController {

    private final IncidenciaService service;
    private final AuditoriaService auditoriaService;

    /** Bandeja paginada de incidencias con filtros opcionales por estado, tipo, rol y texto. */
    @GetMapping
    public PaginaResponse<IncidenciaResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            @RequestParam(required = false) EstadoIncidencia estado,
            @RequestParam(required = false) TipoIncidencia tipo,
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) String texto) {
        return service.listar(estado, tipo, rol, texto, pagina, tamano);
    }

    /** Detalle de una incidencia. */
    @GetMapping("/{id}")
    public IncidenciaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** Toma la incidencia (ABIERTA &gt; EN_REVISION) y la asigna al admin autenticado. */
    @PostMapping("/{id}/tomar")
    public IncidenciaResponse tomar(@PathVariable Long id) {
        return service.tomar(id);
    }

    /** Resuelve la incidencia con la respuesta obligatoria que recibe el usuario por correo. */
    @PostMapping("/{id}/resolver")
    public IncidenciaResponse resolver(@PathVariable Long id,
                                       @Valid @RequestBody ResolucionIncidenciaRequest request) {
        return service.resolver(id, request);
    }

    /** Rechaza la incidencia explicando el por que (la respuesta tambien llega por correo). */
    @PostMapping("/{id}/rechazar")
    public IncidenciaResponse rechazar(@PathVariable Long id,
                                       @Valid @RequestBody ResolucionIncidenciaRequest request) {
        return service.rechazar(id, request);
    }
}
