package co.edu.uniquindio.littlestyle.modules.reportes.factory;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.ColumnaReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.FormatoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ReporteDatos;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TablaReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TipoColumna;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Genera reportes CSV listos para Excel en configuración regional es-CO (RNF-24).
 *
 * <p>Incluye BOM UTF-8 para que Excel respete tildes y eñes, usa punto y coma como separador,
 * termina las líneas en CRLF y aplica el escape RFC 4180. Los valores son los mismos crudos del
 * XLSX: decimales con punto, sin símbolo de moneda y fechas ISO.</p>
 */
public class GeneradorReporteCsv implements GeneradorReporte {

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String SEPARADOR = ";";
    private static final String FIN_LINEA = "\r\n";

    @Override
    public FormatoReporte formato() {
        return FormatoReporte.CSV;
    }

    @Override
    public byte[] generar(ReporteDatos datos) {
        TablaReporte tabla = datos.tabla();
        StringBuilder csv = new StringBuilder();

        List<ColumnaReporte> columnas = tabla.columnas();
        for (int i = 0; i < columnas.size(); i++) {
            if (i > 0) {
                csv.append(SEPARADOR);
            }
            csv.append(escapar(ValoresReporte.texto(columnas.get(i).nombre())));
        }
        csv.append(FIN_LINEA);

        for (List<Object> fila : tabla.filas()) {
            for (int i = 0; i < columnas.size(); i++) {
                if (i > 0) {
                    csv.append(SEPARADOR);
                }
                Object valor = i < fila.size() ? fila.get(i) : null;
                csv.append(escapar(valorCrudo(valor, columnas.get(i).tipo())));
            }
            csv.append(FIN_LINEA);
        }

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.writeBytes(BOM);
        salida.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
        return salida.toByteArray();
    }

    private static String valorCrudo(Object valor, TipoColumna tipo) {
        if (valor == null) {
            return "";
        }
        return switch (tipo) {
            case ENTERO -> valor instanceof Number n ? String.valueOf(n.longValue()) : ValoresReporte.texto(String.valueOf(valor));
            case MONEDA -> {
                BigDecimal numero = ValoresReporte.aDecimal(valor);
                yield numero == null ? "" : ValoresReporte.numero(numero);
            }
            case FECHA -> {
                LocalDate fecha = ValoresReporte.aFecha(valor);
                yield fecha == null ? "" : ValoresReporte.fecha(fecha);
            }
            default -> ValoresReporte.texto(String.valueOf(valor));
        };
    }

    /** Escape RFC 4180: duplica comillas y encierra cuando hay separador, comilla o salto de línea. */
    static String escapar(String valor) {
        String texto = valor == null ? "" : valor;
        boolean requiereComillas = texto.indexOf(SEPARADOR.charAt(0)) >= 0
                || texto.indexOf('"') >= 0
                || texto.indexOf('\n') >= 0
                || texto.indexOf('\r') >= 0;
        if (!requiereComillas) {
            return texto;
        }
        return '"' + texto.replace("\"", "\"\"") + '"';
    }
}
