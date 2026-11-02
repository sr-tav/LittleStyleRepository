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
    /** Unidades vendidas por prenda y talla (demanda comercial). No confundir con BUSQUEDAS. */
    DEMANDA("demanda", "Reporte de demanda"),
    /**
     * Intención de búsqueda del catálogo: qué términos se buscaron, cuántas veces y cuántos no
     * arrojaron resultados. Es un reporte distinto de {@link #DEMANDA}, que mide unidades vendidas.
     */
    BUSQUEDAS("busquedas", "Reporte de búsquedas");

    private final String ruta;
    private final String titulo;

    public static TipoReporte desde(String valor) {
        String v = valor == null ? "" : valor.trim();
        return Arrays.stream(values()).filter(t -> t.ruta.equalsIgnoreCase(v)).findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                        "Tipo de reporte no válido. Usa: ventas, inventario, demanda o busquedas", "tipo"));
    }
}
