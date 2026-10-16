package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaVitrinaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.shared.dto.PaginaResponse;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Exploración del catálogo para el cliente autenticado (base de US-09).
 * Solo prendas publicadas ({@code ACTIVA}); los filtros se aplican en la base
 * de datos con paginación, nunca trayendo todo el catálogo a memoria.
 */
@Service
@RequiredArgsConstructor
public class CatalogoClienteService {

    static final int TAMANO_MAXIMO = 50;

    private final PrendaRepository prendaRepository;

    @Transactional(readOnly = true)
    public PaginaResponse<PrendaVitrinaResponse> explorar(String texto, String categoria, String genero,
                                                          String precioMin, String precioMax, String disponible,
                                                          String material, String talla, int pagina,
                                                          int tamano, String orden) {
        return explorar(texto, categoria, genero, precioMin, precioMax, disponible, material, talla, null, null,
                pagina, tamano, orden);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PrendaVitrinaResponse> explorar(String texto, String categoria, String genero,
                                                          String precioMin, String precioMax, String disponible,
                                                          String material, String talla, List<Long> ids, int pagina,
                                                          int tamano, String orden) {
        return explorar(texto, categoria, genero, precioMin, precioMax, disponible, material, talla, ids, null,
                pagina, tamano, orden);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PrendaVitrinaResponse> explorar(String texto, String categoria, String genero,
                                                          String precioMin, String precioMax, String disponible,
                                                          String material, String talla, List<Long> ids,
                                                          Integer edadMeses, int pagina, int tamano, String orden) {
        if (pagina < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La página no puede ser negativa", "pagina");
        }
        BigDecimal min = numero(precioMin, "precioMin");
        BigDecimal max = numero(precioMax, "precioMax");
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El precio mínimo no puede superar al máximo", "precioMin");
        }
        if (edadMeses != null && edadMeses < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La edad no puede ser negativa", "edadMeses");
        }
        Pageable paginable = PageRequest.of(pagina, Math.min(Math.max(tamano, 1), TAMANO_MAXIMO), orden(orden));
        String q = texto == null || texto.isBlank() ? null : texto.trim();
        String t = talla == null || talla.isBlank() ? null : talla.trim();
        List<MaterialTextil> materiales = materiales(material);
        Page<Long> paginaIds;
        if (ids != null) {
            if (ids.isEmpty()) {
                return PaginaResponse.from(Page.empty(paginable));
            }
            paginaIds = prendaRepository.buscarIdsVitrinaPorIds(EstadoPrenda.ACTIVA, ids,
                    categoria(categoria), genero(genero), min, max, q, disponibilidad(disponible),
                    materiales, t, edadMeses, paginable);
        } else {
            paginaIds = prendaRepository.buscarIdsVitrina(EstadoPrenda.ACTIVA,
                    categoria(categoria), genero(genero), min, max, q, disponibilidad(disponible),
                    materiales, t, edadMeses, paginable);
        }
        if (paginaIds.isEmpty()) {
            return PaginaResponse.from(Page.empty(paginable));
        }
        // Paso 2: trae las tallas de la página ya calculada y reordena, porque
        // el `in` no conserva el orden del `Pageable`.
        Map<Long, Prenda> porId = new HashMap<>();
        prendaRepository.buscarPorIds(paginaIds.getContent()).forEach(p -> porId.put(p.getId(), p));
        List<PrendaVitrinaResponse> contenido = paginaIds.getContent().stream()
                .map(porId::get)
                .filter(Objects::nonNull)
                .map(PrendaVitrinaResponse::from)
                .toList();
        return PaginaResponse.from(new PageImpl<>(contenido, paginable, paginaIds.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public PrendaDetalleResponse detalle(Long id) {
        Prenda prenda = prendaRepository.findByIdAndEstado(id, EstadoPrenda.ACTIVA)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Prenda no encontrada"));
        return PrendaDetalleResponse.from(prenda);
    }

    private static CategoriaPrenda categoria(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return CategoriaPrenda.valueOf(valor.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La categoría no es válida", "categoria");
        }
    }

    private static List<MaterialTextil> materiales(String valor) {
        if (valor == null || valor.isBlank()) return null;
        List<MaterialTextil> lista = new ArrayList<>();
        for (String parte : valor.split(",")) {
            if (parte.isBlank()) continue;
            try {
                lista.add(MaterialTextil.valueOf(parte.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "El material no es válido", "material");
            }
        }
        return lista.isEmpty() ? null : lista;
    }

    private static GeneroPrenda genero(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return GeneroPrenda.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El género no es válido", "genero");
        }
    }

    private static BigDecimal numero(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            BigDecimal numero = new BigDecimal(valor.trim());
            if (numero.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "El precio no puede ser negativo", campo);
            }
            return numero;
        } catch (NumberFormatException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El precio no es un número válido", campo);
        }
    }

    private static Boolean disponibilidad(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        if (valor.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (valor.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "El filtro de disponibilidad debe ser true o false",
                "disponible");
    }

    private static Sort orden(String valor) {
        // Desempate por id: sin orden total la paginación devuelve filas
        // repetidas o saltadas entre páginas.
        Sort desempate = Sort.by("id");
        if (valor == null || valor.isBlank() || valor.equalsIgnoreCase("novedades")) {
            return Sort.by(Sort.Direction.DESC, "fechaActualizacion").and(desempate);
        }
        return switch (valor.trim()) {
            case "precioAsc" -> Sort.by(Sort.Direction.ASC, "precio").and(desempate);
            case "precioDesc" -> Sort.by(Sort.Direction.DESC, "precio").and(desempate);
            case "nombre" -> Sort.by(Sort.Direction.ASC, "nombre").and(desempate);
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El orden debe ser novedades, precioAsc, precioDesc o nombre", "orden");
        };
    }
}
