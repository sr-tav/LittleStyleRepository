package co.edu.uniquindio.littlestyle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;

@ConfigurationProperties(prefix = "app.recomendacion")
public record RecomendacionProperties (

    /** Meses tras los cuales las medidas se consideran desactualizadas. */
    @DefaultValue("6") int vigenciaMedidasMeses,
    /** Máximo de prendas devueltas en una consulta. */
    @DefaultValue("24") int maxResultados,
    /**
     * Estrategia de talla por defecto (nombre de {@code EstrategiaTalla}:
     * EXACTA, PROXIMIDAD, HOLGURA). Vacía usa el bean {@code @Primary}.
     */
    String estrategiaPorDefecto,
    /** Estrategia por categoría en mayúsculas (p. ej. VESTIDOS=PROXIMIDAD). La marca manda sobre esta. */
    Map<String, String> estrategiaPorCategoria,
    /** Estrategia por marca en mayúsculas. Manda sobre la categoría. */
    Map<String, String> estrategiaPorMarca
) {
}
