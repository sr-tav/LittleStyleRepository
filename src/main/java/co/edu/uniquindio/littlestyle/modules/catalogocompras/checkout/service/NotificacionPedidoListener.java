package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoCreadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEnviadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoPagoRechazadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEstadoCambiadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoPagadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.shared.correo.CorreoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Al confirmarse el envío, avisa al cliente por correo con el número de guía.
 * Solo corre si la transacción se confirmó; si el correo falla, el pedido
 * ya quedó en ENVIADO y el error solo queda en el log.
 */
@Component
@RequiredArgsConstructor
public class NotificacionPedidoListener {

    private final PedidoRepository pedidoRepository;
    private final PrendaRepository prendaRepository;
    private final co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.repository.TransaccionPagoRepository transaccionRepository;
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
        avisarVendedores(evento.pedidoId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRechazarse(PedidoPagoRechazadoEvent evento) {
        transaccionRepository.findById(evento.transaccionId()).ifPresent(transaccion -> {
            var cliente = transaccion.getCliente();
            correo.enviarPagoRechazado(
                    cliente.getEmail(),
                    cliente.getNombre(),
                    transaccion.getReferencia(),
                    String.valueOf(transaccion.getMontoCentavos()));
        });
    }

    /**
     * Definición del Proyecto §3.2.1: el vendedor debe recibir notificaciones
     * de pedidos. Al confirmarse el pago se avisa a cada vendedor con solo
     * sus líneas del pedido.
     */
    void avisarVendedores(Long pedidoId) {
        var pedidoOpt = pedidoRepository.findById(pedidoId);
        if (pedidoOpt.isEmpty()) return;
        var pedido = pedidoOpt.get();
        Map<Long, List<LineaPedido>> lineasPorVendedor = new LinkedHashMap<>();
        Map<Long, co.edu.uniquindio.littlestyle.modules.auth.model.Usuario> vendedores = new HashMap<>();
        for (LineaPedido linea : pedido.getItems()) {
            Prenda prenda = prendaRepository.findById(linea.getPrendaId()).orElse(null);
            if (prenda == null || prenda.getVendedor() == null) continue;
            Long vendedorId = prenda.getVendedor().getId();
            vendedores.putIfAbsent(vendedorId, prenda.getVendedor());
            lineasPorVendedor.computeIfAbsent(vendedorId, k -> new ArrayList<>()).add(linea);
        }
        for (var entry : lineasPorVendedor.entrySet()) {
            var vendedor = vendedores.get(entry.getKey());
            List<CorreoService.ItemCorreo> items = entry.getValue().stream()
                    .map(i -> new CorreoService.ItemCorreo(i.getNombre(), i.getTalla(), i.getCantidad()))
                    .toList();
            correo.enviarPedidoNuevoVendedor(
                    vendedor.getEmail(),
                    vendedor.getNombreTienda() != null ? vendedor.getNombreTienda() : vendedor.getNombre(),
                    pedido.getId(),
                    pedido.getCliente().getNombre(),
                    items);
        }
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
                evento.nuevo().etiqueta(),
                pedido.getTotal().toPlainString()));
    }

    private static List<CorreoService.ItemCorreo> itemsDe(
            co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido pedido) {
        return pedido.getItems().stream()
                .map(i -> new CorreoService.ItemCorreo(i.getNombre(), i.getTalla(), i.getCantidad()))
                .toList();
    }
}
