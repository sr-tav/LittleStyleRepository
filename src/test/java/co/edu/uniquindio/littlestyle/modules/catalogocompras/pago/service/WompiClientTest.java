package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import co.edu.uniquindio.littlestyle.config.PagoProperties;
import co.edu.uniquindio.littlestyle.config.WompiProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.TransaccionWompi;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba los tiempos límite contra un servidor HTTP local que simula la API de Wompi. */
class WompiClientTest {

    private static final String TRANSACCION = """
            {"data":{"id":"tx-1","status":"APPROVED","reference":"LS-1-A","amount_in_cents":100,
              "currency":"COP","payment_method_type":"CARD"}}
            """;

    private HttpServer servidor;

    @AfterEach
    void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void siWompiNoRespondeATiempoAvisaQueSeIntenteMasTarde() throws IOException {
        iniciarServidor(2000);
        WompiClient cliente = cliente(Duration.ofMillis(300));

        long inicio = System.nanoTime();
        assertThatThrownBy(() -> cliente.consultarTransaccion("tx-1"))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
                    assertThat(e.getMessage()).contains("Intenta más tarde");
                });
        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofMillis(1500));
    }

    @Test
    void consultaLaTransaccionCuandoWompiRespondeDentroDelPlazo() throws IOException {
        iniciarServidor(0);

        TransaccionWompi transaccion = cliente(Duration.ofSeconds(2)).consultarTransaccion("tx-1");

        assertThat(transaccion.estado()).isEqualTo("APPROVED");
        assertThat(transaccion.referencia()).isEqualTo("LS-1-A");
    }

    @Test
    void siWompiNoEstaDisponibleAvisaQueSeIntenteMasTarde() throws IOException {
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoLibre = socket.getLocalPort();
        }
        WompiClient cliente = new WompiClient(propiedadesWompi("http://127.0.0.1:" + puertoLibre + "/v1"),
                propiedadesPago(Duration.ofSeconds(1)));

        assertThatThrownBy(() -> cliente.consultarTransaccion("tx-1"))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getMessage()).contains("Intenta más tarde");
                });
    }

    private void iniciarServidor(long demoraMs) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1/transactions/", intercambio -> {
            try {
                Thread.sleep(demoraMs);
                byte[] cuerpo = TRANSACCION.getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().add("Content-Type", "application/json");
                intercambio.sendResponseHeaders(200, cuerpo.length);
                intercambio.getResponseBody().write(cuerpo);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException ignorada) {
                // El cliente cerró la conexión al agotar su tiempo de espera
            } finally {
                intercambio.close();
            }
        });
        servidor.start();
    }

    private WompiClient cliente(Duration timeoutLectura) {
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1";
        return new WompiClient(propiedadesWompi(url), propiedadesPago(timeoutLectura));
    }

    private static WompiProperties propiedadesWompi(String url) {
        return new WompiProperties(url, "pub_test", "integridad", "eventos", "", "COP");
    }

    private static PagoProperties propiedadesPago(Duration timeoutLectura) {
        PagoProperties base = PagoProperties.porDefecto();
        return new PagoProperties(base.vigenciaEvento(), base.toleranciaReloj(), Duration.ofSeconds(1),
                timeoutLectura, base.maxIntentosPorPedido(), true, 1, 1, base.expiracionTransaccion());
    }
}
