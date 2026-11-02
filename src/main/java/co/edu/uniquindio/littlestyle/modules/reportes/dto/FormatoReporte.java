package co.edu.uniquindio.littlestyle.modules.reportes.dto;

import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Formatos de exportación soportados (patrón Factory Method).
 *
 * <p>Cada valor nuevo exige un {@code CreadorGeneradorX} y un {@code GeneradorReporteX}; el
 * controlador y el servicio nunca deciden por formato con un {@code switch}.</p>
 */
@Getter
@RequiredArgsConstructor
public enum FormatoReporte {
    PDF("pdf", MediaType.APPLICATION_PDF),
    XLSX("xlsx", MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
    CSV("csv", new MediaType("text", "csv", StandardCharsets.UTF_8));

    private final String extension;
    private final MediaType tipoMedia;

    /**
     * Interpreta el valor recibido por query param sin distinguir mayúsculas. También acepta la
     * extensión ({@code pdf}, {@code xlsx}, {@code csv}) como alias del nombre del formato.
     */
    public static FormatoReporte desde(String valor) {
        String v = valor == null ? "" : valor.trim();
        return Arrays.stream(values())
                .filter(f -> f.name().equalsIgnoreCase(v) || f.extension.equalsIgnoreCase(v))
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                        "Formato de exportación no soportado. Usa: PDF, XLSX o CSV", "formato"));
    }
}
