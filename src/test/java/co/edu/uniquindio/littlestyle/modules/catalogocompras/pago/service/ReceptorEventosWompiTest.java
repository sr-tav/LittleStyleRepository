package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import co.edu.uniquindio.littlestyle.config.PagoProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.EventoWompiRequest;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Suite para el receptor de eventos de Wompi: cubre la respuesta asíncrona,
 * el procesamiento en paralelo, el rechazo de la cola llena, la validación
 * de firmas y la política de reintentos. Se mockea PagoService para controlar
 * cada escenario con CountDownLatch.
 */
class ReceptorEventosWompiTest {

    private final PagoService pagoService = mock(PagoService.class);
    // Latch que permite pausar la aplicación del evento y coordinar los hilos
    private final CountDownLatch liberar = new CountDownLatch(1);
    private ReceptorEventosWompi receptor;

    // Libera los hilos detenidos y apaga el receptor para no dejar hilos vivos
    @AfterEach
    void cerrar() {
        liberar.countDown();
        if (receptor != null) {
            receptor.destroy();
        }
    }

    // La respuesta al webhook debe ser inmediata: el evento se aplica por fuera
    // de la petición, sin bloquear el HTTP más de un segundo
    @Test
    void respondeSinEsperarAQueSeApliqueElEvento() throws Exception {
        receptor = new ReceptorEventosWompi(pagoService, propiedades(2, 10));
        CountDownLatch aplicado = new CountDownLatch(1);
        doAnswer(invocacion -> {
            liberar.await(5, TimeUnit.SECONDS);
            aplicado.countDown();
            return null;
        }).when(pagoService).aplicarEvento(any());

        long inicio = System.nanoTime();
        receptor.recibir(evento());
        long duracionMs = Duration.ofNanos(System.nanoTime() - inicio).toMillis();

        assertThat(duracionMs).isLessThan(1000);
        assertThat(aplicado.getCount()).isEqualTo(1);
        liberar.countDown();
        assertThat(aplicado.await(5, TimeUnit.SECONDS)).isTrue();
    }

    // Varios webhooks llegan a la vez y todos deben ejecutarse concurrentemente
    // gracias al pool de hilos, sin procesarse uno detrás de otro
    @Test
    void variosAvisosSimultaneosSeAplicanEnParaleloYNoEnFila() throws Exception {
        receptor = new ReceptorEventosWompi(pagoService, propiedades(3, 10));
        CountDownLatch enCurso = new CountDownLatch(3);
        doAnswer(invocacion -> {
            enCurso.countDown();
            liberar.await(5, TimeUnit.SECONDS);
            return null;
        }).when(pagoService).aplicarEvento(any());

        receptor.recibir(evento());
        receptor.recibir(evento());
        receptor.recibir(evento());

        // Los tres eventos se aplican al mismo tiempo: ninguno espera a que termine otro
        assertThat(enCurso.await(5, TimeUnit.SECONDS)).isTrue();
    }

    // Wompi reintenta cuando recibe 503: si la cola está llena, el receptor
    // responde SERVICE_UNAVAILABLE en lugar de descartar el evento
    @Test
    void siLaColaSeLlenaRespondeServicioNoDisponibleParaQueWompiReintente() {
        receptor = new ReceptorEventosWompi(pagoService, propiedades(1, 1));
        doAnswer(invocacion -> {
            liberar.await(5, TimeUnit.SECONDS);
            return null;
        }).when(pagoService).aplicarEvento(any());

        receptor.recibir(evento());
        verify(pagoService, timeout(2000)).aplicarEvento(any());
        receptor.recibir(evento());

        assertThatThrownBy(() -> receptor.recibir(evento()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    // Un evento con firma inválida se rechaza con 401 y nunca se llega a aplicar
    @Test
    void unAvisoInvalidoSeRechazaSinAplicarse() {
        receptor = new ReceptorEventosWompi(pagoService, propiedades(1, 1));
        doThrow(new BusinessException(HttpStatus.UNAUTHORIZED, "Firma del evento inválida o vencida"))
                .when(pagoService).validarEvento(any());

        assertThatThrownBy(() -> receptor.recibir(evento()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(pagoService, never()).aplicarEvento(any());
    }

    // Si aplicar el evento falla por algo transitorio, se reintenta hasta que
    // una pasada tiene éxito
    @Test
    void reintentaAplicarElEventoAnteFallosTemporales() {
        receptor = new ReceptorEventosWompi(pagoService, propiedades(1, 1));
        doThrow(new IllegalStateException("bloqueo"))
                .doNothing()
                .when(pagoService).aplicarEvento(any());

        receptor.recibir(evento());

        verify(pagoService, timeout(3000).times(2)).aplicarEvento(any());
    }

    // Los reintentos tienen tope: se verifica que se ejecute exactamente
    // MAX_REINTENTOS veces y que después no siga intentando
    @Test
    void noReintentaMasDeLoPermitido() throws Exception {
        receptor = new ReceptorEventosWompi(pagoService, propiedades(1, 1));
        doThrow(new IllegalStateException("caído")).when(pagoService).aplicarEvento(any());

        receptor.recibir(evento());

        verify(pagoService, timeout(5000).times(ReceptorEventosWompi.MAX_REINTENTOS)).aplicarEvento(any());
        Thread.sleep(800);
        verify(pagoService, times(ReceptorEventosWompi.MAX_REINTENTOS)).aplicarEvento(any());
    }

    // Configura las propiedades de Wompi con los hilos o la cola que pida el test
    private static PagoProperties propiedades(int hilos, int cola) {
        PagoProperties base = PagoProperties.porDefecto();
        return new PagoProperties(base.vigenciaEvento(), base.toleranciaReloj(), base.timeoutConexion(),
                base.timeoutLectura(), base.maxIntentosPorPedido(), true, hilos, cola,
                base.expiracionTransaccion());
    }

    // Evento genérico de transacción con firma de ejemplo, listo para usar
    private static EventoWompiRequest evento() {
        return new EventoWompiRequest("transaction.updated", Map.of("transaction", Map.of("id", "1")), "test",
                new EventoWompiRequest.FirmaEvento(List.of("transaction.id"), "checksum"), 1L);
    }
}
