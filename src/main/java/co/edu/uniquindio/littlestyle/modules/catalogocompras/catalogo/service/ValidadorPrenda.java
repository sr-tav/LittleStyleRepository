package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.ComponenteRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.TallaRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Reglas que cruzan varios campos y que Bean Validation no expresa: composición que suma 100 % sin
 * materiales repetidos, y tabla de tallas sin nombres repetidos ni rangos invertidos.
 */
@Component
public class ValidadorPrenda {

    private static final Pattern CON_LETRA = Pattern.compile("\\p{L}");

    /** Valida nombre, descripción, composición y tabla de tallas; lanza 400 con el campo del error. */
    public void validar(PrendaRequest request) {
        validarNombre(request);
        validarDescripcion(request);
        validarComposicion(request);
        validarEstampados(request);
        validarTallas(request);
    }

    /** Nombre de talla canónico: sin espacios sobrantes y en mayúsculas ("xs " → "XS"). */
    public static String normalizarTalla(String talla) {
        return talla.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    /** El nombre debe tener al menos una letra: no vale "12345" ni "---". */
    private void validarNombre(PrendaRequest request) {
        String nombre = request.nombre() == null ? "" : request.nombre().trim();
        if (!CON_LETRA.matcher(nombre).find()) {
            throw error("El nombre debe contener al menos una letra", "nombre");
        }
    }

    /** La descripción es obligatoria (también para quien llame al servicio sin pasar por Bean Validation). */
    private void validarDescripcion(PrendaRequest request) {
        if (request.descripcion() == null || request.descripcion().isBlank()) {
            throw error("La descripción es obligatoria", "descripcion");
        }
    }

    /** El vendedor debe declarar al menos un estampado; una prenda sin diseño se registra como «Liso». */
    private void validarEstampados(PrendaRequest request) {
        if (request.estampadosEfectivos().isEmpty()) {
            throw error("Selecciona al menos un estampado (elige «Liso» si la prenda no tiene)", "estampados");
        }
    }

    /** Exige materiales sin repetir que sumen exactamente 100 %. */
    private void validarComposicion(PrendaRequest request) {
        Set<MaterialTextil> materiales = EnumSet.noneOf(MaterialTextil.class);
        int suma = 0;
        for (ComponenteRequest componente : request.composicion()) {
            if (!materiales.add(componente.material())) {
                throw error("El material " + componente.material() + " está repetido en la composición", "composicion");
            }
            suma += componente.porcentaje();
        }
        if (suma != 100) {
            throw error("La composición textil debe sumar 100 % (suma " + suma + " %)", "composicion");
        }
    }

    /**
     * Valida las variantes (talla y color): sin repetir, con color declarado, cada color con al menos una
     * talla y la misma talla con los mismos rangos en todos sus colores.
     */
    private void validarTallas(PrendaRequest request) {
        Set<ColorPreferido> declarados = request.colores() == null
                ? EnumSet.noneOf(ColorPreferido.class) : request.colores();
        Set<String> claves = new HashSet<>();
        Set<ColorPreferido> usados = EnumSet.noneOf(ColorPreferido.class);
        Map<String, TallaRequest> primeraPorTalla = new HashMap<>();
        for (TallaRequest talla : request.tallas()) {
            String nombre = normalizarTalla(talla.talla());
            ColorPreferido color = colorDe(talla, request);
            if (!claves.add(nombre + "|" + color)) {
                throw error("La talla " + nombre + " en color " + color + " está repetida", "tallas");
            }
            if (!declarados.contains(color)) {
                throw error("El color " + color + " de la talla " + nombre + " no está entre los colores de la prenda", "tallas");
            }
            usados.add(color);
            if (talla.estaturaMinCm().compareTo(talla.estaturaMaxCm()) > 0) {
                throw error("En la talla " + nombre + " la estatura mínima supera a la máxima", "tallas");
            }
            if (talla.pesoMinKg().compareTo(talla.pesoMaxKg()) > 0) {
                throw error("En la talla " + nombre + " el peso mínimo supera al máximo", "tallas");
            }
            if (talla.edadMinMeses() != null && talla.edadMaxMeses() != null
                    && talla.edadMinMeses() > talla.edadMaxMeses()) {
                throw error("En la talla " + nombre + " la edad mínima supera a la máxima", "tallas");
            }
            TallaRequest previa = primeraPorTalla.putIfAbsent(nombre, talla);
            if (previa != null && !mismosRangos(previa, talla)) {
                throw error("La talla " + nombre + " debe tener los mismos rangos en todos sus colores", "tallas");
            }
        }
        if (!usados.containsAll(declarados)) {
            throw error("Cada color de la prenda debe tener al menos una talla", "colores");
        }
    }

    /** Color de la variante: el indicado o, si falta, el único color de la prenda. */
    public static ColorPreferido colorDe(TallaRequest talla, PrendaRequest request) {
        if (talla.color() != null) {
            return talla.color();
        }
        if (request.colores() != null && request.colores().size() == 1) {
            return request.colores().iterator().next();
        }
        throw error("Indica el color de cada talla cuando la prenda tiene varios colores", "tallas");
    }

    private static boolean mismosRangos(TallaRequest a, TallaRequest b) {
        return a.estaturaMinCm().compareTo(b.estaturaMinCm()) == 0
                && a.estaturaMaxCm().compareTo(b.estaturaMaxCm()) == 0
                && a.pesoMinKg().compareTo(b.pesoMinKg()) == 0
                && a.pesoMaxKg().compareTo(b.pesoMaxKg()) == 0
                && Objects.equals(a.edadMinMeses(), b.edadMinMeses())
                && Objects.equals(a.edadMaxMeses(), b.edadMaxMeses());
    }

    /** Error 400 asociado a un campo. */
    private static BusinessException error(String mensaje, String campo) {
        return new BusinessException(HttpStatus.BAD_REQUEST, mensaje, campo);
    }
}
