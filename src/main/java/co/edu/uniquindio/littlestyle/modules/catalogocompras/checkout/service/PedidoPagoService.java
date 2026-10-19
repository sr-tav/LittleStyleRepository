package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.LineaDisponibilidadPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEstadoCambiadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoPagadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.HistorialEstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.HistorialEstadoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service.AlertasInventarioService;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Confirma el pago de un pedido descontando el stock de forma atómica. */
@Service
@RequiredArgsConstructor
public class PedidoPagoService {

    private final PedidoRepository pedidoRepository;
    private final HistorialEstadoRepository historialRepository;
    private final PrendaRepository prendaRepository;
    private final AlertasInventarioService alertasInventarioService;
    private final Clock clock;
    private final ApplicationEventPublisher eventos;
    private final DespachoService despachoService;

    /**
     * Bloquea el pedido y sus prendas y verifica el stock de cada talla. Si alcanza, lo descuenta, deja el
     * pedido {@code REGISTRADO}, crea un despacho por vendedor y evalúa alertas; si no, cancela el pedido y
     * devuelve las prendas agotadas.
     */
    @Transactional
    public ResultadoConfirmacionPago confirmarPago(Long pedidoId) {
        Pedido pedido = pedidoRepository.bloquearPorId(pedidoId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "El pedido no está pendiente de pago");
        }

        Map<ClaveStock, Integer> cantidades = agruparItems(pedido);
        Map<Long, Prenda> prendas = bloquearPrendas(pedidoId, cantidades);

        Map<ClaveStock, TallaPrenda> tallas = new HashMap<>();
        List<LineaDisponibilidadPagoResponse> agotadas = new ArrayList<>();
        for (Map.Entry<ClaveStock, Integer> entry : cantidades.entrySet()) {
            ClaveStock clave = entry.getKey();
            Prenda prenda = prendas.get(clave.prendaId());
            TallaPrenda talla = prenda.buscarTalla(clave.talla()).orElse(null);
            int stockDisponible = talla == null ? 0 : talla.getStock();
            if (entry.getValue() > stockDisponible) {
                agotadas.add(new LineaDisponibilidadPagoResponse(prenda.getId(), prenda.getNombre(),
                        clave.talla(), entry.getValue(), stockDisponible));
            } else {
                tallas.put(clave, talla);
            }
        }
        if (!agotadas.isEmpty()) {
            transicionarConHistorial(pedido, EstadoPedido.CANCELADO);
            return new ResultadoConfirmacionPago(
                    ResultadoConfirmacionPago.Estado.STOCK_INSUFICIENTE, agotadas);
        }

        cantidades.forEach((clave, cantidad) -> {
            TallaPrenda talla = tallas.get(clave);
            talla.setStock(talla.getStock() - cantidad);
        });
        transicionarConHistorial(pedido, EstadoPedido.PAGADO);
        // PAGADO es transitorio: el pedido queda REGISTRADO y listo para
        // preparar en la misma transacción (dos filas de historial).
        transicionarConHistorial(pedido, EstadoPedido.REGISTRADO);
        // Multi-vendedor: agrupa líneas por vendedor y crea un despacho en
        // REGISTRADO por cada uno, en la misma transacción del pago.
        despachoService.crearDespachosAlPagar(pedido.getId());
        prendas.values().forEach(alertasInventarioService::evaluar);
        return new ResultadoConfirmacionPago(ResultadoConfirmacionPago.Estado.CONFIRMADO, List.of());
    }

    /**
     * Cambia el estado, lo registra en el historial y publica el evento correspondiente (el pago tiene evento
     * propio).
     */
    private void transicionarConHistorial(Pedido pedido, EstadoPedido nuevo) {
        EstadoPedido anterior = pedido.getEstado();
        pedido.transicionarA(nuevo);
        historialRepository.save(HistorialEstadoPedido.builder()
                .pedido(pedido)
                .anterior(anterior)
                .nuevo(nuevo)
                .fechaCambio(LocalDateTime.now(clock))
                .build());
        if (nuevo == EstadoPedido.PAGADO) {
            // PAGADO tiene correo propio; el genérico duplicaría el aviso.
            eventos.publishEvent(new PedidoPagadoEvent(pedido.getId()));
            return;
        }
        eventos.publishEvent(new PedidoEstadoCambiadoEvent(pedido.getId(), nuevo));
    }

    /** Suma las cantidades del pedido por prenda y talla. */
    private Map<ClaveStock, Integer> agruparItems(Pedido pedido) {
        Map<ClaveStock, Integer> cantidades = new HashMap<>();
        for (LineaPedido item : pedido.getItems()) {
            String tallaNormalizada = item.getTalla().trim().toUpperCase(Locale.ROOT);
            cantidades.merge(new ClaveStock(item.getPrendaId(), tallaNormalizada),
                    item.getCantidad(), Math::addExact);
        }
        return cantidades;
    }

    /**
     * Bloquea las prendas del pedido en orden de id, para evitar bloqueos cruzados entre pagos simultáneos.
     */
    private Map<Long, Prenda> bloquearPrendas(Long pedidoId, Map<ClaveStock, Integer> cantidades) {
        Map<Long, Prenda> prendas = new HashMap<>();
        cantidades.keySet().stream().map(ClaveStock::prendaId).distinct().sorted()
                .forEach(prendaId -> prendas.put(prendaId, prendaRepository.bloquearPorId(prendaId)
                        .orElseThrow(() -> new IllegalStateException(
                                "No se encontró la prenda " + prendaId + " del pedido " + pedidoId))));
        return prendas;
    }

    /** Identifica una talla de una prenda. */
    private record ClaveStock(Long prendaId, String talla) {
    }
}
