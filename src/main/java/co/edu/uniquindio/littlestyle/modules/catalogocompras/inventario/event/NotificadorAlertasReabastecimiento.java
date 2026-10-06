package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observador de las alertas de reabastecimiento. Por ahora deja constancia en el log; el envío del correo
 * al vendedor (ADR-07) se conectará aquí cuando exista el servicio de notificaciones, sin tocar el inventario.
 */
@Slf4j
@Component
public class NotificadorAlertasReabastecimiento {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alEmitirAlerta(AlertaReabastecimientoEmitida evento) {
        log.info("Alerta de reabastecimiento {} emitida: vendedorId={} prendaId={} stock={} mínimo={} alertaId={}",
                evento.nivel(), evento.vendedorId(), evento.prendaId(), evento.stockActual(),
                evento.stockMinimo(), evento.alertaId());
    }
}
