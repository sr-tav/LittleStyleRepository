package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import co.edu.uniquindio.littlestyle.config.PagoProperties;
import co.edu.uniquindio.littlestyle.config.WompiProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.EventoWompiRequest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FirmaWompiServiceTest {

    /** Checksum calculado de forma independiente (sha256sum) para el evento de prueba. */
    private static final String CHECKSUM_ESPERADO =
            "177a142e25beca66d59f857f08b24ca96fb01b5cb39911a05940ea0cb8144a5d";

    private static final long TIMESTAMP = 1530291411L;
    /** Reloj fijo dos minutos después del evento de prueba: dentro de la vigencia de 10 minutos. */
    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(TIMESTAMP + 120), ZoneOffset.UTC);
    private static final WompiProperties WOMPI = new WompiProperties(
            "https://sandbox.wompi.co/v1", "pub_test_llave",
            "prod_integrity_Z5mMke9x0k8gpErbDqwrJXMqsI6SFli6", "test_events_secreto",
            "http://localhost:4200/cliente/pago/resultado", "COP");

    private final FirmaWompiService firma = servicio(WOMPI, CLOCK);

    @Test
    void generaLaFirmaDeIntegridadDocumentadaPorWompi() {
        assertThat(firma.firmaIntegridad("sk8-438k4-xmxm392-sn2m", 2490000, "COP"))
                .isEqualTo("37c8407747e595535433ef8f6a811d853cd943046624a0ec04662b17bbf33bf5");
    }

    @Test
    void noGeneraFirmaSinSecretoDeIntegridad() {
        FirmaWompiService sinSecreto = servicio(new WompiProperties(
                "https://sandbox.wompi.co/v1", "pub_test_llave", " ", "secreto", "url", "COP"), CLOCK);

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
        FirmaWompiService sinSecreto = servicio(new WompiProperties(
                "https://sandbox.wompi.co/v1", "pub", "integridad", null, "url", "COP"), CLOCK);

        assertThat(sinSecreto.eventoValido(evento(CHECKSUM_ESPERADO, "APPROVED", 1530291411L))).isFalse();
    }

    @Test
    void rechazaUnEventoAutenticoReutilizadoDespuesDeSuVigencia() {
        FirmaWompiService onceMinutosDespues = servicio(WOMPI,
                Clock.fixed(Instant.ofEpochSecond(TIMESTAMP + 11 * 60), ZoneOffset.UTC));

        assertThat(firma.eventoValido(evento(CHECKSUM_ESPERADO, "APPROVED", TIMESTAMP))).isTrue();
        assertThat(onceMinutosDespues.firmaCoincide(evento(CHECKSUM_ESPERADO, "APPROVED", TIMESTAMP))).isTrue();
        assertThat(onceMinutosDespues.eventoValido(evento(CHECKSUM_ESPERADO, "APPROVED", TIMESTAMP))).isFalse();
    }

    @Test
    void rechazaEventosFechadosEnElFuturoMasAllaDeLaTolerancia() {
        FirmaWompiService relojAtrasado = servicio(WOMPI,
                Clock.fixed(Instant.ofEpochSecond(TIMESTAMP - 5 * 60), ZoneOffset.UTC));

        assertThat(relojAtrasado.eventoValido(evento(CHECKSUM_ESPERADO, "APPROVED", TIMESTAMP))).isFalse();
    }

    @Test
    void aceptaElTimestampEnMilisegundosOComoFechaIso() {
        for (Object timestamp : List.of(TIMESTAMP * 1000, "2018-06-29T16:56:51Z", "2018-06-29T11:56:51-05:00",
                "2018-06-29T16:56:51", String.valueOf(TIMESTAMP))) {
            String texto = timestamp.toString();
            String checksum = FirmaWompiService.sha256(
                    "1234-1610641025-49201APPROVED4490000" + texto + "test_events_secreto");

            assertThat(firma.eventoValido(evento(checksum, "APPROVED", timestamp)))
                    .as("timestamp %s", texto).isTrue();
        }
    }

    @Test
    void interpretaLosFormatosDeFechaDelEvento() {
        Instant esperado = Instant.ofEpochSecond(TIMESTAMP);
        assertThat(FirmaWompiService.instanteEvento("1530291411")).isEqualTo(esperado);
        assertThat(FirmaWompiService.instanteEvento("1530291411000")).isEqualTo(esperado);
        assertThat(FirmaWompiService.instanteEvento("1530291411.0")).isEqualTo(esperado);
        assertThat(FirmaWompiService.instanteEvento("2018-06-29T16:56:51.000Z")).isEqualTo(esperado);
        assertThat(FirmaWompiService.instanteEvento("29/06/2018")).isNull();
        assertThat(FirmaWompiService.instanteEvento(" ")).isNull();
    }

    @Test
    void usaElTimestampTalComoLlegaParaLaFirma() {
        assertThat(evento("x", "APPROVED", 1530291411.0).timestampTexto()).isEqualTo("1530291411");
        assertThat(evento("x", "APPROVED", " 1530291411 ").timestampTexto()).isEqualTo("1530291411");
        assertThat(evento("x", "APPROVED", "").timestampTexto()).isNull();
    }

    private static FirmaWompiService servicio(WompiProperties wompi, Clock clock) {
        return new FirmaWompiService(wompi, PagoProperties.porDefecto(), clock);
    }

    private EventoWompiRequest evento(String checksum, String estado, Object timestamp) {
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
