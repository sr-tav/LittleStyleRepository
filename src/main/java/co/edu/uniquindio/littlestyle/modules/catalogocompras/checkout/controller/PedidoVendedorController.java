package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoVendedorDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoVendedorResumenResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoVendedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Pedidos del vendedor autenticado para preparar despachos. Solo lectura: los
 * cambios de estado los hace el administrador.
 */
@RestController
@RequestMapping("/api/vendedor/pedidos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDEDOR')")
public class PedidoVendedorController {

    private final PedidoVendedorService pedidos;

    @GetMapping
    public ResponseEntity<List<PedidoVendedorResumenResponse>> misPedidos() {
        return ResponseEntity.ok(pedidos.misPedidos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoVendedorDetalleResponse> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(pedidos.detalle(id));
    }
}
