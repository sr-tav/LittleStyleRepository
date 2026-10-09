package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.NivelRiesgo;

import java.math.BigDecimal;
import java.util.List;

public record RecomendacionItem(Long prendaId, String nombre, BigDecimal precio, String tallaSugerida, int puntaje, NivelRiesgo riesgo, List<String> advertencias, boolean tieneStock, String imagenUrl) {
}
