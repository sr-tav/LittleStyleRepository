package co.edu.uniquindio.littlestyle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.recomendacion")
public record RecomendacionProperties (

    /** Meses tras los cuales las medidas se consideran desactualizadas. */
    @DefaultValue("6") int vigenciaMedidasMeses,
    /** Máximo de prendas devueltas en una consulta. */
    @DefaultValue("24") int maxResultados
) {
}
