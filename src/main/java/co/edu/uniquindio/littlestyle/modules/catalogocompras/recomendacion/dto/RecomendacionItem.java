package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.NivelRiesgo;

import java.util.List;

public record RecomendacionItem(Long prendaId, String nombre, String tallaSugerida, int puntaje, NivelRiesgo riesgo, List<String> advertencias, boolean tieneStock) {
}
