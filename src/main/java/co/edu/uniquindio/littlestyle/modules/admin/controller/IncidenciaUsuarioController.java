package co.edu.uniquindio.littlestyle.modules.admin.controller;

import co.edu.uniquindio.littlestyle.modules.admin.dto.CrearIncidenciaRequest;
import co.edu.uniquindio.littlestyle.modules.admin.dto.IncidenciaResponse;
import co.edu.uniquindio.littlestyle.modules.admin.service.IncidenciaService;
import co.edu.uniquindio.littlestyle.shared.dto.PaginaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Creacion y seguimiento de incidencias propias para clientes y vendedores (US-14, RNF-17).
 */
@RestController
@RequestMapping({"/api/cliente/incidencias", "/api/vendedor/incidencias"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CLIENTE', 'VENDEDOR')")
public class IncidenciaUsuarioController {

    private final IncidenciaService incidenciaService;

    /** Registra soporte, apelacion de producto o reporte de producto. */
    @PostMapping
    public ResponseEntity<IncidenciaResponse> crear(@Valid @RequestBody CrearIncidenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidenciaService.crear(request));
    }

    /** Lista las incidencias del usuario autenticado, sin exponer las de otras cuentas. */
    @GetMapping
    public PaginaResponse<IncidenciaResponse> listarPropias(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {
        return incidenciaService.listarPropias(pagina, tamano);
    }
}
