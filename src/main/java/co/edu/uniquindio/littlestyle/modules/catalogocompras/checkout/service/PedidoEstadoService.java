package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEnviadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEstadoCambiadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.HistorialEstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.HistorialEstadoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PedidoEstadoService {

    private final PedidoRepository pedidoRepository;
    private final HistorialEstadoRepository historialRepository;
    private final ApplicationEventPublisher eventos;

    @Transactional
    public Pedido avanzar(Long pedidoId, EstadoPedido nuevo, String numeroGuia) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        EstadoPedido anterior = pedido.getEstado();
        pedido.avanzarA(nuevo, numeroGuia);
        historialRepository.save(HistorialEstadoPedido.builder()
                .pedido(pedido)
                .anterior(anterior)
                .nuevo(nuevo)
                .numeroGuia(nuevo == EstadoPedido.ENVIADO ? pedido.getNumeroGuia() : null)
                .fechaCambio(LocalDateTime.now())
                .build());
        if (nuevo == EstadoPedido.ENVIADO) {
            eventos.publishEvent(new PedidoEnviadoEvent(pedidoId));
        }
        eventos.publishEvent(new PedidoEstadoCambiadoEvent(pedidoId, nuevo));
        return pedido;
    }
}