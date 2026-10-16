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
     * Transiciones manuales del ADMINISTRADOR. PAGADO y PENDIENTE_PAGO se
     * excluyen: marcar pagado a mano vendería sin pago ni descuento de stock.
     */
    private static final Set<EstadoPedido> MANUAL_ADMIN = EnumSet.of(
            EstadoPedido.REGISTRADO, EstadoPedido.EN_PREPARACION, EstadoPedido.ENVIADO, EstadoPedido.CANCELADO);
    /**
     * Transiciones manuales del VENDEDOR sobre sus pedidos: solo avanzar
     * (preparar y despachar). Cancelar es del administrador porque el pedido
     * tiene estado y guía únicos aunque incluya varios vendedores.
     */
    private static final Set<EstadoPedido> MANUAL_VENDEDOR = EnumSet.of(
            EstadoPedido.EN_PREPARACION, EstadoPedido.ENVIADO);
    /** Cancelar repone stock solo si ya se había descontado (hubo pago). */
    private static final Set<EstadoPedido> CON_STOCK_DESCONTADO = EnumSet.of(
            EstadoPedido.PAGADO, EstadoPedido.REGISTRADO, EstadoPedido.EN_PREPARACION);

    private final PedidoRepository pedidoRepository;
    private final HistorialEstadoRepository historialRepository;
    private final PrendaRepository prendaRepository;
    private final AlertasInventarioService alertasInventarioService;
    private final ApplicationEventPublisher eventos;

    @Transactional
    public Pedido avanzar(Long pedidoId, EstadoPedido nuevo, String numeroGuia) {
        if (nuevo == null || !MANUAL_ADMIN.contains(nuevo)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "El estado manual debe ser REGISTRADO, EN_PREPARACION, ENVIADO o CANCELADO", "nuevo");
        }
        return avanzarConBloqueo(pedidoId, nuevo, numeroGuia);
    }

    @Transactional
    public Pedido avanzarComoVendedor(Long pedidoId, EstadoPedido nuevo, String numeroGuia) {
        if (nuevo == null || !MANUAL_VENDEDOR.contains(nuevo)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "El vendedor solo puede avanzar a EN_PREPARACION o ENVIADO", "nuevo");
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
     */
    private void reponerStock(Pedido pedido) {
        for (LineaPedido linea : pedido.getItems()) {
            try {
                Prenda prenda = prendaRepository.bloquearPorId(linea.getPrendaId()).orElse(null);
                if (prenda == null) {
                    log.warn("Sin reposición: prenda {} del pedido {} ya no existe",
                            linea.getPrendaId(), pedido.getId());
                    continue;
                }
                String tallaNombre = linea.getTalla().trim().toUpperCase(Locale.ROOT);
                TallaPrenda talla = prenda.buscarTalla(tallaNombre).orElse(null);
                if (talla == null) {
                    log.warn("Sin reposición: talla {} de la prenda {} ya no existe",
                            tallaNombre, linea.getPrendaId());
                    continue;
                }
                talla.setStock(talla.getStock() + linea.getCantidad());
                alertasInventarioService.evaluar(prenda);
            } catch (RuntimeException ex) {
                log.error("Sin reposición para la línea {} del pedido {}",
                        linea.getPrendaId(), pedido.getId(), ex);
            }
        }
    }
}
