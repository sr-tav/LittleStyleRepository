package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.AgregarItemCarritoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.CarritoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.CambiarCantidadCarritoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.ItemCarritoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.model.Carrito;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.model.ItemCarrito;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.repository.CarritoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.LineaResumenCheckoutResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.service.ValidadorAlergiaCompra;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto.PerfilResponse;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.service.PerfilInfantilService;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
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

@Service
@RequiredArgsConstructor
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final PrendaRepository prendaRepository;
    private final DisponibilidadStockService disponibilidadStockService;
    private final PerfilInfantilService perfilInfantilService;
    private final ValidadorAlergiaCompra validadorAlergiaCompra;

    @Transactional(readOnly = true)
    public CarritoResponse obtener() {
        Carrito carrito = carritoRepository.findByClienteId(clienteActualId()).orElse(null);
        if (carrito == null) {
            return respuesta(List.of());
        }
        return respuesta(carrito.getItems());
    }

    @Transactional
    public CarritoResponse bloquearYObtenerActual() {
        Long clienteId = clienteActualId();
        Carrito carrito = carritoRepository.bloquearPorClienteId(clienteId).orElse(null);
        return carrito == null
                ? respuesta(List.of())
                : respuesta(carrito.getItems());
    }

    @Transactional
    public CarritoResponse agregar(AgregarItemCarritoRequest request) {
        validarAgregar(request);
        Carrito carrito = obtenerOBloquearActual();
        Prenda prenda = bloquearPrendaActiva(request.prendaId());
        validarAlergias(prenda, request.perfilInfantilId());
        TallaPrenda talla = talla(prenda, request.talla());
        ItemCarrito existente = carrito.getItems().stream()
                .filter(item -> item.getPrendaId().equals(prenda.getId())
                        && item.getTalla().equalsIgnoreCase(talla.getTalla()))
                .findFirst().orElse(null);
        long cantidadSolicitada = request.cantidad()
                + (existente == null ? 0L : existente.getCantidad());
        disponibilidadStockService.validarDisponibilidad(talla.getStock(), cantidadSolicitada);
        if (existente == null) {
            existente = ItemCarrito.builder()
                    .prendaId(prenda.getId())
                    .talla(talla.getTalla())
                    .cantidad(request.cantidad())
                    .tallaSugerida(normalizarSugerida(request.tallaSugerida()))
                    .build();
            carrito.agregar(existente);
        } else {
            existente.setCantidad((int) cantidadSolicitada);
            if (request.tallaSugerida() != null) {
                existente.setTallaSugerida(normalizarSugerida(request.tallaSugerida()));
            }
        }
        carritoRepository.saveAndFlush(carrito);
        return respuesta(carrito.getItems());
    }

    @Transactional
    public CarritoResponse cambiarCantidad(Long itemId, CambiarCantidadCarritoRequest request) {
        if (itemId == null || itemId <= 0 || request == null
                || request.cantidad() == null || request.cantidad() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El identificador y la cantidad deben ser válidos", "cantidad");
        }
        Carrito carrito = obtenerOBloquearActual();
        ItemCarrito item = itemPropio(carrito, itemId);
        Prenda prenda = bloquearPrendaActiva(item.getPrendaId());
        TallaPrenda talla = talla(prenda, item.getTalla());
        disponibilidadStockService.validarDisponibilidad(talla.getStock(), request.cantidad());
        item.setCantidad(request.cantidad());
        carritoRepository.saveAndFlush(carrito);
        return respuesta(carrito.getItems());
    }

    @Transactional
    public CarritoResponse quitar(Long itemId) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El producto del carrito no es válido", "itemId");
        }
        Carrito carrito = obtenerOBloquearActual();
        ItemCarrito item = itemPropio(carrito, itemId);
        carrito.quitar(item);
        carritoRepository.saveAndFlush(carrito);
        return respuesta(carrito.getItems());
    }

    @Transactional
    public CarritoResponse vaciar() {
        Carrito carrito = obtenerOBloquearActual();
        new ArrayList<>(carrito.getItems()).forEach(item -> {
            carrito.quitar(item);
        });
        carritoRepository.saveAndFlush(carrito);
        return respuesta(carrito.getItems());
    }

    @Transactional
    public void bloquearParaConfirmarPago(Long clienteId) {
        carritoRepository.bloquearPorClienteId(clienteId);
    }

    @Transactional
    public void retirarComprados(Long clienteId, List<LineaResumenCheckoutResponse> comprados) {
        Carrito carrito = carritoRepository.bloquearPorClienteId(clienteId).orElse(null);
        if (carrito == null) {
            return;
        }
        Map<ClavePrendaTalla, Integer> cantidades = new HashMap<>();
        comprados.forEach(linea -> cantidades.merge(
                new ClavePrendaTalla(linea.prendaId(), normalizarTalla(linea.talla())),
                linea.cantidad(), Math::addExact));
        List<ItemCarrito> quitar = new ArrayList<>();
        carrito.getItems().forEach(item -> {
            ClavePrendaTalla clave = new ClavePrendaTalla(item.getPrendaId(), normalizarTalla(item.getTalla()));
            int comprada = cantidades.getOrDefault(clave, 0);
            if (comprada == 0) {
                return;
            }
            if (item.getCantidad() <= comprada) {
                quitar.add(item);
            } else {
                item.setCantidad(item.getCantidad() - comprada);
            }
        });
        quitar.forEach(carrito::quitar);
        carritoRepository.saveAndFlush(carrito);
    }

    private Carrito obtenerOBloquearActual() {
        Long clienteId = clienteActualId();
        Carrito carrito = carritoRepository.bloquearPorClienteId(clienteId).orElseGet(() ->
                carritoRepository.save(Carrito.builder()
                        .cliente(Usuario.builder().id(clienteId).build())
                        .build()));
        return carrito;
    }

    private ItemCarrito itemPropio(Carrito carrito, Long itemId) {
        return carrito.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Producto del carrito no encontrado"));
    }

    private Prenda bloquearPrendaActiva(Long prendaId) {
        return prendaRepository.bloquearPorIdYEstado(prendaId, EstadoPrenda.ACTIVA)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Prenda no encontrada"));
    }

    private TallaPrenda talla(Prenda prenda, String nombre) {
        return prenda.buscarTalla(nombre.trim())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                        "La talla no está disponible para esta prenda", "talla"));
    }

    private CarritoResponse respuesta(List<ItemCarrito> items) {
        Map<Long, Prenda> prendas = new HashMap<>();
        prendaRepository.findAllById(items.stream().map(ItemCarrito::getPrendaId).distinct().toList())
                .forEach(prenda -> prendas.put(prenda.getId(), prenda));
        List<ItemCarritoResponse> lineas = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int cantidad = 0;

        for (ItemCarrito item : items) {
            Prenda prenda = prendas.get(item.getPrendaId());
            if (prenda == null) {
                throw new BusinessException(HttpStatus.NOT_FOUND,
                        "Una prenda del carrito ya no se encuentra disponible");
            }
            TallaPrenda tallaActual = prenda.buscarTalla(item.getTalla()).orElse(null);
            boolean disponible = prenda.getEstado() == EstadoPrenda.ACTIVA
                    && tallaActual != null && disponibilidadStockService.hayDisponibilidad(
                    tallaActual.getStock(), item.getCantidad());
            BigDecimal subtotalLinea = prenda.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad()));
            subtotal = subtotal.add(subtotalLinea);
            cantidad = Math.addExact(cantidad, item.getCantidad());
            lineas.add(new ItemCarritoResponse(item.getId(), prenda.getId(), prenda.getNombre(),
                    prenda.imagenPrincipal().map(imagen -> imagen.getUrl()).orElse(null),
                    item.getTalla(), item.getCantidad(), prenda.getPrecio(), subtotalLinea,
                    disponible, item.getTallaSugerida()));
        }
        BigDecimal costoEnvio = items.isEmpty() ? BigDecimal.ZERO : null;
        BigDecimal total = items.isEmpty() ? BigDecimal.ZERO : null;
        return new CarritoResponse(lineas, cantidad, subtotal, costoEnvio, total);
    }

    private void validarAgregar(AgregarItemCarritoRequest request) {
        if (request == null || request.prendaId() == null || request.prendaId() <= 0
                || request.talla() == null || request.talla().isBlank()
                || request.cantidad() == null || request.cantidad() <= 0
                || request.perfilInfantilId() != null && request.perfilInfantilId() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La prenda, talla y cantidad deben ser válidas", "cantidad");
        }
    }

    /**
     * Bloquea el agregado cuando la prenda choca con una alergia grave
     * (excluyente). Si la solicitud indica el perfil infantil activo, se evalúa
     * solo contra ese perfil; si no llega, se evalúa contra todos los perfiles
     * del cliente. Las coincidencias por advertencia (lana, algodón, tintes,
     * broches, etc.) no bloquean el agregado.
     */
    private void validarAlergias(Prenda prenda, Long perfilInfantilId) {
        if (perfilInfantilId != null) {
            validarExcluida(perfilInfantilService.obtener(perfilInfantilId), prenda);
            return;
        }
        for (PerfilResponse perfil : perfilInfantilService.listar()) {
            validarExcluida(perfil, prenda);
        }
    }

    private void validarExcluida(PerfilResponse perfil, Prenda prenda) {
        validadorAlergiaCompra.validarExcluida(
                ValidadorAlergiaCompra.alergiasEvaluables(perfil), prenda, perfil.id());
    }

    private static String normalizarSugerida(String tallaSugerida) {
        if (tallaSugerida == null || tallaSugerida.isBlank()) return null;
        return tallaSugerida.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private Long clienteActualId() {
        return SecurityUtils.usuarioActual()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"))
                .id();
    }

    private static String normalizarTalla(String talla) {
        return talla.trim().toUpperCase(Locale.ROOT);
    }

    private record ClavePrendaTalla(Long prendaId, String talla) {
    }
}
