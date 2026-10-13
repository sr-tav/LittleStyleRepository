package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import co.edu.uniquindio.littlestyle.config.WompiProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.EventoWompiRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Firmas SHA-256 de Wompi: la firma de integridad que acompaña al Widget y la verificación del
 * checksum de los eventos del webhook, que evita aceptar confirmaciones falsificadas.
 */
@Service
@RequiredArgsConstructor
public class FirmaWompiService {

    private final WompiProperties wompiProperties;

    /** SHA-256 de referencia + monto en centavos + moneda + secreto de integridad. */
    public String firmaIntegridad(String referencia, long montoCentavos, String moneda) {
        if (!wompiProperties.pagosConfigurados()) {
            throw new IllegalStateException("El secreto de integridad de Wompi no está configurado");
        }
        return sha256(referencia + montoCentavos + moneda + wompiProperties.secretoIntegridad());
    }

    /**
     * Recalcula el checksum concatenando los valores de {@code signature.properties} dentro de
     * {@code data}, el {@code timestamp} y el secreto de eventos, y lo compara en tiempo constante.
     */
    public boolean eventoValido(EventoWompiRequest evento) {
        if (!wompiProperties.eventosConfigurados() || evento == null || evento.data() == null
                || evento.signature() == null || evento.timestamp() == null) {
            return false;
        }
        List<String> propiedades = evento.signature().properties();
        String checksum = evento.signature().checksum();
        if (propiedades == null || propiedades.isEmpty() || checksum == null || checksum.isBlank()) {
            return false;
        }

        StringBuilder cadena = new StringBuilder();
        for (String propiedad : propiedades) {
            Object valor = valorEnRuta(evento.data(), propiedad);
            if (valor == null) {
                return false;
            }
            cadena.append(valor);
        }
        cadena.append(evento.timestamp()).append(wompiProperties.secretoEventos());

        byte[] esperado = sha256(cadena.toString()).getBytes(StandardCharsets.US_ASCII);
        byte[] recibido = checksum.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(esperado, recibido);
    }

    private Object valorEnRuta(Map<String, Object> data, String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return null;
        }
        Object actual = data;
        for (String parte : ruta.split("\\.")) {
            if (!(actual instanceof Map<?, ?> mapa)) {
                return null;
            }
            actual = mapa.get(parte);
        }
        return actual instanceof Map<?, ?> || actual instanceof List<?> ? null : actual;
    }

    static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}
