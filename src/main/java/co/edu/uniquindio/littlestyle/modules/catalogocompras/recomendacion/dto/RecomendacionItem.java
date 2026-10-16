package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.NivelRiesgo;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param puntaje compatibilidad 0-100: SOLO talla y seguridad (distancia,
 *                -30 si advierte, +5 si hay stock). Lo que mide RNF-11.
 * @param afinidad gusto 0-10 aparte: +5 color preferido, +5 estampado, con
 *                 tope. Solo reordena; nunca excluye ni compensa advertencias.
 */
public record RecomendacionItem(Long prendaId, String nombre, BigDecimal precio, String tallaSugerida, int puntaje, int afinidad, NivelRiesgo riesgo, List<String> advertencias, boolean tieneStock, String imagenUrl) {
}
