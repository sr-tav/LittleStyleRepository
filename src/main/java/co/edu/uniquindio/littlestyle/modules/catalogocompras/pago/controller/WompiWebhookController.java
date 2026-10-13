package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.EventoWompiRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service.PagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook público de eventos de Wompi. No usa JWT: la autenticidad se garantiza con la
 * verificación del checksum SHA-256 del evento. Un 200 confirma la recepción; cualquier otro
 * código hace que Wompi reintente el envío.
 */
@RestController
@RequestMapping(WompiWebhookController.RUTA)
@RequiredArgsConstructor
public class WompiWebhookController {

    public static final String RUTA = "/api/pagos/wompi/eventos";

    private final PagoService pagoService;

    @PostMapping
    public ResponseEntity<Void> recibirEvento(@RequestBody EventoWompiRequest evento) {
        pagoService.procesarEvento(evento);
        return ResponseEntity.ok().build();
    }
}
