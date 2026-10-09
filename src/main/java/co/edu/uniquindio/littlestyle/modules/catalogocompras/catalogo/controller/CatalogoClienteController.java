package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaVitrinaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service.CatalogoClienteService;
import co.edu.uniquindio.littlestyle.shared.dto.PaginaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping
    public ResponseEntity<PaginaResponse<PrendaVitrinaResponse>> explorar(
            @RequestParam(name = "q", required = false) String texto,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String precioMin,
            @RequestParam(required = false) String precioMax,
            @RequestParam(required = false) String disponible,
            @RequestParam(required = false) String material,
            @RequestParam(required = false) String talla,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamano,
            @RequestParam(defaultValue = "novedades") String orden) {
        return ResponseEntity.ok(catalogo.explorar(texto, categoria, genero, precioMin, precioMax,
                disponible, material, talla, pagina, tamano, orden));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrendaDetalleResponse> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(catalogo.detalle(id));
    }
}
