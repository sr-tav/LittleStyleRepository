package co.edu.uniquindio.littlestyle.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato CORS de las descargas (US-16): sin exponer {@code Content-Disposition} y
 * {@code Content-Length}, el navegador no puede nombrar el archivo ni calcular el progreso cuando
 * frontend y backend están en orígenes distintos.
 */
class SecurityConfigCorsTest {

    @Test
    void elFrontendPuedeLeerElNombreYElTamanoDelArchivo() {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", List.of("http://localhost:4200"));

        CorsConfiguration cors = config.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/reportes/ventas"));

        assertThat(cors).isNotNull();
        assertThat(cors.getExposedHeaders())
                .containsExactly("Content-Disposition", "Content-Length");
    }
}
