package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.AgregarItemCarritoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.CarritoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.CambiarCantidadCarritoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.ItemCarritoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.model.Carrito;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.model.ItemCarrito;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.repository.CarritoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion;
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

/**
 * Operaciones sobre el carrito del cliente autenticado. Los cambios bloquean el carrito y la prenda para
 * validar el stock sin carreras.
 */
@Service
@RequiredArgsConstructor
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final PrendaRepository prendaRepository;
    private final DisponibilidadStockService disponibilidadStockService;
    private final PerfilInfantilService perfilInfantilService;
    private final ValidadorAlergiaCompra validadorAlergiaCompra;

    /** Carrito del cliente actual; vacío si todavía no tiene uno. */
    @Transactional(readOnly = true)
    public CarritoResponse obtener() {
        Carrito carrito = carritoRepository.findByClienteId(clienteActualId()).orElse(null);
        if (carrito == null) {
            return respuesta(List.of());
        }
        return respuesta(carrito.getItems());
    }

    /** Bloquea el carrito del cliente y lo devuelve, para que el checkout lo lea sin cambios concurrentes. */
    @Transactional
    public CarritoResponse bloquearYObtenerActual() {
        Long clienteId = clienteActualId();
        Carrito carrito = carritoRepository.bloquearPorClienteId(clienteId).orElse(null);
        return carrito == null
                ? respuesta(List.of())
                : respuesta(carrito.getItems());
    }

    /**
     * Agrega la prenda publicada en la talla pedida, rechazándola si choca con una alergia grave del perfil.
     * Si la línea ya existe suma la cantidad; en ambos casos valida que haya stock para el total.
     */
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

    /** Fija la cantidad de una línea propia validando el stock de la talla. */
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

    /** Quita una línea propia del carrito. */
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

    /** Quita todas las líneas del carrito del cliente. */
    @Transactional
    public CarritoResponse vaciar() {
        Carrito carrito = obtenerOBloquearActual();
        new ArrayList<>(carrito.getItems()).forEach(item -> {
            carrito.quitar(item);
        });
        carritoRepository.saveAndFlush(carrito);
        return respuesta(carrito.getItems());
    }

    /**
     * Bloquea el carrito del cliente antes de confirmar un pago, para ordenar los bloqueos con el carrito
     * primero.
     */
    @Transactional
    public void bloquearParaConfirmarPago(Long clienteId) {
        carritoRepository.bloquearPorClienteId(clienteId);
    }

    /**
     * Descuenta del carrito lo comprado en un pedido pagado: quita las líneas compradas por completo y reduce
     * la cantidad de las que tenían más unidades.
     */
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

    /** Bloquea el carrito del cliente actual o lo crea si no existe. */
    private Carrito obtenerOBloquearActual() {
        Long clienteId = clienteActualId();
        Carrito carrito = carritoRepository.bloquearPorClienteId(clienteId).orElseGet(() ->
                carritoRepository.save(Carrito.builder()
                        .cliente(Usuario.builder().id(clienteId).build())
                        .build()));
        return carrito;
    }

    /** Busca la línea dentro del carrito del cliente; 404 si no le pertenece. */
    private ItemCarrito itemPropio(Carrito carrito, Long itemId) {
        return carrito.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Producto del carrito no encontrado"));
    }

    /** Bloquea la prenda si está publicada; 404 en otro caso. */
    private Prenda bloquearPrendaActiva(Long prendaId) {
        return prendaRepository.bloquearPorIdYEstado(prendaId, EstadoPrenda.ACTIVA)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Prenda no encontrada"));
    }

    /** Talla de la prenda con ese nombre; 400 si la prenda no la ofrece. */
    private TallaPrenda talla(Prenda prenda, String nombre) {
        return prenda.buscarTalla(nombre.trim())
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                        "La talla no está disponible para esta prenda", "talla"));
    }

    /**
     * Arma la respuesta con el precio actual de cada prenda, la disponibilidad de cada línea y el subtotal.
     */
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
            // Una prenda SIN_REVISAR sigue publicada: solo la desactivacion administrativa la oculta.
            boolean publicable = prenda.getEstado() == EstadoPrenda.ACTIVA
                    && prenda.getEstadoModeracion() != EstadoModeracion.DESACTIVADA
                    && prenda.getVendedor() != null
                    && prenda.getVendedor().getEstado() == EstadoUsuario.ACTIVO;
            boolean disponible = publicable
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

    /** Valida que la prenda, la talla, la cantidad y el perfil (si llega) sean válidos. */
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

    /** Rechaza la prenda si contiene un material excluido por las alergias graves del perfil. */
    private void validarExcluida(PerfilResponse perfil, Prenda prenda) {
        validadorAlergiaCompra.validarExcluida(
                ValidadorAlergiaCompra.alergiasEvaluables(perfil), prenda, perfil.id());
    }

    /** Talla sugerida en mayúsculas, o nula si no se envió. */
    private static String normalizarSugerida(String tallaSugerida) {
        if (tallaSugerida == null || tallaSugerida.isBlank()) return null;
        return tallaSugerida.trim().toUpperCase(java.util.Locale.ROOT);
    }

    /** Id del cliente autenticado; 401 si no hay sesión. */
    private Long clienteActualId() {
        return SecurityUtils.usuarioActual()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"))
                .id();
    }

    /** Talla sin espacios y en mayúsculas, para comparar líneas. */
    private static String normalizarTalla(String talla) {
        return talla.trim().toUpperCase(Locale.ROOT);
    }

    /** Identifica una línea por prenda y talla. */
    private record ClavePrendaTalla(Long prendaId, String talla) {
    }
}
