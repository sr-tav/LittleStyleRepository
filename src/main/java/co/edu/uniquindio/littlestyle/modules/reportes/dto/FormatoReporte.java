package co.edu.uniquindio.littlestyle.modules.reportes.dto;

import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum FormatoReporte {
    PDF("pdf", MediaType.APPLICATION_PDF);   // XLSX/CSV: agregar aquí + un Creador nuevo

    private final String extension;
    private final MediaType tipoMedia;

    public static FormatoReporte desde(String valor) {
        String v = valor == null ? "" : valor.trim();
        return Arrays.stream(values()).filter(f -> f.name().equalsIgnoreCase(v)).findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                        "Formato de exportación no soportado", "formato"));
    }
}