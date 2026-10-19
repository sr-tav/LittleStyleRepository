package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import co.edu.uniquindio.littlestyle.config.PagoProperties;
import co.edu.uniquindio.littlestyle.config.WompiProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.TransaccionWompi;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Map;

/**
 * Cliente HTTP de la API de Wompi. Consulta el estado real de una transacción para confirmar
 * pagos sin depender únicamente del webhook (útil en desarrollo sin URL pública).
 * Cada consulta tiene tiempos máximos de conexión y lectura (RNF-28): si Wompi no responde a
 * tiempo se informa al cliente que intente más tarde y el pago local queda intacto.
 */
@Slf4j
@Component
public class WompiClient {

    static final String MENSAJE_SIN_RESPUESTA =
            "Wompi está tardando en responder. Intenta más tarde; tu pedido se conserva sin cambios.";

    private static final ParameterizedTypeReference<Map<String, Object>> RESPUESTA =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    /** Cliente HTTP con la URL de la API y los tiempos de conexión y lectura configurados. */
    public WompiClient(WompiProperties wompiProperties, PagoProperties pagoProperties) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(pagoProperties.timeoutConexion());
        fabrica.setReadTimeout(pagoProperties.timeoutLectura());
        this.restClient = RestClient.builder()
                .baseUrl(wompiProperties.apiUrl())
                .requestFactory(fabrica)
                .build();
    }

    /**
     * Consulta una transacción en Wompi. Traduce los fallos a errores de negocio: 404 si no existe, 504 si
     * Wompi no responde a tiempo, 503 si no hay conexión y 502 ante respuestas inválidas.
     */
    public TransaccionWompi consultarTransaccion(String transaccionId) {
        try {
            Map<String, Object> respuesta = restClient.get()
                    .uri("/transactions/{id}", transaccionId)
                    .retrieve()
                    .body(RESPUESTA);
            if (respuesta == null || respuesta.get("data") == null) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY,
                        "Wompi no devolvió la información de la transacción");
            }
            return TransaccionWompi.desdeMapa(respuesta.get("data"));
        } catch (HttpClientErrorException.NotFound e) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Transacción de pago no encontrada");
        } catch (ResourceAccessException e) {
            if (esTiempoAgotado(e)) {
                log.warn("Wompi no respondió a tiempo al consultar la transacción {}", transaccionId);
                throw new BusinessException(HttpStatus.GATEWAY_TIMEOUT, MENSAJE_SIN_RESPUESTA);
            }
            log.warn("No fue posible conectar con Wompi para la transacción {}: {}", transaccionId, e.getMessage());
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No fue posible comunicarse con Wompi. Intenta más tarde; tu pedido se conserva sin cambios.");
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("No fue posible consultar la transacción {} en Wompi: {}", transaccionId, e.getMessage());
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "No fue posible consultar el estado del pago. Intenta de nuevo en unos segundos.");
        }
    }

    /** Indica si alguna causa del error es un tiempo de espera agotado. */
    private static boolean esTiempoAgotado(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof SocketTimeoutException || causa instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
