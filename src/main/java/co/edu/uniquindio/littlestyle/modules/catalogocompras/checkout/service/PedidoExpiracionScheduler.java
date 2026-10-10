package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PedidoExpiracionScheduler {

    private final PedidoExpiracionService pedidoExpiracionService;

    @Scheduled(fixedDelayString = "${app.checkout.expiracion-intervalo-ms:60000}")
    public void cancelarPedidosVencidos() {
        pedidoExpiracionService.cancelarPedidosVencidos();
    }
}
