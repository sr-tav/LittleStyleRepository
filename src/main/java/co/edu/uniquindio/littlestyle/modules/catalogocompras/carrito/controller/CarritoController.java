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

/**
 * Carrito de compras del cliente autenticado ({@code /api/cliente/carrito}). Cada operación responde con el
 * carrito actualizado.
 */
@RestController
@RequestMapping("/api/cliente/carrito")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
public class CarritoController {

    private final CarritoService carritoService;

    /** Devuelve el carrito con subtotales y la disponibilidad actual de cada línea. */
    @GetMapping
    public ResponseEntity<CarritoResponse> obtener() {
        return ResponseEntity.ok(carritoService.obtener());
    }

    /** Agrega una prenda y talla, o suma la cantidad si ya estaba en el carrito. */
    @PostMapping("/items")
    public ResponseEntity<CarritoResponse> agregar(@Valid @RequestBody AgregarItemCarritoRequest request) {
        return ResponseEntity.ok(carritoService.agregar(request));
    }

    /** Fija la cantidad de una línea del carrito. */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CarritoResponse> cambiarCantidad(
            @PathVariable Long itemId,
            @Valid @RequestBody CambiarCantidadCarritoRequest request) {
        return ResponseEntity.ok(carritoService.cambiarCantidad(itemId, request));
    }

    /** Quita una línea del carrito. */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CarritoResponse> quitar(@PathVariable Long itemId) {
        return ResponseEntity.ok(carritoService.quitar(itemId));
    }

    /** Quita todas las líneas del carrito. */
    @DeleteMapping
    public ResponseEntity<CarritoResponse> vaciar() {
        return ResponseEntity.ok(carritoService.vaciar());
    }
}
