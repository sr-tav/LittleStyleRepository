package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service.ValidadorPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto.ActualizarStockRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto.AlertaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto.InventarioItemResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.repository.AlertaReabastecimientoRepository;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
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

    /** Inventario del vendedor autenticado, de la prenda modificada más recientemente a la más antigua. */
    @Transactional(readOnly = true)
    public List<InventarioItemResponse> listar() {
        return prendaRepository.findAllByVendedorIdOrderByFechaActualizacionDesc(vendedorActual()).stream()
                .map(InventarioItemResponse::from)
                .toList();
    }

    /**
     * Bloquea la prenda propia, fija el stock y el mínimo de las tallas enviadas (sin repetidas y existentes
     * en la prenda) y evalúa las alertas en la misma transacción.
     */
    @Transactional
    public InventarioItemResponse actualizarStock(Long prendaId, ActualizarStockRequest request) {
        Prenda prenda = prendaRepository.bloquearPropia(prendaId, vendedorActual())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Prenda no encontrada"));

        Set<String> vistas = new HashSet<>();
        Set<String> origenes = new HashSet<>();
        Set<String> destinos = new HashSet<>();

        for (ActualizarStockRequest.StockTalla cambio : request.tallas()) {
            String nombre = ValidadorPrenda.normalizarTalla(cambio.talla());

            if (!origenes.add(nombre + "|" + cambio.color())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "La variante " + TallaPrenda.etiqueta(nombre, cambio.color()) + " está repetida", "tallas");
            }

            TallaPrenda talla = prenda.buscarVariante(nombre, cambio.color())
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.BAD_REQUEST,
                            "La prenda no tiene la variante "
                                    + TallaPrenda.etiqueta(nombre, cambio.color()),
                            "tallas"));

            ColorPreferido colorDestino = cambio.nuevoColor() == null
                    ? cambio.color()
                    : cambio.nuevoColor();

            if (!destinos.add(nombre + "|" + colorDestino)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "La variante " + TallaPrenda.etiqueta(nombre, colorDestino) + " está repetida",
                        "tallas");
            }

            if (cambio.nuevoColor() != null && cambio.color() != null
                    && cambio.nuevoColor() != cambio.color()) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "Solo puedes asignar color a una variante que todavía no tiene color",
                        "tallas");
            }

            if (cambio.color() == null && cambio.nuevoColor() != null) {
                boolean destinoOcupado = prenda.getTallas().stream()
                        .anyMatch(otra -> otra != talla
                                && ValidadorPrenda.normalizarTalla(otra.getTalla()).equals(nombre)
                                && otra.getColor() == colorDestino);

                if (destinoOcupado) {
                    throw new BusinessException(HttpStatus.CONFLICT,
                            "La talla " + nombre + " ya tiene una variante en ese color",
                            "tallas");
                }

                talla.setColor(colorDestino);
                prenda.getColores().add(colorDestino);
            }

            talla.setStock(cambio.stock());
            if (cambio.stockMinimo() != null) {
                talla.setStockMinimo(cambio.stockMinimo());
            }
        }
        prenda.setFechaActualizacion(LocalDateTime.now());
        alertas.evaluar(prenda);
        return InventarioItemResponse.from(prenda);
    }

    /** Alertas activas del vendedor autenticado. */
    @Transactional(readOnly = true)
    public List<AlertaResponse> alertasActivas() {
        return alertaRepository.findAllByPrendaVendedorIdAndFechaResolucionIsNullOrderByFechaEmisionDesc(vendedorActual())
                .stream()
                .map(AlertaResponse::from)
                .toList();
    }

    /** Id del vendedor autenticado; 401 si no hay sesión. */
    private static Long vendedorActual() {
        return SecurityUtils.usuarioActual()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"))
                .id();
    }
}
