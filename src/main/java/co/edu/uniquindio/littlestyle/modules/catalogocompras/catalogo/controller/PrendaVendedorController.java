package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.EstadoPrendaRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service.ImagenPrendaService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service.PrendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Catálogo del vendedor autenticado (UI-19 y UI-20). El prefijo /api/vendedor exige rol VENDEDOR. */
@RestController
@RequestMapping("/api/vendedor/prendas")
@RequiredArgsConstructor
public class PrendaVendedorController {

    private final PrendaService prendaService;
    private final ImagenPrendaService imagenService;

    @GetMapping
    public ResponseEntity<List<PrendaResponse>> listar(@RequestParam(name = "q", required = false) String busqueda) {
        return ResponseEntity.ok(prendaService.listar(busqueda));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrendaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(prendaService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<PrendaResponse> crear(@Valid @RequestBody PrendaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prendaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrendaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody PrendaRequest request) {
        return ResponseEntity.ok(prendaService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<PrendaResponse> cambiarEstado(@PathVariable Long id,
                                                        @Valid @RequestBody EstadoPrendaRequest request) {
        return ResponseEntity.ok(prendaService.cambiarEstado(id, request.estado()));
    }

    /** Carga masiva: varios archivos en el campo multipart {@code archivos}. */
    @PostMapping(path = "/{id}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<PrendaResponse.Imagen>> subirImagenes(
            @PathVariable Long id, @RequestPart("archivos") List<MultipartFile> archivos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(imagenService.subir(id, archivos));
    }

    @PutMapping("/{id}/imagenes/{imagenId}/principal")
    public ResponseEntity<List<PrendaResponse.Imagen>> marcarPrincipal(@PathVariable Long id,
                                                                       @PathVariable Long imagenId) {
        return ResponseEntity.ok(imagenService.marcarPrincipal(id, imagenId));
    }

    @DeleteMapping("/{id}/imagenes/{imagenId}")
    public ResponseEntity<Void> eliminarImagen(@PathVariable Long id, @PathVariable Long imagenId) {
        imagenService.eliminar(id, imagenId);
        return ResponseEntity.noContent().build();
    }
}
