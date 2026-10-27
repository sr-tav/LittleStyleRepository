package co.edu.uniquindio.littlestyle.modules.admin.controller;

import co.edu.uniquindio.littlestyle.modules.admin.dto.CambioEstadoCuentaRequest;
import co.edu.uniquindio.littlestyle.modules.admin.dto.UsuarioAdminResponse;
import co.edu.uniquindio.littlestyle.modules.admin.service.AdminUsuarioService;
import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.shared.dto.PaginaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bandeja de gestion de cuentas del administrador (US-14): lista, detalle y cambio de estado
 * (habilitar/suspender/bloquear) con auditoria y aviso al usuario.
 */
@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioAdminController {

    private final AdminUsuarioService service;

    /** Resulta la bandeja paginada de cuentas: filtros por rol, estado y texto libre (nombre, email, tienda). */
    @GetMapping
    public PaginaResponse<UsuarioAdminResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) EstadoUsuario estado,
            @RequestParam(required = false) String texto) {
        return service.listar(rol, estado, texto,pagina, tamano);
    }

    /** Devuelve el detalle de una cuenta. */
    @GetMapping("/{id}")
    public UsuarioAdminResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** Cambia el estado de la cuenta (ACTIVO, SUSPENDIDO, BLOQUEADO) con motivo obligatorio. */
    @PutMapping("/{id}/estado")
    public UsuarioAdminResponse cambiarEstado(@PathVariable Long id,
                                              @Valid @RequestBody CambioEstadoCuentaRequest request) {
        return service.cambiarEstado(id, request.estado(), request.motivo());
    }
}
