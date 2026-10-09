package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaVitrinaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.shared.dto.PaginaResponse;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

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
        if (pagina < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La página no puede ser negativa", "pagina");
        }
        BigDecimal min = numero(precioMin, "precioMin");
        BigDecimal max = numero(precioMax, "precioMax");
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El precio mínimo no puede superar al máximo", "precioMin");
        }
        Pageable paginable = PageRequest.of(pagina, Math.min(Math.max(tamano, 1), TAMANO_MAXIMO), orden(orden));
        String q = texto == null || texto.isBlank() ? null : texto.trim();
        String t = talla == null || talla.isBlank() ? null : talla.trim();
        return PaginaResponse.from(prendaRepository.buscarVitrina(EstadoPrenda.ACTIVA, categoria(categoria),
                genero(genero), min, max, q, disponibilidad(disponible), material(material), t,
                paginable).map(PrendaVitrinaResponse::from));
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

    private static MaterialTextil material(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return MaterialTextil.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex ) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El material no es válido", "material");
        }
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
        if (valor == null || valor.isBlank() || valor.equalsIgnoreCase("novedades")) {
            return Sort.by(Sort.Direction.DESC, "fechaActualizacion");
        }
        return switch (valor.trim()) {
            case "precioAsc" -> Sort.by(Sort.Direction.ASC, "precio");
            case "precioDesc" -> Sort.by(Sort.Direction.DESC, "precio");
            case "nombre" -> Sort.by(Sort.Direction.ASC, "nombre");
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El orden debe ser novedades, precioAsc, precioDesc o nombre", "orden");
        };
    }
}
