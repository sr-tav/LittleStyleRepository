package co.edu.uniquindio.littlestyle.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(CheckoutProperties.class)
@EnableScheduling
public class CheckoutConfig {

    @Bean
    public Clock checkoutClock() {
        return Clock.systemDefaultZone();
    }
}
