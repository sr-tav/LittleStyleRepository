package co.edu.uniquindio.littlestyle.modules.reportes.dto;

/**
 * Tipo lógico de una columna de la tabla cruda de un reporte.
 *
 * <p>Los formatos interoperables (XLSX y CSV) escriben el valor real según este tipo: un
 * {@link #MONEDA} nunca viaja como texto con símbolo de pesos, de modo que una hoja de cálculo
 * pueda sumarlo. El PDF sigue usando las filas ya formateadas.</p>
 */
public enum TipoColumna {
    TEXTO,
    ENTERO,
    MONEDA,
    FECHA
}
