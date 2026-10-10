package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.config.CheckoutProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TarifaEnvioServiceTest {

    private final TarifaEnvioService service = new TarifaEnvioService(new CheckoutProperties(
            new BigDecimal("7000"), new BigDecimal("10000"), new BigDecimal("15000"),
            List.of("CALARCA", "LA TEBAIDA")));

    @Test
    void calculaTarifaDeArmeniaSinImportarMayusculas() {
        assertThat(service.calcular("Quindío", " armenia ")).isEqualByComparingTo("7000");
    }

    @Test
    void calculaTarifaDeMunicipioConfiguradoAunqueTengaTilde() {
        assertThat(service.calcular("QUINDIO", "La Tebaída")).isEqualByComparingTo("10000");
    }

    @Test
    void calculaTarifaGeneralParaOtrasCiudades() {
        assertThat(service.calcular("Cundinamarca", "Bogotá")).isEqualByComparingTo("15000");
    }

    @Test
    void noClasificaUnaCiudadConElMismoNombreDeArmeniaComoQuindioSiCambiaElDepartamento() {
        assertThat(service.calcular("Antioquia", "Armenia")).isEqualByComparingTo("15000");
    }
}
