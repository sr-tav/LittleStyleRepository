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

/** Perfiles infantiles del cliente autenticado y su historial de mediciones (US-06). */
@RestController
@RequestMapping("/api/cliente/perfiles")
@RequiredArgsConstructor
public class PerfilInfantilController {

    private final PerfilInfantilService perfilService;

    /** Lista los perfiles del cliente, del modificado más recientemente al más antiguo. */
    @GetMapping
    public ResponseEntity<List<PerfilResponse>> listar() {
        return ResponseEntity.ok(perfilService.listar());
    }

    /** Devuelve un perfil propio con su última medición y su completitud. */
    @GetMapping("/{id}")
    public ResponseEntity<PerfilResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(perfilService.obtener(id));
    }

    /** Crea un perfil sin mediciones. Responde 201. */
    @PostMapping
    public ResponseEntity<PerfilResponse> crear(@Valid @RequestBody PerfilRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(perfilService.crear(request));
    }

    /** Crea un perfil junto con su primera medición en una sola operación. Responde 201. */
    @PostMapping("/con-medicion-inicial")
    public ResponseEntity<PerfilResponse> crearConMedicionInicial(
            @Valid @RequestBody CrearPerfilConMedicionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(perfilService.crearConMedicionInicial(request));
    }

    /** Actualiza los datos de un perfil propio. */
    @PutMapping("/{id}")
    public ResponseEntity<PerfilResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody PerfilRequest request) {
        return ResponseEntity.ok(perfilService.actualizar(id, request));
    }

    /** Elimina un perfil propio con sus mediciones. Responde 204. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        perfilService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Registra una nueva medición de crecimiento. Responde 201. */
    @PostMapping("/{id}/mediciones")
    public ResponseEntity<MedicionResponse> crearMedicion(@PathVariable Long id,
                                                          @Valid @RequestBody MedicionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(perfilService.crearMedicion(id, request));
    }

    /** Corrige una medición existente del perfil. */
    @PutMapping("/{id}/mediciones/{medicionId}")
    public ResponseEntity<MedicionResponse> actualizarMedicion(@PathVariable Long id,
                                                               @PathVariable Long medicionId,
                                                               @Valid @RequestBody MedicionRequest request) {
        return ResponseEntity.ok(perfilService.actualizarMedicion(id, medicionId, request));
    }

    /** Historial de mediciones del perfil, de la más reciente a la más antigua. */
    @GetMapping("/{id}/mediciones")
    public ResponseEntity<List<MedicionResponse>> listarMediciones(@PathVariable Long id) {
        return ResponseEntity.ok(perfilService.listarMediciones(id));
    }
}
