package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto.ActualizarStockRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto.AlertaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto.InventarioItemResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service.InventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Inventario y alertas de reabastecimiento del vendedor autenticado (UI-21 y UI-22). */
@RestController
@RequestMapping("/api/vendedor/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

    @GetMapping
    public ResponseEntity<List<InventarioItemResponse>> listar() {
        return ResponseEntity.ok(inventarioService.listar());
    }

    @PutMapping("/{prendaId}")
    public ResponseEntity<InventarioItemResponse> actualizarStock(@PathVariable Long prendaId,
                                                                  @Valid @RequestBody ActualizarStockRequest request) {
        return ResponseEntity.ok(inventarioService.actualizarStock(prendaId, request));
    }

    @GetMapping("/alertas")
    public ResponseEntity<List<AlertaResponse>> alertas() {
        return ResponseEntity.ok(inventarioService.alertasActivas());
    }
}
