package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.TallaRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ComponenteComposicion;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service.AlertasInventarioService;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Alta y edición de prendas del vendedor autenticado. Una prenda de otro vendedor se trata como
 * inexistente (404) para no revelar su existencia.
 */
@Service
@RequiredArgsConstructor
public class PrendaService {

    private final PrendaRepository prendaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ValidadorPrenda validador;
    private final AlertasInventarioService alertas;

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

    @Transactional(readOnly = true)
    public PrendaResponse obtener(Long id) {
        return PrendaResponse.from(propia(id));
    }

    @Transactional
    public PrendaResponse crear(PrendaRequest request) {
        validador.validar(request);
        LocalDateTime ahora = LocalDateTime.now();
        Prenda prenda = Prenda.builder()
                .vendedor(usuarioRepository.getReferenceById(vendedorActual()))
                .estado(EstadoPrenda.ACTIVA)
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .build();
        aplicarDatos(prenda, request);
        request.tallas().forEach(t -> prenda.getTallas().add(nuevaTalla(prenda, t)));
        prendaRepository.save(prenda);
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
        aplicarDatos(prenda, request);
        sincronizarTallas(prenda, request.tallas());
        prenda.setFechaActualizacion(LocalDateTime.now());
        alertas.evaluar(prenda);
        return PrendaResponse.from(prenda);
    }

    @Transactional
    public PrendaResponse cambiarEstado(Long id, EstadoPrenda estado) {
        Prenda prenda = propiaBloqueada(id);
        prenda.setEstado(estado);
        prenda.setFechaActualizacion(LocalDateTime.now());
        alertas.evaluar(prenda);
        return PrendaResponse.from(prenda);
    }

    private void aplicarDatos(Prenda prenda, PrendaRequest r) {
        prenda.setNombre(r.nombre().trim());
        prenda.setCategoria(r.categoria());
        prenda.setDescripcion(textoOpcional(r.descripcion()));
        prenda.setMarca(textoOpcional(r.marca()));
        prenda.setPrecio(r.precio());
        prenda.setStockMinimo(r.stockMinimo());
        prenda.setEstampado(r.estampado());
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

    private void sincronizarTallas(Prenda prenda, List<TallaRequest> solicitadas) {
        Map<String, TallaRequest> porNombre = new LinkedHashMap<>();
        solicitadas.forEach(t -> porNombre.put(ValidadorPrenda.normalizarTalla(t.talla()), t));

        for (Iterator<TallaPrenda> it = prenda.getTallas().iterator(); it.hasNext(); ) {
            TallaPrenda existente = it.next();
            TallaRequest cambio = porNombre.remove(existente.getTalla());
            if (cambio == null) {
                if (existente.getStock() > 0) {
                    throw new BusinessException(HttpStatus.CONFLICT, "No puedes quitar la talla " + existente.getTalla()
                            + " porque tiene " + existente.getStock() + " unidades. Deja su stock en 0 desde el inventario",
                            "tallas");
                }
                it.remove();
            } else {
                aplicarRangos(existente, cambio);
            }
        }
        porNombre.values().forEach(t -> prenda.getTallas().add(nuevaTalla(prenda, t)));
    }

    private static TallaPrenda nuevaTalla(Prenda prenda, TallaRequest t) {
        TallaPrenda talla = TallaPrenda.builder()
                .prenda(prenda)
                .talla(ValidadorPrenda.normalizarTalla(t.talla()))
                .stock(t.stock() == null ? 0 : t.stock())
                .build();
        aplicarRangos(talla, t);
        return talla;
    }

    private static void aplicarRangos(TallaPrenda talla, TallaRequest t) {
        talla.setEstaturaMinCm(t.estaturaMinCm());
        talla.setEstaturaMaxCm(t.estaturaMaxCm());
        talla.setPesoMinKg(t.pesoMinKg());
        talla.setPesoMaxKg(t.pesoMaxKg());
    }

    private Prenda propia(Long id) {
        return prendaRepository.findByIdAndVendedorId(id, vendedorActual()).orElseThrow(PrendaService::noEncontrada);
    }

    private Prenda propiaBloqueada(Long id) {
        return prendaRepository.bloquearPropia(id, vendedorActual()).orElseThrow(PrendaService::noEncontrada);
    }

    static BusinessException noEncontrada() {
        return new BusinessException(HttpStatus.NOT_FOUND, "Prenda no encontrada");
    }

    static Long vendedorActual() {
        return SecurityUtils.usuarioActual()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"))
                .id();
    }

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
