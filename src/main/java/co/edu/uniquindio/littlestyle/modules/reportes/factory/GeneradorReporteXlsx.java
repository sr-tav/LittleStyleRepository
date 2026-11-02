package co.edu.uniquindio.littlestyle.modules.reportes.factory;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.ColumnaReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.FormatoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ReporteDatos;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TablaReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.TipoColumna;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Genera reportes XLSX reales con Apache POI (RNF-24).
 *
 * <p>La hoja {@code Datos} conserva el tipo de cada celda: enteros y moneda son números con
 * formato, las fechas son fechas y solo los textos son texto. La hoja {@code Resumen} replica los
 * parámetros del PDF (título, alcance, autor, fecha y totales). Se usa {@link SXSSFWorkbook} con
 * ventana de 100 filas para que un reporte grande no consuma memoria proporcional al archivo.</p>
 */
public class GeneradorReporteXlsx implements GeneradorReporte {

    private static final int VENTANA_FILAS = 100;

    @Override
    public FormatoReporte formato() {
        return FormatoReporte.XLSX;
    }

    @Override
    public byte[] generar(ReporteDatos datos) {
        SXSSFWorkbook libro = new SXSSFWorkbook(VENTANA_FILAS);
        libro.setCompressTempFiles(true);
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Estilos estilos = new Estilos(libro);
            hojaDatos(libro, datos, estilos);
            hojaResumen(libro, datos, estilos);
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new ReporteGeneracionException("Fallo al escribir el XLSX de " + datos.tipo(), e);
        } finally {
            try {
                libro.close();
            } catch (IOException ignorado) {
                // El archivo ya se escribió; cerrar la ventana temporal no debe tapar ese resultado.
            }
            libro.dispose();
        }
    }

    private void hojaDatos(SXSSFWorkbook libro, ReporteDatos datos, Estilos estilos) {
        TablaReporte tabla = datos.tabla();
        Sheet hoja = libro.createSheet("Datos");
        List<ColumnaReporte> columnas = tabla.columnas();

        Row cabecera = hoja.createRow(0);
        cabecera.setHeightInPoints(22);
        for (int c = 0; c < columnas.size(); c++) {
            Cell celda = cabecera.createCell(c);
            celda.setCellValue(ValoresReporte.texto(columnas.get(c).nombre()));
            celda.setCellStyle(estilos.cabecera);
        }

        int[] anchos = new int[columnas.size()];
        for (int c = 0; c < columnas.size(); c++) {
            anchos[c] = Math.max(10, columnas.get(c).nombre().length() + 2);
        }

        List<List<Object>> filas = tabla.filas();
        for (int f = 0; f < filas.size(); f++) {
            Row fila = hoja.createRow(f + 1);
            List<Object> valores = filas.get(f);
            for (int c = 0; c < columnas.size(); c++) {
                Object valor = c < valores.size() ? valores.get(c) : null;
                escribirCelda(fila.createCell(c), valor, columnas.get(c).tipo(), estilos);
                if (valor != null) {
                    anchos[c] = Math.max(anchos[c], valor.toString().length() + 2);
                }
            }
        }

        for (int c = 0; c < columnas.size(); c++) {
            hoja.setColumnWidth(c, Math.min(60, anchos[c]) * 256);
        }
        hoja.createFreezePane(0, 1);
        if (!columnas.isEmpty()) {
            hoja.setAutoFilter(new CellRangeAddress(0, Math.max(0, filas.size()), 0, columnas.size() - 1));
        }
    }

    private void escribirCelda(Cell celda, Object valor, TipoColumna tipo, Estilos estilos) {
        if (valor == null) {
            celda.setBlank();
            return;
        }
        switch (tipo) {
            case ENTERO -> {
                celda.setCellValue(valor instanceof Number n ? n.doubleValue() : 0d);
                celda.setCellStyle(estilos.entero);
            }
            case MONEDA -> {
                BigDecimal numero = ValoresReporte.aDecimal(valor);
                celda.setCellValue(numero == null ? 0d : numero.doubleValue());
                celda.setCellStyle(estilos.moneda);
            }
            case FECHA -> {
                LocalDate fecha = ValoresReporte.aFecha(valor);
                if (fecha == null) {
                    celda.setCellValue(ValoresReporte.texto(String.valueOf(valor)));
                    celda.setCellStyle(estilos.texto);
                } else {
                    celda.setCellValue(java.sql.Date.valueOf(fecha));
                    celda.setCellStyle(estilos.fecha);
                }
            }
            default -> {
                celda.setCellValue(ValoresReporte.texto(String.valueOf(valor)));
                celda.setCellStyle(estilos.texto);
            }
        }
    }

    private void hojaResumen(SXSSFWorkbook libro, ReporteDatos datos, Estilos estilos) {
        Sheet hoja = libro.createSheet("Resumen");
        hoja.setColumnWidth(0, 32 * 256);
        hoja.setColumnWidth(1, 70 * 256);

        int f = 0;
        Row titulo = hoja.createRow(f++);
        titulo.setHeightInPoints(28);
        Cell celdaTitulo = titulo.createCell(0);
        celdaTitulo.setCellValue(ValoresReporte.texto("LittleStyle · " + datos.titulo()));
        celdaTitulo.setCellStyle(estilos.titulo);

        Map<String, Object> p = datos.parametros();
        f = escribirPar(hoja, f, "Subtítulo", p.get("SUBTITULO"), estilos);
        f = escribirPar(hoja, f, "Alcance", p.get("ALCANCE"), estilos);
        f = escribirPar(hoja, f, "Generado por", p.get("GENERADO_POR"), estilos);
        f = escribirPar(hoja, f, "Generado en", p.get("GENERADO_EN"), estilos);
        f++;
        Row cabecera = hoja.createRow(f++);
        Cell c0 = cabecera.createCell(0);
        c0.setCellValue("Total");
        c0.setCellStyle(estilos.cabecera);
        Cell c1 = cabecera.createCell(1);
        c1.setCellValue("Valor");
        c1.setCellStyle(estilos.cabecera);

        for (Map.Entry<String, Object> e : p.entrySet()) {
            if (esParametroDeCabecera(e.getKey())) {
                continue;
            }
            f = escribirPar(hoja, f, legible(e.getKey()), e.getValue(), estilos);
        }
        hoja.createFreezePane(0, 1);
    }

    private static boolean esParametroDeCabecera(String clave) {
        return switch (clave) {
            case "TITULO", "SUBTITULO", "ALCANCE", "GENERADO_POR", "GENERADO_EN" -> true;
            default -> false;
        };
    }

    private int escribirPar(Sheet hoja, int fila, String etiqueta, Object valor, Estilos estilos) {
        Row r = hoja.createRow(fila);
        Cell c0 = r.createCell(0);
        c0.setCellValue(etiqueta);
        c0.setCellStyle(estilos.etiqueta);
        Cell c1 = r.createCell(1);
        c1.setCellValue(ValoresReporte.texto(valor == null ? "" : String.valueOf(valor)));
        c1.setCellStyle(estilos.texto);
        return fila + 1;
    }

    private static String legible(String clave) {
        String t = clave.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    /** Estilos reutilizados por todas las celdas del libro. */
    private static final class Estilos {
        private final CellStyle cabecera;
        private final CellStyle texto;
        private final CellStyle entero;
        private final CellStyle moneda;
        private final CellStyle fecha;
        private final CellStyle titulo;
        private final CellStyle etiqueta;

        private Estilos(SXSSFWorkbook libro) {
            Font fuenteCabecera = libro.createFont();
            fuenteCabecera.setBold(true);
            fuenteCabecera.setColor(IndexedColors.WHITE.getIndex());

            XSSFCellStyle cab = (XSSFCellStyle) libro.createCellStyle();
            cab.setFont(fuenteCabecera);
            cab.setFillForegroundColor(new XSSFColor(new byte[]{0x14, 0x53, 0x2D}, null));
            cab.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cab.setAlignment(HorizontalAlignment.CENTER);
            cab.setVerticalAlignment(VerticalAlignment.CENTER);
            cab.setWrapText(true);
            cab.setBorderBottom(BorderStyle.THIN);
            cab.setBottomBorderColor(IndexedColors.WHITE.getIndex());
            cabecera = cab;

            texto = libro.createCellStyle();
            texto.setVerticalAlignment(VerticalAlignment.CENTER);

            entero = libro.createCellStyle();
            entero.setDataFormat(libro.createDataFormat().getFormat("#,##0"));
            entero.setAlignment(HorizontalAlignment.RIGHT);

            moneda = libro.createCellStyle();
            moneda.setDataFormat(libro.createDataFormat().getFormat("\"COP\" #,##0"));
            moneda.setAlignment(HorizontalAlignment.RIGHT);

            fecha = libro.createCellStyle();
            fecha.setDataFormat(libro.createDataFormat().getFormat("dd/MM/yyyy"));
            fecha.setAlignment(HorizontalAlignment.CENTER);

            Font fuenteTitulo = libro.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            fuenteTitulo.setColor(IndexedColors.DARK_GREEN.getIndex());
            titulo = libro.createCellStyle();
            titulo.setFont(fuenteTitulo);

            Font fuenteEtiqueta = libro.createFont();
            fuenteEtiqueta.setBold(true);
            etiqueta = libro.createCellStyle();
            etiqueta.setFont(fuenteEtiqueta);
        }
    }
}
