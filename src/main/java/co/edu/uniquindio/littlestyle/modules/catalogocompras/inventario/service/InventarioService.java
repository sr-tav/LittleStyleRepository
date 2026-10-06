package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service.ValidadorPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto.ActualizarStockRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto.AlertaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto.InventarioItemResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.repository.AlertaReabastecimientoRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Existencias del vendedor autenticado. Cada actualización bloquea la prenda, aplica el stock y evalúa
 * las alertas en una sola transacción.
 */
@Service
@RequiredArgsConstructor
public class InventarioService {

    private final PrendaRepository prendaRepository;
    private final AlertaReabastecimientoRepository alertaRepository;
    private final AlertasInventarioService alertas;

    @Transactional(readOnly = true)
    public List<InventarioItemResponse> listar() {
        return prendaRepository.findAllByVendedorIdOrderByFechaActualizacionDesc(vendedorActual()).stream()
                .map(InventarioItemResponse::from)
                .toList();
    }

    @Transactional
    public InventarioItemResponse actualizarStock(Long prendaId, ActualizarStockRequest request) {
        Prenda prenda = prendaRepository.bloquearPropia(prendaId, vendedorActual())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Prenda no encontrada"));

        Set<String> vistas = new HashSet<>();
        for (ActualizarStockRequest.StockTalla cambio : request.tallas()) {
            String nombre = ValidadorPrenda.normalizarTalla(cambio.talla());
            if (!vistas.add(nombre)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "La talla " + nombre + " está repetida", "tallas");
            }
            TallaPrenda talla = prenda.buscarTalla(nombre).orElseThrow(() -> new BusinessException(
                    HttpStatus.BAD_REQUEST, "La prenda no tiene la talla " + nombre, "tallas"));
            talla.setStock(cambio.stock());
        }
        if (request.stockMinimo() != null) {
            prenda.setStockMinimo(request.stockMinimo());
        }
        prenda.setFechaActualizacion(LocalDateTime.now());
        alertas.evaluar(prenda);
        return InventarioItemResponse.from(prenda);
    }

    @Transactional(readOnly = true)
    public List<AlertaResponse> alertasActivas() {
        return alertaRepository.findAllByPrendaVendedorIdAndFechaResolucionIsNullOrderByFechaEmisionDesc(vendedorActual())
                .stream()
                .map(AlertaResponse::from)
                .toList();
    }

    private static Long vendedorActual() {
        return SecurityUtils.usuarioActual()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"))
                .id();
    }
}
