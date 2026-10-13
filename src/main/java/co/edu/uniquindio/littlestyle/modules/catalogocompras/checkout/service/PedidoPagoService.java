package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.config.CheckoutProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.LineaDisponibilidadPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service.AlertasInventarioService;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class PedidoPagoService {

    private final PedidoRepository pedidoRepository;
    private final PrendaRepository prendaRepository;
    private final AlertasInventarioService alertasInventarioService;
    private final CheckoutProperties checkoutProperties;
    private final Clock clock;

    @Transactional
    public ResultadoConfirmacionPago confirmarPago(Long pedidoId) {
        Pedido pedido = pedidoRepository.bloquearPorId(pedidoId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "El pedido no está pendiente de pago");
        }

        if (pedidoVencido(pedido)) {
            pedido.transicionarA(EstadoPedido.CANCELADO);
            return new ResultadoConfirmacionPago(ResultadoConfirmacionPago.Estado.PEDIDO_VENCIDO, List.of());
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
            pedido.transicionarA(EstadoPedido.CANCELADO);
            return new ResultadoConfirmacionPago(
                    ResultadoConfirmacionPago.Estado.STOCK_INSUFICIENTE, agotadas);
        }

        cantidades.forEach((clave, cantidad) -> {
            TallaPrenda talla = tallas.get(clave);
            talla.setStock(talla.getStock() - cantidad);
        });
        pedido.transicionarA(EstadoPedido.PAGADO);
        prendas.values().forEach(alertasInventarioService::evaluar);
        return new ResultadoConfirmacionPago(ResultadoConfirmacionPago.Estado.CONFIRMADO, List.of());
    }

    private boolean pedidoVencido(Pedido pedido) {
        LocalDateTime limite = LocalDateTime.now(clock)
                .minus(checkoutProperties.tiempoExpiracionPedido());
        return !pedido.getFechaCreacion().isAfter(limite);
    }

    private Map<ClaveStock, Integer> agruparItems(Pedido pedido) {
        Map<ClaveStock, Integer> cantidades = new HashMap<>();
        for (LineaPedido item : pedido.getItems()) {
            String tallaNormalizada = item.getTalla().trim().toUpperCase(Locale.ROOT);
            cantidades.merge(new ClaveStock(item.getPrendaId(), tallaNormalizada),
                    item.getCantidad(), Math::addExact);
        }
        return cantidades;
    }

    private Map<Long, Prenda> bloquearPrendas(Long pedidoId, Map<ClaveStock, Integer> cantidades) {
        Map<Long, Prenda> prendas = new HashMap<>();
        cantidades.keySet().stream().map(ClaveStock::prendaId).distinct().sorted()
                .forEach(prendaId -> prendas.put(prendaId, prendaRepository.bloquearPorId(prendaId)
                        .orElseThrow(() -> new IllegalStateException(
                                "No se encontró la prenda " + prendaId + " del pedido " + pedidoId))));
        return prendas;
    }

    private record ClaveStock(Long prendaId, String talla) {
    }
}
