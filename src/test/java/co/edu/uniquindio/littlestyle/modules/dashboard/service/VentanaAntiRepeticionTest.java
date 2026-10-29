package co.edu.uniquindio.littlestyle.modules.dashboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La ventana cuenta un evento una sola vez por periodo y no acumula memoria indefinidamente. */
class VentanaAntiRepeticionTest {

    private RelojManual reloj;
    private VentanaAntiRepeticion ventana;

    @BeforeEach
    void setUp() {
        reloj = new RelojManual();
        ventana = new VentanaAntiRepeticion(Duration.ofMinutes(30), reloj);
    }

    @Test
    void laPrimeraVezCuenta() {
        assertThat(ventana.registrarSiNuevo("vista:1:10")).isTrue();
    }

    @Test
    void unaRepeticionDentroDeLaVentanaNoCuenta() {
        ventana.registrarSiNuevo("vista:1:10");
        reloj.avanzar(Duration.ofMinutes(5));

        assertThat(ventana.registrarSiNuevo("vista:1:10")).isFalse();
    }

    @Test
    void despuesDeLaVentanaVuelveAContar() {
        ventana.registrarSiNuevo("vista:1:10");
        reloj.avanzar(Duration.ofMinutes(30));

        assertThat(ventana.registrarSiNuevo("vista:1:10")).isTrue();
    }

    @Test
    void insistirNoAlargaLaVentana() {
        ventana.registrarSiNuevo("vista:1:10");
        reloj.avanzar(Duration.ofMinutes(20));
        assertThat(ventana.registrarSiNuevo("vista:1:10")).isFalse(); // repetición: no renueva la ventana
        reloj.avanzar(Duration.ofMinutes(11));                        // 31 min desde el primer evento

        assertThat(ventana.registrarSiNuevo("vista:1:10")).isTrue();
    }

    @Test
    void clientesYPrendasDistintasSonIndependientes() {
        assertThat(ventana.registrarSiNuevo("vista:1:10")).isTrue();
        assertThat(ventana.registrarSiNuevo("vista:2:10")).isTrue();  // otro cliente, misma prenda
        assertThat(ventana.registrarSiNuevo("vista:1:11")).isTrue();  // mismo cliente, otra prenda
        assertThat(ventana.registrarSiNuevo("busqueda:1:10")).isTrue(); // otro tipo de evento
    }

    @Test
    void descartaLosEventosVencidosParaNoCrecerSinLimite() {
        ventana.registrarSiNuevo("vista:1:10");
        ventana.registrarSiNuevo("vista:1:11");
        ventana.registrarSiNuevo("vista:1:12");
        assertThat(ventana.tamano()).isEqualTo(3);

        reloj.avanzar(Duration.ofMinutes(35));
        ventana.registrarSiNuevo("vista:9:99");

        assertThat(ventana.tamano()).isEqualTo(1);
    }

    @Test
    void rechazaUnaVentanaNulaONegativa() {
        assertThatThrownBy(() -> new VentanaAntiRepeticion(Duration.ZERO, reloj))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VentanaAntiRepeticion(Duration.ofMinutes(-1), reloj))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Reloj que solo avanza cuando la prueba lo pide. */
    static final class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-10-28T10:00:00Z");

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
