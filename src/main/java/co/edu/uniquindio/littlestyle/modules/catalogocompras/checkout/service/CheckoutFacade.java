package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service.DisponibilidadStockService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ItemResumenCheckoutRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DireccionEnvioRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.LineaResumenCheckoutResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ResumenCheckoutRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ResumenCheckoutResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.ComponenteMaterial;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.PrendaRecomendable;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.TablaTallas;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.FiltroAlergia;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto.PerfilResponse;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.service.PerfilInfantilService;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Coordina las validaciones y los cálculos previos a la creación de un pedido.
 * No reserva ni descuenta existencias; solo valida el stock físico actual.
 */
@Service
@RequiredArgsConstructor
public class CheckoutFacade {

    private final PrendaRepository prendaRepository;
    private final PerfilInfantilService perfilInfantilService;
    private final FiltroAlergia filtroAlergia;
    private final DisponibilidadStockService disponibilidadStockService;
    private final TarifaEnvioService tarifaEnvioService;

    @Transactional(readOnly = true)
    public ResumenCheckoutResponse calcularResumen(ResumenCheckoutRequest request,
                                                    List<ItemResumenCheckoutRequest> items) {
        validarPerfil(request);
        PerfilResponse perfil = perfilInfantilService.obtener(request.perfilInfantilId());
        validarItems(items);
        Set<AlergiaTextil> alergias = perfil.sinAlergias()
                ? Set.of()
                : perfil.alergias();

        Map<Long, Prenda> prendas = new HashMap<>();
        List<LineaResumenCheckoutResponse> lineas = new ArrayList<>();
        Map<ClaveTalla, Long> cantidadesSolicitadas = new HashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (ItemResumenCheckoutRequest item : items) {
            Prenda prenda = prendas.computeIfAbsent(item.prendaId(), id -> prendaRepository
                    .findByIdAndEstado(id, EstadoPrenda.ACTIVA)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                            "Prenda no encontrada")));

            validarCompatibilidad(perfil.id(), alergias, prenda);
            TallaPrenda talla = prenda.buscarTalla(item.talla())
                    .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                            "La talla no está disponible para esta prenda", "talla"));
            ClaveTalla clave = new ClaveTalla(prenda.getId(), normalizarTalla(talla.getTalla()));
            cantidadesSolicitadas.merge(clave, (long) item.cantidad(), Long::sum);

            BigDecimal subtotalLinea = prenda.getPrecio().multiply(BigDecimal.valueOf(item.cantidad()));
            subtotal = subtotal.add(subtotalLinea);
            lineas.add(new LineaResumenCheckoutResponse(prenda.getId(), prenda.getNombre(),
                    talla.getTalla(), item.cantidad(), prenda.getPrecio(), subtotalLinea));
        }

        validarStockDisponible(prendas, cantidadesSolicitadas);
        BigDecimal costoEnvio = tarifaEnvioService.calcular(
                request.direccion().departamento(), request.direccion().municipio());
        return new ResumenCheckoutResponse(perfil.id(), lineas, subtotal, costoEnvio,
                subtotal.add(costoEnvio));
    }

    private void validarPerfil(ResumenCheckoutRequest request) {
        if (request == null || request.perfilInfantilId() == null || request.perfilInfantilId() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El perfil infantil debe ser válido", "perfilInfantilId");
        }
        if (request.direccion() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La dirección de entrega es obligatoria para calcular el envío", "direccion");
        }
    }

    private void validarItems(List<ItemResumenCheckoutRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, "El carrito está vacío", "items");
        }
        if (items.stream().anyMatch(item -> item == null
                || item.prendaId() == null || item.prendaId() <= 0
                || item.talla() == null || item.talla().isBlank()
                || item.cantidad() == null || item.cantidad() <= 0)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Los productos del carrito deben ser válidos", "items");
        }
    }

    private void validarCompatibilidad(Long perfilId, Set<AlergiaTextil> alergias, Prenda prenda) {
        PrendaRecomendable prendaEvaluable = new PrendaRecomendable(
                prenda.getId(), prenda.getNombre(), prenda.getCategoria().name(), null,
                prenda.getPrecio(), null, new TablaTallas(List.of()),
                prenda.getComposicion().stream()
                        .map(componente -> new ComponenteMaterial(componente.getMaterial(),
                                componente.getPorcentaje()))
                        .toList(),
                prenda.isTintesSinteticos(), prenda.isBrochesMetalicos(),
                prenda.isContieneNiquel(), prenda.isTratamientoFormaldehido(), Set.of(),
                prenda.getEstampado(), Map.of());
        if (filtroAlergia.evaluar(alergias, prendaEvaluable).excluye()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La prenda " + prenda.getNombre()
                            + " no es compatible con las alergias del perfil infantil " + perfilId,
                    "items");
        }
    }

    private void validarStockDisponible(Map<Long, Prenda> prendas,
                                        Map<ClaveTalla, Long> cantidadesSolicitadas) {
        cantidadesSolicitadas.forEach((clave, cantidad) -> {
            TallaPrenda talla = prendas.get(clave.prendaId()).buscarTalla(clave.talla())
                    .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                            "La talla no está disponible para esta prenda", "talla"));
            disponibilidadStockService.validarDisponibilidad(talla.getStock(), cantidad);
        });
    }

    private String normalizarTalla(String talla) {
        return talla.trim().toUpperCase(Locale.ROOT);
    }

    private record ClaveTalla(Long prendaId, String talla) {
    }
}
