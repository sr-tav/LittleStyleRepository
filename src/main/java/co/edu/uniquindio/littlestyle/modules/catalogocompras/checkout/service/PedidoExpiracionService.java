package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.config.CheckoutProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PedidoExpiracionService {

    private final PedidoRepository pedidoRepository;
    private final CheckoutProperties checkoutProperties;
    private final Clock clock;

    @Transactional
    public int cancelarPedidosVencidos() {
        LocalDateTime limite = LocalDateTime.now(clock)
                .minus(checkoutProperties.tiempoExpiracionPedido());
        int pedidosCancelados = 0;

        for (Long pedidoId : pedidoRepository.buscarIdsVencidos(EstadoPedido.PENDIENTE_PAGO, limite)) {
            Pedido pedido = pedidoRepository.bloquearPorId(pedidoId)
                    .orElseThrow(() -> new IllegalStateException("No se encontró el pedido " + pedidoId));
            if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO
                    || pedido.getFechaCreacion().isAfter(limite)) {
                continue;
            }

            pedido.transicionarA(EstadoPedido.CANCELADO);
            pedidosCancelados++;
        }

        return pedidosCancelados;
    }
}
