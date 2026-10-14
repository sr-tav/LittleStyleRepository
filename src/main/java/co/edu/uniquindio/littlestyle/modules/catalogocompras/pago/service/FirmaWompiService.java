package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import co.edu.uniquindio.littlestyle.config.PagoProperties;
import co.edu.uniquindio.littlestyle.config.WompiProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.EventoWompiRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Firmas SHA-256 de Wompi: la firma de integridad que acompaña al Widget y la verificación del
 * checksum de los eventos del webhook, que evita aceptar confirmaciones falsificadas (RNF-07).
 * Además, un evento solo es válido durante {@link PagoProperties#vigenciaEvento()}: así un aviso
 * auténtico capturado no puede reenviarse después para alterar el estado de un pago.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirmaWompiService {

    /** A partir de este valor un timestamp numérico se interpreta en milisegundos (año 5138 en segundos). */
    private static final long UMBRAL_MILISEGUNDOS = 100_000_000_000L;

    private final WompiProperties wompiProperties;
    private final PagoProperties pagoProperties;
    private final Clock clock;

    /** SHA-256 de referencia + monto en centavos + moneda + secreto de integridad. */
    public String firmaIntegridad(String referencia, long montoCentavos, String moneda) {
        if (!wompiProperties.pagosConfigurados()) {
            throw new IllegalStateException("El secreto de integridad de Wompi no está configurado");
        }
        return sha256(referencia + montoCentavos + moneda + wompiProperties.secretoIntegridad());
    }

    /** Un evento es válido si su checksum coincide y su fecha está dentro de la vigencia configurada. */
    public boolean eventoValido(EventoWompiRequest evento) {
        if (!firmaCoincide(evento)) {
            return false;
        }
        Instant emitido = instanteEvento(evento.timestampTexto());
        if (emitido == null) {
            log.warn("Evento de Wompi con timestamp no reconocido: {}", evento.timestampTexto());
            return false;
        }
        Instant ahora = Instant.now(clock);
        if (emitido.isBefore(ahora.minus(pagoProperties.vigenciaEvento()))) {
            log.warn("Evento de Wompi vencido: emitido {} (vigencia {})", emitido, pagoProperties.vigenciaEvento());
            return false;
        }
        if (emitido.isAfter(ahora.plus(pagoProperties.toleranciaReloj()))) {
            log.warn("Evento de Wompi fechado en el futuro: {}", emitido);
            return false;
        }
        return true;
    }

    /**
     * Recalcula el checksum concatenando los valores de {@code signature.properties} dentro de
     * {@code data}, el {@code timestamp} y el secreto de eventos, y lo compara en tiempo constante.
     */
    boolean firmaCoincide(EventoWompiRequest evento) {
        if (!wompiProperties.eventosConfigurados() || evento == null || evento.data() == null
                || evento.signature() == null || evento.timestampTexto() == null) {
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
        cadena.append(evento.timestampTexto()).append(wompiProperties.secretoEventos());

        byte[] esperado = sha256(cadena.toString()).getBytes(StandardCharsets.US_ASCII);
        byte[] recibido = checksum.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(esperado, recibido);
    }

    /**
     * Interpreta el timestamp del evento: segundos o milisegundos Unix, o una fecha ISO-8601 con o sin
     * zona (sin zona se asume UTC). Devuelve {@code null} si no reconoce el formato.
     */
    static Instant instanteEvento(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            if (texto.matches("\\d+(\\.\\d+)?")) {
                long valor = Long.parseLong(texto.contains(".") ? texto.substring(0, texto.indexOf('.')) : texto);
                return valor >= UMBRAL_MILISEGUNDOS ? Instant.ofEpochMilli(valor) : Instant.ofEpochSecond(valor);
            }
            try {
                return OffsetDateTime.parse(texto).toInstant();
            } catch (DateTimeException sinZona) {
                return LocalDateTime.parse(texto).toInstant(ZoneOffset.UTC);
            }
        } catch (DateTimeException | NumberFormatException e) {
            return null;
        }
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
