package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.controller;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaVitrinaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service.CatalogoClienteService;
import co.edu.uniquindio.littlestyle.modules.dashboard.service.TrackingAnalyticsService;
import co.edu.uniquindio.littlestyle.shared.dto.PaginaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Vitrina del cliente autenticado (base de US-09). El prefijo /api/cliente
 * exige rol CLIENTE; el controlador solo orquesta, la lógica está en el servicio.
 */
@RestController
@RequestMapping("/api/cliente/catalogo")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
public class CatalogoClienteController {

    private final CatalogoClienteService catalogo;
    private final TrackingAnalyticsService tracking;

    /**
     * Busca prendas publicadas con filtros opcionales (texto, categoría, género, precio, disponibilidad,
     * material, talla, edad o solo la talla sugerida para un perfil), paginación y orden. La cabecera {@code
     * X-Catalogo-Ms} informa el tiempo de la consulta.
     */
    @GetMapping
    public ResponseEntity<PaginaResponse<PrendaVitrinaResponse>> explorar(
            @AuthenticationPrincipal AuthenticatedUser usuario,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            @RequestParam(name = "q", required = false) String texto,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String precioMin,
            @RequestParam(required = false) String precioMax,
            @RequestParam(required = false) String disponible,
            @RequestParam(required = false) String material,
            @RequestParam(required = false) String talla,
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(required = false) Long perfilId,
            @RequestParam(required = false) Boolean soloMiTalla,
            @RequestParam(required = false) Integer edadMeses,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamano,
            @RequestParam(defaultValue = "novedades") String orden) {
        long ini = System.nanoTime();
        PaginaResponse<PrendaVitrinaResponse> res = catalogo.explorar(texto, categoria, genero, precioMin,
                precioMax, disponible, material, talla, ids, perfilId, soloMiTalla, edadMeses, pagina, tamano, orden);
        long ms = (System.nanoTime() - ini) / 1_000_000;
        // Solo la primera página: paginar la misma búsqueda no es una búsqueda nueva (métrica US-15).
        if (pagina == 0 && texto != null && !texto.isBlank()) {
            tracking.registrarBusqueda(texto, res.totalElementos(), idDe(usuario), userAgent);
        }
        return ResponseEntity.ok().header("X-Catalogo-Ms", String.valueOf(ms)).body(res);
    }

    /** Devuelve la ficha de una prenda publicada. */
    @GetMapping("/{id}")
    public ResponseEntity<PrendaDetalleResponse> detalle(
            @AuthenticationPrincipal AuthenticatedUser usuario,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            @PathVariable Long id) {
        PrendaDetalleResponse ficha = catalogo.detalle(id);
        tracking.registrarVista(id, idDe(usuario), userAgent); // solo si la prenda existe y está publicada (detalle lanza 404 si no)
        return ResponseEntity.ok(ficha);
    }

    private static Long idDe(AuthenticatedUser usuario) {
        return usuario == null ? null : usuario.id();
    }
}
