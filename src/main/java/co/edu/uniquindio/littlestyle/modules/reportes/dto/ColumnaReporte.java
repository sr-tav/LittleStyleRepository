package co.edu.uniquindio.littlestyle.modules.reportes.dto;

/**
 * Columna de la tabla cruda de un reporte.
 *
 * @param nombre encabezado visible para el usuario
 * @param tipo   tipo lógico que determina cómo se escribe el valor en XLSX/CSV
 */
public record ColumnaReporte(String nombre, TipoColumna tipo) {

    public ColumnaReporte {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la columna es obligatorio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de la columna es obligatorio");
        }
    }
}
