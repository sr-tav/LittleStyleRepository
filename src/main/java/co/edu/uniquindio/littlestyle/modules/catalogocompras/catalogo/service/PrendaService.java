package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service.AlertasInventarioService;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Alta y edición de prendas del vendedor autenticado. Una prenda de otro vendedor se trata como
 * inexistente (404) para no revelar su existencia.
 *
 * @see "US-14"
 * @see "RNF-08"
 */
@Service
@RequiredArgsConstructor
public class PrendaService {

    private final PrendaRepository prendaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ValidadorPrenda validador;
    private final AlertasInventarioService alertas;
    private final ImagenPrendaService imagenService;

    /** Prendas del vendedor; la búsqueda compara nombre y categoría sin tildes ni mayúsculas. */
    @Transactional(readOnly = true)
    public List<PrendaResponse> listar(String busqueda) {
        String filtro = normalizarTexto(busqueda);
        return prendaRepository.findAllByVendedorIdOrderByFechaActualizacionDesc(vendedorActual()).stream()
                .filter(p -> filtro.isEmpty()
                        || normalizarTexto(p.getNombre()).contains(filtro)
                        || normalizarTexto(p.getCategoria().name()).contains(filtro))
                .map(PrendaResponse::from)
                .toList();
    }

    /** Prenda propia; 404 si no existe o es de otro vendedor. */
    @Transactional(readOnly = true)
    public PrendaResponse obtener(Long id) {
        return PrendaResponse.from(propia(id));
    }

    /**
     * Crea una prenda publicada para el vendedor autenticado con sus fotos (al menos una) en una sola
     * transacción: si algo falla no queda ni la prenda ni los archivos.
     */
    @Transactional
    public PrendaResponse crear(PrendaRequest request, List<MultipartFile> archivos) {
        validador.validar(request);
        if (archivos == null || archivos.stream().allMatch(a -> a == null || a.isEmpty())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Agrega al menos una foto de la prenda", "archivos");
        }
        LocalDateTime ahora = LocalDateTime.now();
        Prenda prenda = Prenda.builder()
                .vendedor(usuarioRepository.getReferenceById(vendedorActual()))
                .estado(EstadoPrenda.ACTIVA)
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .build();
        aplicarDatos(prenda, request);
        request.tallas().forEach(t -> prenda.getTallas().add(nuevaTalla(prenda, t, request)));
        prendaRepository.save(prenda);
        imagenService.agregar(prenda, archivos);
        alertas.evaluar(prenda);
        return PrendaResponse.from(prenda);
    }

    /**
     * Actualiza los datos y la tabla de tallas. El stock de las tallas existentes no se toca aquí (se
     * gestiona en el inventario), así un formulario abierto hace rato no sobrescribe ventas recientes.
     */
    @Transactional
    public PrendaResponse actualizar(Long id, PrendaRequest request) {
        validador.validar(request);
        Prenda prenda = propiaBloqueada(id);
        validarQueNoEsteDesactivadaPorModeracion(prenda);
        aplicarDatos(prenda, request);
        sincronizarTallas(prenda, request);
        prenda.setFechaActualizacion(LocalDateTime.now());
        alertas.evaluar(prenda);
        return PrendaResponse.from(prenda);
    }

    /** Cambia el estado de publicación y reevalúa las alertas (retirarla resuelve las activas). */
    @Transactional
    public PrendaResponse cambiarEstado(Long id, EstadoPrenda estado) {
        Prenda prenda = propiaBloqueada(id);
        validarQueNoEsteDesactivadaPorModeracion(prenda);
        if (estado == EstadoPrenda.ACTIVA && prenda.getImagenes().isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Agrega al menos una foto antes de publicar la prenda", "imagenes");
        }
        prenda.setEstado(estado);
        prenda.setFechaActualizacion(LocalDateTime.now());
        alertas.evaluar(prenda);
        return PrendaResponse.from(prenda);
    }

    /** Copia a la prenda los datos del formulario; sin género queda unisex. */
    private void aplicarDatos(Prenda prenda, PrendaRequest r) {
        prenda.setNombre(r.nombre().trim());
        prenda.setCategoria(r.categoria());
        prenda.setDescripcion(r.descripcion().trim());
        prenda.setInstruccionesCuidado(r.instruccionesCuidado().trim());
        prenda.setMarca(textoOpcional(r.marca()));
        prenda.setPrecio(r.precio());
        Set<Estampado> estampados = r.estampadosEfectivos();
        prenda.getEstampados().clear();
        prenda.getEstampados().addAll(estampados);
        // El estampado principal alimenta al motor de recomendación: el indicado si es válido o el primero.
        prenda.setEstampado(r.estampado() != null && estampados.contains(r.estampado())
                ? r.estampado()
                : estampados.stream().min(Comparator.naturalOrder()).orElse(null));
        prenda.setGenero(r.genero() != null ? r.genero() : GeneroPrenda.UNISEX);
        prenda.setTintesSinteticos(Boolean.TRUE.equals(r.tintesSinteticos()));
        prenda.setBrochesMetalicos(Boolean.TRUE.equals(r.brochesMetalicos()));
        prenda.setContieneNiquel(Boolean.TRUE.equals(r.contieneNiquel()));
        prenda.setTratamientoFormaldehido(Boolean.TRUE.equals(r.tratamientoFormaldehido()));

        prenda.getColores().clear();
        if (r.colores() != null) {
            prenda.getColores().addAll(r.colores());
        }
        prenda.getComposicion().clear();
        r.composicion().forEach(c -> prenda.getComposicion().add(new ComponenteComposicion(c.material(), c.porcentaje())));
    }

