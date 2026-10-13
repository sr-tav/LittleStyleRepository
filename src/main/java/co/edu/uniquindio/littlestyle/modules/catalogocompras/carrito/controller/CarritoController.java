package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.AgregarItemCarritoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.CarritoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.CambiarCantidadCarritoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service.CarritoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cliente/carrito")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
public class CarritoController {

    private final CarritoService carritoService;

    @GetMapping
    public ResponseEntity<CarritoResponse> obtener() {
        return ResponseEntity.ok(carritoService.obtener());
    }

    @PostMapping("/items")
    public ResponseEntity<CarritoResponse> agregar(@Valid @RequestBody AgregarItemCarritoRequest request) {
        return ResponseEntity.ok(carritoService.agregar(request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CarritoResponse> cambiarCantidad(
            @PathVariable Long itemId,
            @Valid @RequestBody CambiarCantidadCarritoRequest request) {
        return ResponseEntity.ok(carritoService.cambiarCantidad(itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CarritoResponse> quitar(@PathVariable Long itemId) {
        return ResponseEntity.ok(carritoService.quitar(itemId));
    }

    @DeleteMapping
    public ResponseEntity<CarritoResponse> vaciar() {
        return ResponseEntity.ok(carritoService.vaciar());
    }
}
