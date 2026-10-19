package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.config.CheckoutProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del cálculo de la tarifa de envío según la ciudad configurada en
 * CheckoutProperties, incluyendo reglas de normalización (mayúsculas y tildes).
 * Todo se prueba con una configuración fija, sin base de datos.
 */
class TarifaEnvioServiceTest {

    // Servicio con tarifas de ejemplo: local (Armenia), municipios cercanos y tarifa general.
    private final TarifaEnvioService service = new TarifaEnvioService(new CheckoutProperties(
            new BigDecimal("7000"), new BigDecimal("10000"), new BigDecimal("15000"),
            List.of("CALARCA", "LA TEBAIDA"), Duration.ofMinutes(30)));

    // Armenia aplica la tarifa local aunque la ciudad llegue con espacios o minúsculas.
    @Test
    void calculaTarifaDeArmeniaSinImportarMayusculas() {
        assertThat(service.calcular("Quindío", " armenia ")).isEqualByComparingTo("7000");
    }

    // Los municipios con tarifa especial se reconocen aunque el nombre traiga tildes o mayúsculas.
    @Test
    void calculaTarifaDeMunicipioConfiguradoAunqueTengaTilde() {
        assertThat(service.calcular("QUINDIO", "La Tebaída")).isEqualByComparingTo("10000");
    }

    // Cualquier destino fuera de la zona local y los municipios configurados paga la tarifa general.
    @Test
    void calculaTarifaGeneralParaOtrasCiudades() {
        assertThat(service.calcular("Cundinamarca", "Bogotá")).isEqualByComparingTo("15000");
    }

    // El nombre de la ciudad por sí solo no basta: Armenia de Antioquia paga tarifa general,
    // porque la zona local solo cubre el departamento de Quindío.
    @Test
    void noClasificaUnaCiudadConElMismoNombreDeArmeniaComoQuindioSiCambiaElDepartamento() {
        assertThat(service.calcular("Antioquia", "Armenia")).isEqualByComparingTo("15000");
    }
}
