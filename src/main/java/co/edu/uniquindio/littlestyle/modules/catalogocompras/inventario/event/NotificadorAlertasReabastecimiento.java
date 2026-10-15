package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.event;

import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.shared.correo.CorreoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observador de las alertas de reabastecimiento (US-08, ADR-07): deja
 * constancia en el log y avisa al vendedor por correo con el nivel, el
 * stock actual y el mínimo configurado. El correo sale solo si la
 * transacción que cambió el stock se confirma; si el correo falla, la
 * alerta ya quedó registrada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificadorAlertasReabastecimiento {

    private final UsuarioRepository usuarios;
    private final CorreoService correo;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alEmitirAlerta(AlertaReabastecimientoEmitida evento) {
        log.info("Alerta de reabastecimiento {} emitida: vendedorId={} prendaId={} stock={} mínimo={} alertaId={}",
                evento.nivel(), evento.vendedorId(), evento.prendaId(), evento.stockActual(),
                evento.stockMinimo(), evento.alertaId());
        usuarios.findById(evento.vendedorId()).ifPresentOrElse(
                vendedor -> correo.enviarAlertaStock(
                        vendedor.getEmail(),
                        vendedor.getNombreTienda() == null ? vendedor.getNombre() : vendedor.getNombreTienda(),
                        evento.nombrePrenda(),
                        evento.nivel().name(),
                        evento.stockActual(),
                        evento.stockMinimo()),
                () -> log.warn("Vendedor {} no encontrado; se omite el correo de alerta {}", evento.vendedorId(),
                        evento.alertaId()));
    }
}
