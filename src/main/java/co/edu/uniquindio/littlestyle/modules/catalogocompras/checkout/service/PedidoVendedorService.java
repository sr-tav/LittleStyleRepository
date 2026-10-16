package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.AvanzarEstadoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoVendedorDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoVendedorResumenResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.HistorialEstadoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pedidos del vendedor autenticado: solo los que contienen al menos una de
 * sus prendas, y de cada pedido solo sus propias líneas. Puede avanzar el
 * estado de sus pedidos (preparar y despachar); cancelar es del administrador
 * porque el pedido tiene estado y guía únicos aunque incluya varios vendedores.
 */
@Service
@RequiredArgsConstructor
public class PedidoVendedorService {

    private final PedidoRepository pedidoRepository;
    private final PrendaRepository prendaRepository;
    private final HistorialEstadoRepository historialRepository;
    private final PedidoEstadoService estados;

    @Transactional(readOnly = true)
    public List<PedidoVendedorResumenResponse> misPedidos() {
        Set<Long> propias = new HashSet<>(prendaRepository.findIdsByVendedorId(vendedorActual()));
        return pedidoRepository.findAllConLineasDelVendedor(vendedorActual()).stream()
                .map(p -> PedidoVendedorResumenResponse.from(p, propias))
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoVendedorDetalleResponse detalle(Long id) {
        Set<Long> propias = new HashSet<>(prendaRepository.findIdsByVendedorId(vendedorActual()));
        Pedido pedido = pedidoRepository.findById(id)
                .filter(p -> p.getItems().stream().anyMatch(l -> propias.contains(l.getPrendaId())))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        List<PedidoDetalleResponse.Movimiento> historial = historialRepository
                .findAllByPedidoIdOrderByFechaCambioAsc(id).stream()
                .map(h -> new PedidoDetalleResponse.Movimiento(h.getAnterior(), h.getNuevo(), h.getFechaCambio()))
                .toList();
        return PedidoVendedorDetalleResponse.from(pedido, propias, historial);
    }

    @Transactional
    public PedidoVendedorDetalleResponse avanzarEstado(Long id, AvanzarEstadoRequest request) {
        Set<Long> propias = new HashSet<>(prendaRepository.findIdsByVendedorId(vendedorActual()));
        Pedido pedido = pedidoRepository.findById(id)
                .filter(p -> p.getItems().stream().anyMatch(l -> propias.contains(l.getPrendaId())))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        estados.avanzarComoVendedor(pedido.getId(), request.nuevo(), request.numeroGuia());
        return detalle(id);
    }

    private static Long vendedorActual() {
        return SecurityUtils.usuarioActual()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado")).id();
    }
}
