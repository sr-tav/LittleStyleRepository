package co.edu.uniquindio.littlestyle.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({WompiProperties.class, PagoProperties.class})
public class WompiConfig {
}
