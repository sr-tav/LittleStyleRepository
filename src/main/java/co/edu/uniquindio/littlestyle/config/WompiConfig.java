package co.edu.uniquindio.littlestyle.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra las propiedades de la pasarela Wompi y del procesamiento de pagos. */
@Configuration
@EnableConfigurationProperties({WompiProperties.class, PagoProperties.class})
public class WompiConfig {
}
