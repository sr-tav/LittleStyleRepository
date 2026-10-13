package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import co.edu.uniquindio.littlestyle.config.WompiProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.EventoWompiRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FirmaWompiServiceTest {

    /** Checksum calculado de forma independiente (sha256sum) para el evento de prueba. */
    private static final String CHECKSUM_ESPERADO =
            "177a142e25beca66d59f857f08b24ca96fb01b5cb39911a05940ea0cb8144a5d";

    private final FirmaWompiService firma = new FirmaWompiService(new WompiProperties(
            "https://sandbox.wompi.co/v1", "pub_test_llave",
            "prod_integrity_Z5mMke9x0k8gpErbDqwrJXMqsI6SFli6", "test_events_secreto",
            "http://localhost:4200/cliente/pago/resultado", "COP"));

    @Test
    void generaLaFirmaDeIntegridadDocumentadaPorWompi() {
        assertThat(firma.firmaIntegridad("sk8-438k4-xmxm392-sn2m", 2490000, "COP"))
                .isEqualTo("37c8407747e595535433ef8f6a811d853cd943046624a0ec04662b17bbf33bf5");
    }

    @Test
    void noGeneraFirmaSinSecretoDeIntegridad() {
        FirmaWompiService sinSecreto = new FirmaWompiService(new WompiProperties(
                "https://sandbox.wompi.co/v1", "pub_test_llave", " ", "secreto", "url", "COP"));

        assertThatThrownBy(() -> sinSecreto.firmaIntegridad("ref", 100, "COP"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aceptaEventoConChecksumValidoSinImportarMayusculas() {
        assertThat(firma.eventoValido(evento(CHECKSUM_ESPERADO, "APPROVED", 1530291411L))).isTrue();
        assertThat(firma.eventoValido(evento(CHECKSUM_ESPERADO.toUpperCase(), "APPROVED", 1530291411L)))
                .isTrue();
    }

    @Test
    void rechazaEventoAlteradoOConTimestampDistinto() {
        assertThat(firma.eventoValido(evento(CHECKSUM_ESPERADO, "DECLINED", 1530291411L))).isFalse();
        assertThat(firma.eventoValido(evento(CHECKSUM_ESPERADO, "APPROVED", 1530291412L))).isFalse();
        assertThat(firma.eventoValido(evento("abc", "APPROVED", 1530291411L))).isFalse();
    }

    @Test
    void rechazaEventosIncompletos() {
        assertThat(firma.eventoValido(null)).isFalse();
        assertThat(firma.eventoValido(new EventoWompiRequest("transaction.updated",
                Map.of("transaction", Map.of("id", "1")), "test", null, 1L))).isFalse();
        assertThat(firma.eventoValido(new EventoWompiRequest("transaction.updated",
                Map.of("transaction", Map.of("id", "1")), "test",
                new EventoWompiRequest.FirmaEvento(List.of("transaction.no_existe"), CHECKSUM_ESPERADO),
                1530291411L))).isFalse();
        assertThat(firma.eventoValido(new EventoWompiRequest("transaction.updated",
                Map.of("transaction", Map.of("id", "1")), "test",
                new EventoWompiRequest.FirmaEvento(List.of(), CHECKSUM_ESPERADO), 1530291411L))).isFalse();
    }

    @Test
    void rechazaEventosSiNoHaySecretoDeEventosConfigurado() {
        FirmaWompiService sinSecreto = new FirmaWompiService(new WompiProperties(
                "https://sandbox.wompi.co/v1", "pub", "integridad", null, "url", "COP"));

        assertThat(sinSecreto.eventoValido(evento(CHECKSUM_ESPERADO, "APPROVED", 1530291411L))).isFalse();
    }

    private EventoWompiRequest evento(String checksum, String estado, long timestamp) {
        Map<String, Object> transaccion = Map.of(
                "id", "1234-1610641025-49201",
                "status", estado,
                "amount_in_cents", 4490000,
                "reference", "LS-1-ABC",
                "currency", "COP");
        return new EventoWompiRequest("transaction.updated", Map.of("transaction", transaccion), "test",
                new EventoWompiRequest.FirmaEvento(
                        List.of("transaction.id", "transaction.status", "transaction.amount_in_cents"), checksum),
                timestamp);
    }
}
