package co.edu.uniquindio.littlestyle.modules.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class IntentosLoginServiceTest {

    private static final String EMAIL = "maria@correo.com";
    private static final String IP = "10.0.0.1";

    private RelojMutable reloj;
    private IntentosLoginService servicio;

    @BeforeEach
    void setUp() {
        reloj = new RelojMutable(Instant.parse("2026-01-01T10:00:00Z"));
        servicio = new IntentosLoginService(3, Duration.ofMinutes(15), Duration.ofMinutes(10), reloj);
    }

    @Test
    void bloqueaAlAlcanzarElMaximoDeFallos() {
        assertThat(servicio.registrarFallo(EMAIL, IP)).isFalse();
        assertThat(servicio.registrarFallo(EMAIL, IP)).isFalse();
        assertThat(servicio.bloqueoRestante(EMAIL, IP)).isEmpty();

        assertThat(servicio.registrarFallo(EMAIL, IP)).isTrue();
        assertThat(servicio.bloqueoRestante(EMAIL, IP)).contains(Duration.ofMinutes(10));
    }

    @Test
    void elBloqueoExpiraTrasElTiempoConfigurado() {
        for (int i = 0; i < 3; i++) {
            servicio.registrarFallo(EMAIL, IP);
        }
        reloj.avanzar(Duration.ofMinutes(10));

        assertThat(servicio.bloqueoRestante(EMAIL, IP)).isEmpty();
        // Tras el bloqueo el conteo empieza de nuevo
        assertThat(servicio.registrarFallo(EMAIL, IP)).isFalse();
    }

    @Test
    void losFallosFueraDeLaVentanaNoSeAcumulan() {
        servicio.registrarFallo(EMAIL, IP);
        servicio.registrarFallo(EMAIL, IP);
        reloj.avanzar(Duration.ofMinutes(15));

        assertThat(servicio.registrarFallo(EMAIL, IP)).isFalse();
        assertThat(servicio.bloqueoRestante(EMAIL, IP)).isEmpty();
    }

    @Test
    void unLoginExitosoReiniciaElConteo() {
        servicio.registrarFallo(EMAIL, IP);
        servicio.registrarFallo(EMAIL, IP);
        servicio.registrarExito(EMAIL, IP);

        assertThat(servicio.registrarFallo(EMAIL, IP)).isFalse();
    }

    @Test
    void elBloqueoNoAfectaAOtraIpNiAOtroCorreo() {
        for (int i = 0; i < 3; i++) {
            servicio.registrarFallo(EMAIL, IP);
        }

        assertThat(servicio.bloqueoRestante(EMAIL, "10.0.0.2")).isEmpty();
        assertThat(servicio.bloqueoRestante("otro@correo.com", IP)).isEmpty();
    }

    private static final class RelojMutable extends Clock {
        private Instant ahora;

        RelojMutable(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
