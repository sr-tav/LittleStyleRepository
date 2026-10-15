package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoCreadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEnviadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEstadoCambiadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoPagadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.shared.correo.CorreoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Al confirmarse el envío, avisa al cliente por correo con el número de guía.
 * Solo corre si la transacción se confirmó; si el correo falla, el pedido
 * ya quedó en ENVIADO y el error solo queda en el log.
 */
@Component
@RequiredArgsConstructor
public class NotificacionPedidoListener {

    private final PedidoRepository pedidoRepository;
    private final CorreoService correo;

    /**
     * AFTER_COMMIT corre fuera de la transacción que confirmó el envío; con
     * open-in-view=false los LAZY (items, cliente) fallarían sin transacción
     * propia y el correo se perdería (RNF-23). Por eso este método abre su
     * propia transacción de lectura.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alEnviarse(PedidoEnviadoEvent evento) {
        pedidoRepository.findById(evento.pedidoId()).ifPresent(pedido -> {
            List<CorreoService.ItemCorreo> items = pedido.getItems().stream()
                    .map(i -> new CorreoService.ItemCorreo(i.getNombre(), i.getTalla(), i.getCantidad()))
                    .toList();
            correo.enviarPedidoEnviado(
                    pedido.getCliente().getEmail(),
                    pedido.getCliente().getNombre(),
                    pedido.getId(),
                    pedido.getNumeroGuia(),
                    pedido.getTotal().toPlainString(),
                    items);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCrearse(PedidoCreadoEvent evento) {
        pedidoRepository.findById(evento.pedidoId()).ifPresent(pedido -> correo.enviarPedidoCreado(
                pedido.getCliente().getEmail(),
                pedido.getCliente().getNombre(),
                pedido.getId(),
                pedido.getTotal().toPlainString(),
                itemsDe(pedido)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alPagarse(PedidoPagadoEvent evento) {
        pedidoRepository.findById(evento.pedidoId()).ifPresent(pedido -> correo.enviarPagoConfirmado(
                pedido.getCliente().getEmail(),
                pedido.getCliente().getNombre(),
                pedido.getId(),
                pedido.getTotal().toPlainString(),
                itemsDe(pedido)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCambiarEstado(PedidoEstadoCambiadoEvent evento) {
        if (evento.nuevo() == EstadoPedido.ENVIADO) {
            return; // ya tiene correo propio con guía
        }
        pedidoRepository.findById(evento.pedidoId()).ifPresent(pedido -> correo.enviarCambioEstado(
                pedido.getCliente().getEmail(),
                pedido.getCliente().getNombre(),
                pedido.getId(),
                evento.nuevo().name(),
                pedido.getTotal().toPlainString()));
    }

    private static List<CorreoService.ItemCorreo> itemsDe(
            co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido pedido) {
        return pedido.getItems().stream()
                .map(i -> new CorreoService.ItemCorreo(i.getNombre(), i.getTalla(), i.getCantidad()))
                .toList();
    }
}
