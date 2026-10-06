package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.storage;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formatos de imagen aceptados. El tipo se detecta por la firma del archivo (magic bytes) y no por el
 * nombre ni por el Content-Type que envía el navegador, que el cliente puede falsear.
 */
public enum TipoImagen {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    private final String contentType;
    private final String extension;

    TipoImagen(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    public static Optional<TipoImagen> detectar(byte[] datos) {
        return Arrays.stream(values()).filter(tipo -> tipo.coincide(datos)).findFirst();
    }

    private boolean coincide(byte[] d) {
        return switch (this) {
            case JPEG -> d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF;
            case PNG -> d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                    && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A;
            case WEBP -> d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                    && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P';
        };
    }
}
