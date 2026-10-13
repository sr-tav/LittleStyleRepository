package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import co.edu.uniquindio.littlestyle.config.WompiProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.TransaccionWompi;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Cliente HTTP de la API de Wompi. Consulta el estado real de una transacción para confirmar
 * pagos sin depender únicamente del webhook (útil en desarrollo sin URL pública).
 */
@Slf4j
@Component
public class WompiClient {

    private static final ParameterizedTypeReference<Map<String, Object>> RESPUESTA =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public WompiClient(WompiProperties wompiProperties) {
        this.restClient = RestClient.builder().baseUrl(wompiProperties.apiUrl()).build();
    }

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
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("No fue posible consultar la transacción {} en Wompi: {}", transaccionId, e.getMessage());
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "No fue posible consultar el estado del pago. Intenta de nuevo en unos segundos.");
        }
    }
}
