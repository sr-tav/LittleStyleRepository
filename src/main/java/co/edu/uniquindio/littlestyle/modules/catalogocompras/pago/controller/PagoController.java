package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.CrearPedidoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.InicioPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.ResultadoPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.VerificarPagoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cliente/pagos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/checkout")
    public ResponseEntity<InicioPagoResponse> iniciarCheckout(
            @Valid @RequestBody CrearPedidoRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String claveIdempotencia) {
        return ResponseEntity.status(201).body(pagoService.iniciar(request, claveIdempotencia));
    }

    /** Anula un pago del cliente que aún está en CREADA y libera la compra para un nuevo intento. */
    @DeleteMapping("/{referencia}")
    public ResponseEntity<ResultadoPagoResponse> cancelar(@PathVariable String referencia) {
        return ResponseEntity.ok(pagoService.cancelar(referencia));
    }

    @PostMapping("/verificar")
    public ResponseEntity<ResultadoPagoResponse> verificar(@Valid @RequestBody VerificarPagoRequest request) {
        return ResponseEntity.ok(pagoService.verificar(request.transaccionId().trim()));
    }

    @GetMapping("/{referencia}")
    public ResponseEntity<ResultadoPagoResponse> consultar(@PathVariable String referencia) {
        return ResponseEntity.ok(pagoService.consultarPorReferencia(referencia));
    }
}
