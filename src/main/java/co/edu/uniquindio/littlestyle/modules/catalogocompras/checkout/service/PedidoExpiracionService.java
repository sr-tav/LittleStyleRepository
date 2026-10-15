package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.config.CheckoutProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEstadoCambiadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.HistorialEstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.ClaveIdempotenciaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.HistorialEstadoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PedidoExpiracionService {

    private final PedidoRepository pedidoRepository;
    private final HistorialEstadoRepository historialRepository;
    private final ClaveIdempotenciaRepository clavesIdempotencia;
    private final CheckoutProperties checkoutProperties;
    private final Clock clock;
    private final ApplicationEventPublisher eventos;

    @Transactional
    public int cancelarPedidosVencidos() {
        LocalDateTime limite = LocalDateTime.now(clock)
                .minus(checkoutProperties.tiempoExpiracionPedido());
        clavesIdempotencia.eliminarVencidas(LocalDateTime.now(clock));
        int pedidosCancelados = 0;

        for (Long pedidoId : pedidoRepository.buscarIdsVencidos(EstadoPedido.PENDIENTE_PAGO, limite)) {
            Pedido pedido = pedidoRepository.bloquearPorId(pedidoId)
                    .orElseThrow(() -> new IllegalStateException("No se encontró el pedido " + pedidoId));
            if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO
                    || pedido.getFechaCreacion().isAfter(limite)) {
                continue;
            }

            pedido.transicionarA(EstadoPedido.CANCELADO);
            historialRepository.save(HistorialEstadoPedido.builder()
                    .pedido(pedido)
                    .anterior(EstadoPedido.PENDIENTE_PAGO)
                    .nuevo(EstadoPedido.CANCELADO)
                    .fechaCambio(LocalDateTime.now(clock))
                    .build());
            eventos.publishEvent(new PedidoEstadoCambiadoEvent(pedidoId, EstadoPedido.CANCELADO));
            pedidosCancelados++;
        }

        return pedidosCancelados;
    }
}
