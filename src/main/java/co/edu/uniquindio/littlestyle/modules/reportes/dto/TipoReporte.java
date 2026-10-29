package co.edu.uniquindio.littlestyle.modules.reportes.dto;

import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum TipoReporte {
    VENTAS("ventas", "Reporte de ventas"),
    INVENTARIO("inventario", "Reporte de inventario"),
    DEMANDA("demanda", "Reporte de demanda");

    private final String ruta;
    private final String titulo;

    public static TipoReporte desde(String valor) {
        String v = valor == null ? "" : valor.trim();
        return Arrays.stream(values()).filter(t -> t.ruta.equalsIgnoreCase(v)).findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                        "Tipo de reporte no válido. Usa: ventas, inventario o demanda", "tipo"));
    }
}