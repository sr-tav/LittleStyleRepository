package co.edu.uniquindio.littlestyle.modules.reportes.dto;

import java.util.List;
import java.util.Map;

public record ReporteDatos(TipoReporte tipo, String titulo, String nombreArchivo,
                           Map<String, Object> parametros, List<Map<String, ?>> filas) {
}