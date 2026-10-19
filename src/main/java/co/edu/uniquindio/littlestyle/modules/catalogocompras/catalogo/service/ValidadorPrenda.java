package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.ComponenteRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.TallaRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Reglas que cruzan varios campos y que Bean Validation no expresa: composición que suma 100 % sin
 * materiales repetidos, y tabla de tallas sin nombres repetidos ni rangos invertidos.
 */
@Component
public class ValidadorPrenda {

    /** Valida la composición y la tabla de tallas; lanza 400 con el campo del error. */
    public void validar(PrendaRequest request) {
        validarComposicion(request);
        validarTallas(request);
    }

    /** Nombre de talla canónico: sin espacios sobrantes y en mayúsculas ("xs " → "XS"). */
    public static String normalizarTalla(String talla) {
        return talla.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
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

    /** Exige tallas sin repetir y rangos de estatura, peso y edad con el mínimo menor o igual al máximo. */
    private void validarTallas(PrendaRequest request) {
        Set<String> nombres = new HashSet<>();
        for (TallaRequest talla : request.tallas()) {
            String nombre = normalizarTalla(talla.talla());
            if (!nombres.add(nombre)) {
                throw error("La talla " + nombre + " está repetida", "tallas");
            }
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
        }
    }

    /** Error 400 asociado a un campo. */
    private static BusinessException error(String mensaje, String campo) {
        return new BusinessException(HttpStatus.BAD_REQUEST, mensaje, campo);
    }
}
