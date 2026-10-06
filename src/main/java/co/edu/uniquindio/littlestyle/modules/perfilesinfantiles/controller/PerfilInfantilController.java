package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.controller;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto.CrearPerfilConMedicionRequest;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto.MedicionRequest;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto.MedicionResponse;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto.PerfilRequest;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto.PerfilResponse;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.service.PerfilInfantilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cliente/perfiles")
@RequiredArgsConstructor
public class PerfilInfantilController {

    private final PerfilInfantilService perfilService;

    @GetMapping
    public ResponseEntity<List<PerfilResponse>> listar() {
        return ResponseEntity.ok(perfilService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PerfilResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(perfilService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<PerfilResponse> crear(@Valid @RequestBody PerfilRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(perfilService.crear(request));
    }

    @PostMapping("/con-medicion-inicial")
    public ResponseEntity<PerfilResponse> crearConMedicionInicial(
            @Valid @RequestBody CrearPerfilConMedicionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(perfilService.crearConMedicionInicial(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PerfilResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody PerfilRequest request) {
        return ResponseEntity.ok(perfilService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        perfilService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/mediciones")
    public ResponseEntity<MedicionResponse> crearMedicion(@PathVariable Long id,
                                                          @Valid @RequestBody MedicionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(perfilService.crearMedicion(id, request));
    }

    @PutMapping("/{id}/mediciones/{medicionId}")
    public ResponseEntity<MedicionResponse> actualizarMedicion(@PathVariable Long id,
                                                               @PathVariable Long medicionId,
                                                               @Valid @RequestBody MedicionRequest request) {
        return ResponseEntity.ok(perfilService.actualizarMedicion(id, medicionId, request));
    }

    @GetMapping("/{id}/mediciones")
    public ResponseEntity<List<MedicionResponse>> listarMediciones(@PathVariable Long id) {
        return ResponseEntity.ok(perfilService.listarMediciones(id));
    }
}
