package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.LineaDisponibilidadPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEstadoCambiadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoPagadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.HistorialEstadoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service.AlertasInventarioService;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

/** Confirma el pago de un pedido descontando el stock de forma atómica. */
@Slf4j
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
        Map<Long, Prenda> prendas = bloquearPrendas(cantidades);

        Map<ClaveStock, TallaPrenda> tallas = new HashMap<>();
        List<LineaDisponibilidadPagoResponse> agotadas = new ArrayList<>();
        for (Map.Entry<ClaveStock, Integer> entry : cantidades.entrySet()) {
            ClaveStock clave = entry.getKey();
            Prenda prenda = prendas.get(clave.prendaId());
            if (prenda == null) {
                // La prenda ya no existe (p. ej. el vendedor eliminó su cuenta): se trata como agotada,
                // el pedido se cancela y el cobro queda para reembolso
                log.warn("Pedido {}: la prenda {} ya no existe; se cancela y el cobro queda para reembolso",
                        pedidoId, clave.prendaId());
                agotadas.add(new LineaDisponibilidadPagoResponse(clave.prendaId(),
                        nombreDe(pedido, clave.prendaId()), clave.talla(), entry.getValue(), 0));
                continue;
            }
            TallaPrenda talla = prenda.buscarVariante(clave.talla(), clave.color()).orElse(null);
            int stockDisponible = talla == null ? 0 : talla.getStock();
            if (entry.getValue() > stockDisponible) {
                agotadas.add(new LineaDisponibilidadPagoResponse(prenda.getId(), prenda.getNombre(),
                        clave.talla(), entry.getValue(), stockDisponible, clave.color()));
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
            cantidades.merge(new ClaveStock(item.getPrendaId(), tallaNormalizada, item.getColor()),
                    item.getCantidad(), Math::addExact);
        }
        return cantidades;
    }

    /**
     * Bloquea, en orden de id (evita bloqueos cruzados), las prendas del pedido que aún existen. Las que
     * ya no existen no se incluyen: el llamador las trata como agotadas.
     */
    private Map<Long, Prenda> bloquearPrendas(Map<ClaveStock, Integer> cantidades) {
        Map<Long, Prenda> prendas = new HashMap<>();
        cantidades.keySet().stream().map(ClaveStock::prendaId).distinct().sorted()
                .forEach(prendaId -> prendaRepository.bloquearPorId(prendaId)
                        .ifPresent(prenda -> prendas.put(prendaId, prenda)));
        return prendas;
    }

    /** Nombre que tenía la prenda al comprar (snapshot de la línea del pedido). */
    private static String nombreDe(Pedido pedido, Long prendaId) {
        return pedido.getItems().stream()
                .filter(linea -> prendaId.equals(linea.getPrendaId()))
                .map(LineaPedido::getNombre)
                .findFirst().orElse("Prenda no disponible");
    }

    /** Identifica una talla de una prenda. */
    private record ClaveStock(Long prendaId, String talla, ColorPreferido color) { }
}
