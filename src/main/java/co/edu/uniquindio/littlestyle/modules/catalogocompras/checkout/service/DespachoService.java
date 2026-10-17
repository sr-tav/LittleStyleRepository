package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.DespachoEnviadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoEstadoCambiadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Despacho;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.HistorialEstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DespachoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.HistorialEstadoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Despachos por vendedor (US-12 multi-vendedor, estilo Mercado Libre).
 * Al confirmarse el pago se agrupa por vendedor y se crea un despacho en
 * REGISTRADO por cada uno; cada vendedor avanza solo su paquete y el pedido
 * madre se recalcula de forma derivada. Nadie edita el estado global a mano.
 */
@Service
@RequiredArgsConstructor
public class DespachoService {

    private final PedidoRepository pedidoRepository;
    private final DespachoRepository despachoRepository;
    private final HistorialEstadoRepository historialRepository;
    private final PrendaRepository prendaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventos;

    /**
     * Crea un despacho en REGISTRADO por cada vendedor del pedido y asigna cada
     * línea a su despacho. Solo se invoca cuando el pedido pasa a PAGADO y
     * REGISTRADO, nunca antes. Idempotente: si ya existen, los reutiliza.
     */
    @Transactional
    public List<Despacho> crearDespachosAlPagar(Long pedidoId) {
        Pedido pedido = pedidoRepository.bloquearPorId(pedidoId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        if (pedido.getEstado() != EstadoPedido.PAGADO && pedido.getEstado() != EstadoPedido.REGISTRADO) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Los despachos solo se crean cuando el pedido está pagado y registrado");
        }
        Map<Long, List<LineaPedido>> porVendedor = agruparPorVendedor(pedido);
        List<Despacho> creados = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();
        for (Map.Entry<Long, List<LineaPedido>> entry : porVendedor.entrySet()) {
            Long vendedorId = entry.getKey();
            Despacho despacho = despachoRepository.findByPedidoIdAndVendedorId(pedidoId, vendedorId)
                    .orElseGet(() -> {
                        Usuario vendedor = usuarioRepository.findById(vendedorId)
                                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                                        "Vendedor no encontrado"));
                        Despacho nuevo = new Despacho();
                        nuevo.setPedido(pedido);
                        nuevo.setVendedor(vendedor);
                        nuevo.setEstado(EstadoPedido.REGISTRADO);
                        nuevo.setFechaActualizacion(ahora);
                        return despachoRepository.save(nuevo);
                    });
            for (LineaPedido linea : entry.getValue()) {
                linea.setDespacho(despacho);
            }
            creados.add(despacho);
        }
        return creados;
    }

    /**
     * Avanza el despacho del vendedor autenticado. Si el despacho existe pero es
     * de otro vendedor se responde 403 (el recurso existe pero no es suyo); si
     * no existe, 404. El 403 es más viable que el 404 porque el vendedor sí está
     * autenticado y el rechazo es por propiedad, no por inexistencia: el 404
     * ocultaría un IDOR y el panel necesita distinguir "no existe" de "no es tuyo".
     */
    @Transactional
    public Despacho avanzarComoVendedor(Long despachoId, Long vendedorId, EstadoPedido nuevo, String numeroGuia) {
        if (nuevo != EstadoPedido.EN_PREPARACION && nuevo != EstadoPedido.ENVIADO) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "El vendedor solo puede avanzar a EN_PREPARACION o ENVIADO", "nuevo");
        }
        Despacho despacho = despachoRepository.bloquearPorId(despachoId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Despacho no encontrado"));
        if (!despacho.getVendedor().getId().equals(vendedorId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "El despacho pertenece a otro vendedor");
        }
        // Bloqueo del pedido madre en la misma transacción: dos vendedores no se pisan.
        Pedido pedido = pedidoRepository.bloquearPorId(despacho.getPedido().getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new BusinessException(HttpStatus.CONFLICT, "El pedido está cancelado", "estado");
        }
        EstadoPedido anterior = despacho.getEstado();
        despacho.avanzarA(nuevo, numeroGuia, LocalDateTime.now());
        despachoRepository.save(despacho);
        historialRepository.save(HistorialEstadoPedido.builder()
                .pedido(pedido)
                .despacho(despacho)
                .anterior(anterior)
                .nuevo(nuevo)
                .numeroGuia(nuevo == EstadoPedido.ENVIADO ? despacho.getNumeroGuia() : null)
                .fechaCambio(LocalDateTime.now())
                .build());
        recalcularEstadoPedido(pedido.getId());
        if (nuevo == EstadoPedido.ENVIADO) {
            eventos.publishEvent(new DespachoEnviadoEvent(pedido.getId(), despacho.getId()));
        }
        eventos.publishEvent(new PedidoEstadoCambiadoEvent(pedido.getId(), pedido.getEstado()));
        return despacho;
    }

    /** Recalcula el estado global desde los despachos, en la misma transacción. */
    @Transactional
    public EstadoPedido recalcularEstadoPedido(Long pedidoId) {
        Pedido pedido = pedidoRepository.bloquearPorId(pedidoId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        List<Despacho> despachos = despachoRepository.findAllByPedidoIdOrderByIdAsc(pedidoId);
        if (despachos.isEmpty()) {
            return pedido.getEstado();
        }
        EstadoPedido derivado = EstadoPedido.calcularDerivado(
                despachos.stream().map(Despacho::getEstado).toList());
        EstadoPedido anterior = pedido.getEstado();
        if (anterior != derivado) {
            pedido.aplicarEstadoDerivado(derivado);
            historialRepository.save(HistorialEstadoPedido.builder()
                    .pedido(pedido)
                    .anterior(anterior)
                    .nuevo(derivado)
                    .fechaCambio(LocalDateTime.now())
                    .build());
        }
        return pedido.getEstado();
    }

    private Map<Long, List<LineaPedido>> agruparPorVendedor(Pedido pedido) {
        Map<Long, List<LineaPedido>> porVendedor = new LinkedHashMap<>();
        for (LineaPedido linea : pedido.getItems()) {
            Prenda prenda = prendaRepository.findById(linea.getPrendaId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                            "Prenda no encontrada"));
            if (prenda.getVendedor() == null) {
                throw new BusinessException(HttpStatus.CONFLICT, "La prenda no tiene vendedor asignado");
            }
            porVendedor.computeIfAbsent(prenda.getVendedor().getId(), k -> new ArrayList<>()).add(linea);
        }
        if (porVendedor.isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, "El pedido no tiene líneas para despachar");
        }
        return porVendedor;
    }
}
