package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.ComponenteRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.TallaRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.MaterialTextil;
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

    public void validar(PrendaRequest request) {
        validarComposicion(request);
        validarTallas(request);
    }

    /** Nombre de talla canónico: sin espacios sobrantes y en mayúsculas ("xs " → "XS"). */
    public static String normalizarTalla(String talla) {
        return talla.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

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
        }
    }

    private static BusinessException error(String mensaje, String campo) {
        return new BusinessException(HttpStatus.BAD_REQUEST, mensaje, campo);
    }
}
