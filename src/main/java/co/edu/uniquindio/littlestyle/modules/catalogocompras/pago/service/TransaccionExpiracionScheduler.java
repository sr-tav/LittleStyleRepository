package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Red de seguridad para las compras a medio hacer: cada intervalo cierra las transacciones que
 * superaron la antigüedad máxima. El cliente puede liberarlas antes con
 * {@code DELETE /api/cliente/pagos/{referencia}}; este proceso solo cubre las que se quedaron.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransaccionExpiracionScheduler {

    private final TransaccionExpiracionService transaccionExpiracionService;

    @Scheduled(fixedDelayString = "${app.pagos.expiracion-intervalo-ms:60000}")
    public void expirarTransacciones() {
        try {
            transaccionExpiracionService.expirarTransaccionesVencidas();
        } catch (RuntimeException error) {
            // Una falla aquí no debe matar el hilo programado: se registra y el próximo ciclo reintenta.
            log.error("No fue posible expirar las transacciones de pago vencidas: {}", error.getMessage());
        }
    }
}