    /**
     * Actualiza los rangos de las variantes que siguen, agrega las nuevas y quita las que ya no vienen; no
     * permite quitar una variante con stock. Una fila anterior a las variantes (sin color) adopta el primer
     * color pedido para su talla y conserva su stock.
     */
    private void sincronizarTallas(Prenda prenda, PrendaRequest request) {
        Map<String, TallaRequest> porClave = new LinkedHashMap<>();
        request.tallas().forEach(t -> porClave.put(clave(t.talla(), ValidadorPrenda.colorDe(t, request)), t));

        for (TallaPrenda legado : prenda.getTallas()) {
            if (legado.getColor() != null) continue;
            porClave.values().stream()
                    .filter(t -> ValidadorPrenda.normalizarTalla(t.talla()).equals(legado.getTalla()))
                    .map(t -> ValidadorPrenda.colorDe(t, request))
                    .filter(c -> prenda.buscarVariante(legado.getTalla(), c).isEmpty())
                    .findFirst()
                    .ifPresent(legado::setColor);
        }

        for (Iterator<TallaPrenda> it = prenda.getTallas().iterator(); it.hasNext(); ) {
            TallaPrenda existente = it.next();
            TallaRequest cambio = porClave.remove(clave(existente.getTalla(), existente.getColor()));
            if (cambio == null) {
                if (existente.getStock() > 0) {
                    throw new BusinessException(HttpStatus.CONFLICT, "No puedes quitar la talla " + existente.etiqueta()
                            + " porque tiene " + existente.getStock() + " unidades. Deja su stock en 0 desde el inventario",
                            "tallas");
                }
                it.remove();
            } else {
                aplicarRangos(existente, cambio);
            }
        }
        porClave.values().forEach(t -> prenda.getTallas().add(nuevaTalla(prenda, t, request)));
    }

    private static String clave(String talla, ColorPreferido color) {
        return ValidadorPrenda.normalizarTalla(talla) + "|" + color;
    }

    /** Crea una variante con su stock inicial (0 si no se indica), su color y sus rangos. */
    private static TallaPrenda nuevaTalla(Prenda prenda, TallaRequest t, PrendaRequest request) {
        TallaPrenda talla = TallaPrenda.builder()
                .prenda(prenda)
                .talla(ValidadorPrenda.normalizarTalla(t.talla()))
                .color(ValidadorPrenda.colorDe(t, request))
                .stock(t.stock() == null ? 0 : t.stock())
                .build();
        aplicarRangos(talla, t);
        return talla;
    }

    /** Copia a la talla los rangos de estatura, peso y edad y el stock mínimo. */
    private static void aplicarRangos(TallaPrenda talla, TallaRequest t) {
        talla.setEstaturaMinCm(t.estaturaMinCm());
        talla.setEstaturaMaxCm(t.estaturaMaxCm());
        talla.setPesoMinKg(t.pesoMinKg());
        talla.setPesoMaxKg(t.pesoMaxKg());
        talla.setEdadMinMeses(t.edadMinMeses());
        talla.setEdadMaxMeses(t.edadMaxMeses());
        talla.setStockMinimo(t.stockMinimo());
    }

    /** Prenda del vendedor actual; 404 en otro caso. */
    private Prenda propia(Long id) {
        return prendaRepository.findByIdAndVendedorId(id, vendedorActual()).orElseThrow(PrendaService::noEncontrada);
    }

    /** Prenda del vendedor actual con bloqueo de escritura; 404 en otro caso. */
    private Prenda propiaBloqueada(Long id) {
        return prendaRepository.bloquearPropia(id, vendedorActual()).orElseThrow(PrendaService::noEncontrada);
    }

    /** El vendedor no puede editar ni volver a publicar una prenda retirada por moderacion. */
    private static void validarQueNoEsteDesactivadaPorModeracion(Prenda prenda) {
        if (prenda.getEstadoModeracion() == EstadoModeracion.DESACTIVADA) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "La prenda fue desactivada por moderación y no puede modificarse ni reactivarse. "
                            + "Motivo: " + prenda.getMotivoModeracion()
                            + ". Puede apelar creando una incidencia de producto.",
                    "estadoModeracion");
        }
    }

    /** Error 404 de prenda no encontrada. */
    static BusinessException noEncontrada() {
        return new BusinessException(HttpStatus.NOT_FOUND, "Prenda no encontrada");
    }

    /** Id del vendedor autenticado; 401 si no hay sesión. */
    static Long vendedorActual() {
        return SecurityUtils.usuarioActual()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"))
                .id();
    }

    /** Texto sin espacios sobrantes, o {@code null} si viene vacío. */
    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    /** Minúsculas y sin tildes, para que "vestído" encuentre "Vestido". */
    private static String normalizarTexto(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
