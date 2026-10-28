package co.edu.uniquindio.littlestyle.modules.admin.controller;

import co.edu.uniquindio.littlestyle.modules.admin.dto.PrendaAdminDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.admin.dto.PrendaAdminResponse;
import co.edu.uniquindio.littlestyle.modules.admin.dto.RechazoPrendaRequest;
import co.edu.uniquindio.littlestyle.modules.admin.service.ModeracionProductoService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
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
 * Moderacion reactcia de productos (US-14): el admin ve todas las prendas del catalogo, revisa el
 * detalle completo y decide aprobar, rechazar (con motivo) o restaurar.
 */
@RestController
@RequestMapping({"/api/admin/moderacion/prendas", "/api/admin/moderacion"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ModeracionProductoAdminController {

    private final ModeracionProductoService service;

    /** Bandeja de prendas con filtros opcionales (vendedor, estado comercial, estado de moderacion, texto). */
    @GetMapping
    public PaginaResponse<PrendaAdminResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            @RequestParam(required = false) String vendedorEmail,
            @RequestParam(required = false) EstadoPrenda estadoPrenda,
            @RequestParam(required = false) EstadoModeracion estadoModeracion,
            @RequestParam(required = false) String texto) {
        return service.listar(vendedorEmail, estadoPrenda, estadoModeracion, texto, pagina, tamano);
    }

    /** Detalle completo de una prenda, con composicion, tallas e imagenes. */
    @GetMapping("/{id}")
    public PrendaAdminDetalleResponse obtenerDetalle(@PathVariable Long id) {
        return service.obtenerDetalle(id);
    }

    /** Aprueba una prenda pendiente o rechazada; la deja publicable. */
    @PostMapping("/{id}/aprobar")
    public PrendaAdminResponse aprobar(@PathVariable Long id) {
        return service.aprobar(id);
    }

    /** Restaura una prenda rechazada para que vuelva a publicarse. */
    @PostMapping("/{id}/restaurar")
    public PrendaAdminResponse restaurar(@PathVariable Long id) {
        return service.restaurar(id);
    }

    /** Retira una prenda de la vitrina con motivo obligatorio (queda visible solo para su vendedor). */
    @PostMapping({"/{id}/desactivar", "/{id}/rechazar"})
    public PrendaAdminResponse rechazar(@PathVariable Long id,
                                        @Valid @RequestBody RechazoPrendaRequest request) {
        return service.rechazar(id, request.motivo());
    }
}
