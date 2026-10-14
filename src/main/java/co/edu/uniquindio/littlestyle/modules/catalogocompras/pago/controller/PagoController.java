package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.InicioPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.ResultadoPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.VerificarPagoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cliente/pagos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/pedidos/{pedidoId}")
    public ResponseEntity<InicioPagoResponse> iniciar(@PathVariable Long pedidoId) {
        return ResponseEntity.status(201).body(pagoService.iniciar(pedidoId));
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
