package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEnviadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEstadoCambiadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.HistorialEstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.HistorialEstadoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service.AlertasInventarioService;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoEstadoService {

    /**
     * Transiciones manuales del ADMINISTRADOR. Con despachos por vendedor nadie
     * edita el estado global a mano: el administrador solo puede CANCELAR (con
     * reposición total). El avance lo hace cada vendedor sobre su despacho y el
     * pedido madre se recalcula de forma derivada.
     */
    private static final Set<EstadoPedido> MANUAL_ADMIN = EnumSet.of(EstadoPedido.CANCELADO);
    /** Cancelar repone stock solo si ya se había descontado (hubo pago). */
    private static final Set<EstadoPedido> CON_STOCK_DESCONTADO = EnumSet.of(
            EstadoPedido.PAGADO, EstadoPedido.REGISTRADO, EstadoPedido.EN_PREPARACION,
            EstadoPedido.PARCIALMENTE_ENVIADO);

    private final PedidoRepository pedidoRepository;
    private final HistorialEstadoRepository historialRepository;
    private final PrendaRepository prendaRepository;
    private final AlertasInventarioService alertasInventarioService;
    private final co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DespachoRepository despachoRepository;
    private final ApplicationEventPublisher eventos;

    @Transactional
    public Pedido avanzar(Long pedidoId, EstadoPedido nuevo, String numeroGuia) {
        if (nuevo == null || !MANUAL_ADMIN.contains(nuevo)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "El administrador solo puede cancelar el pedido; el avance lo hace cada vendedor en su despacho",
                    "nuevo");
        }
        return avanzarConBloqueo(pedidoId, nuevo, numeroGuia);
    }

    /**
     * Cancelación del CLIENTE: solo antes del pago (Definición: retracto
     * dentro de un tiempo). Sin stock que reponer ni reembolso que tramitar.
     */
    @Transactional
    public Pedido cancelarComoCliente(Long clienteId, Long pedidoId) {
        Pedido pedido = pedidoRepository.findByIdAndClienteId(pedidoId, clienteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Solo se puede cancelar antes del pago; después gestiona la cancelación el vendedor",
                    "estado");
        }
        return avanzarConBloqueo(pedidoId, EstadoPedido.CANCELADO, null);
    }

    private Pedido avanzarConBloqueo(Long pedidoId, EstadoPedido nuevo, String numeroGuia) {
        // Bloqueo pesimista como pago y expiración: dos avances concurrentes
        // no pueden pisar el estado ni duplicar la reposición de stock.
        Pedido pedido = pedidoRepository.bloquearPorId(pedidoId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        EstadoPedido anterior = pedido.getEstado();
        if (nuevo == EstadoPedido.CANCELADO) {
            boolean parcial = anterior == EstadoPedido.PARCIALMENTE_ENVIADO
                    || despachoRepository.findAllByPedidoIdOrderByIdAsc(pedidoId).stream()
                            .anyMatch(d -> d.getEstado() == EstadoPedido.ENVIADO);
            if (parcial || anterior == EstadoPedido.ENVIADO) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "No se puede cancelar un pedido ya parcial o totalmente enviado", "estado");
            }
        }
        pedido.avanzarA(nuevo, numeroGuia);
        if (nuevo == EstadoPedido.CANCELADO && CON_STOCK_DESCONTADO.contains(anterior)) {
            reponerStock(pedido);
        }
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

    /**
     * Devuelve al inventario lo descontado al pagar. El reembolso del dinero
     * NO es automático y queda pendiente del dueño (se documenta en US-12).
     * Si una línea falla, la transacción completa falla (rollback): dejar el
     * pedido CANCELADO con stock repuesto a medias y solo un log es peor que
     * reintentar la cancelación. La discrepancia queda en el log con el error.
     */
    private void reponerStock(Pedido pedido) {
        for (LineaPedido linea : pedido.getItems()) {
            Prenda prenda = prendaRepository.bloquearPorId(linea.getPrendaId()).orElse(null);
            if (prenda == null) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "No se pudo reponer el stock: la prenda " + linea.getPrendaId()
                                + " del pedido " + pedido.getId() + " ya no existe");
            }
            String tallaNombre = linea.getTalla().trim().toUpperCase(Locale.ROOT);
            TallaPrenda talla = prenda.buscarTalla(tallaNombre).orElse(null);
            if (talla == null) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "No se pudo reponer el stock: la talla " + tallaNombre + " de la prenda "
                                + linea.getPrendaId() + " ya no existe");
            }
            talla.setStock(talla.getStock() + linea.getCantidad());
            alertasInventarioService.evaluar(prenda);
        }
    }
}
