package co.edu.uniquindio.littlestyle.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de las propiedades de Wompi (configuración del pasarela
 * de pagos), centradas en lo que se expone hacia afuera y hacia los logs.
 */
class WompiPropertiesTest {

    // Wompi rechaza dominios locales con 403: la URL de redirección solo es
    // válida si apunta a un dominio público bien formado
    @Test
    void omiteLaRedireccionLocalPorqueWompiLaRechazaCon403() {
        assertThat(conRedireccion("http://localhost:4200/cliente/pago/resultado").urlRedireccionPublica()).isNull();
        assertThat(conRedireccion("http://127.0.0.1:4200/cliente/pago/resultado").urlRedireccionPublica()).isNull();
        assertThat(conRedireccion("").urlRedireccionPublica()).isNull();
        assertThat(conRedireccion(null).urlRedireccionPublica()).isNull();
        assertThat(conRedireccion("no es una url").urlRedireccionPublica()).isNull();
    }

    // Con un dominio público real la URL se conserva tal cual
    @Test
    void conservaLaRedireccionConDominioPublico() {
        assertThat(conRedireccion("https://littlestyle.co/cliente/pago/resultado").urlRedireccionPublica())
                .isEqualTo("https://littlestyle.co/cliente/pago/resultado");
    }

    // El toString() nunca debe filtrar las llaves de integridad ni de eventos
    @Test
    void noExponeLosSecretosAlImprimirse() {
        assertThat(new WompiProperties("https://sandbox.wompi.co/v1", "pub_test_x", "test_integrity_secreto",
                "test_events_secreto", null, "COP").toString())
                .doesNotContain("test_integrity_secreto", "test_events_secreto");
    }

    // Helper para armar propiedades con distintas URLs de redirección
    private static WompiProperties conRedireccion(String url) {
        return new WompiProperties("https://sandbox.wompi.co/v1", "pub_test_x", "integridad", "eventos", url, "COP");
    }
}
