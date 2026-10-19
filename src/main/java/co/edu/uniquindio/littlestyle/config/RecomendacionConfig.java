package co.edu.uniquindio.littlestyle.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra las propiedades del motor de recomendación de tallas. */
@Configuration
@EnableConfigurationProperties(RecomendacionProperties.class)
public class RecomendacionConfig {
}
