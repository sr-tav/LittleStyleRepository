package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.AvanzarEstadoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DespachoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Despacho;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DespachoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Paquetes del vendedor autenticado: solo su despacho, con su estado, su guía
 * y sus productos. Actuar sobre el paquete de otro se rechaza con 403.
 */
@Service
@RequiredArgsConstructor
public class PedidoVendedorService {

    private final DespachoRepository despachoRepository;
    private final PedidoRepository pedidoRepository;
    private final co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository prendaRepository;
    private final DespachoService despachos;

    /** Despachos del vendedor autenticado. */
    @Transactional(readOnly = true)
    public List<DespachoResponse> misDespachos() {
        return despachoRepository.findAllByVendedorIdOrderByFechaActualizacionDesc(vendedorActual()).stream()
                .map(this::responder)
                .toList();
    }

    /** Despacho propio con sus productos. */
    @Transactional(readOnly = true)
    public DespachoResponse detalle(Long despachoId) {
        return responder(exigirPropio(despachoId));
    }

    /** Avanza un despacho propio y devuelve su estado actualizado. */
    @Transactional
    public DespachoResponse avanzarEstado(Long despachoId, AvanzarEstadoRequest request) {
        exigirPropio(despachoId);
        Despacho actualizado = despachos.avanzarComoVendedor(
                despachoId, vendedorActual(), request.nuevo(), request.numeroGuia());
        return responder(actualizado);
    }

    /** Carga el despacho; 404 si no existe y 403 si es de otro vendedor. */
    private Despacho exigirPropio(Long despachoId) {
        Despacho despacho = despachoRepository.findById(despachoId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Despacho no encontrado"));
        if (!despacho.getVendedor().getId().equals(vendedorActual())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "El despacho pertenece a otro vendedor");
        }
        return despacho;
    }

    /** Arma la respuesta del despacho con sus líneas e imágenes. */
    private DespachoResponse responder(Despacho despacho) {
        Pedido pedido = pedidoRepository.findById(despacho.getPedido().getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        List<PedidoDetalleResponse.Item> lineas = pedido.getItems().stream()
                .filter(l -> l.getDespacho() != null && l.getDespacho().getId().equals(despacho.getId()))
                .map(l -> PedidoDetalleResponse.Item.from(l, imagenDe(l.getPrendaId())))
                .toList();
        return DespachoResponse.from(despacho, lineas);
    }

    /** Imagen principal actual de la prenda, o {@code null}. */
    private String imagenDe(Long prendaId) {
        return prendaRepository.findById(prendaId)
                .flatMap(co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda::imagenPrincipal)
                .map(co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda::getUrl)
                .orElse(null);
    }

    /** Id del vendedor autenticado; 401 si no hay sesión. */
    private static Long vendedorActual() {
        return SecurityUtils.usuarioActual()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado")).id();
    }
}
