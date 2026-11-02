package co.edu.uniquindio.littlestyle.modules.reportes.dto;

import java.util.List;
import java.util.Map;

/**
 * Datos completos de un reporte listo para cualquier formato.
 *
 * <p>{@code filas} alimenta las plantillas JRXML (valores ya formateados, por ejemplo
 * {@code "COP $ 1.234"}). {@code tabla} mantiene los valores crudos y tipados que necesitan XLSX y
 * CSV para que los números sigan siendo números. Ambas vistas salen de la misma fuente de verdad
 * en {@code ConstructorDatosReporte}.</p>
 */
public record ReporteDatos(TipoReporte tipo, String titulo, String nombreArchivo,
                           Map<String, Object> parametros, List<Map<String, ?>> filas,
                           TablaReporte tabla) {

    /** Constructor histórico para el PDF y para pruebas que solo necesitan filas formateadas. */
    public ReporteDatos(TipoReporte tipo, String titulo, String nombreArchivo,
                        Map<String, Object> parametros, List<Map<String, ?>> filas) {
        this(tipo, titulo, nombreArchivo, parametros, filas, TablaReporte.vacia());
    }

    public ReporteDatos {
        filas = filas == null ? List.of() : List.copyOf(filas);
        tabla = tabla == null ? TablaReporte.vacia() : tabla;
    }
}
