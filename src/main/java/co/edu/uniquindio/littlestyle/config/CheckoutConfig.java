package co.edu.uniquindio.littlestyle.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/** Habilita la configuración del checkout y las tareas programadas (p. ej. la expiración de pedidos). */
@Configuration
@EnableConfigurationProperties(CheckoutProperties.class)
@EnableScheduling
public class CheckoutConfig {

    /** Reloj del sistema usado por el checkout; se inyecta para poder fijar la hora en las pruebas. */
    @Bean
    public Clock checkoutClock() {
        return Clock.systemDefaultZone();
    }
}
