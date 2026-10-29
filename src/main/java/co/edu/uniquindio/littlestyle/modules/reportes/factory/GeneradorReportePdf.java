package co.edu.uniquindio.littlestyle.modules.reportes.factory;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.FormatoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ReporteDatos;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.pdf.SimplePdfExporterConfiguration;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GeneradorReportePdf implements GeneradorReporte {

    private final PlantillasReporte plantillas;

    public GeneradorReportePdf(PlantillasReporte plantillas) {
        this.plantillas = plantillas;
    }

    @Override
    public FormatoReporte formato() {
        return FormatoReporte.PDF;
    }

    @Override
    public byte[] generar(ReporteDatos datos) {
        try {
            Map<String, Object> parametros = new HashMap<>(datos.parametros());
            parametros.put(JRParameter.REPORT_LOCALE, Locale.of("es", "CO"));

            JasperPrint print = JasperFillManager.fillReport(
                    plantillas.obtener(datos.tipo()), parametros, new JRMapCollectionDataSource(datos.filas()));

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            JRPdfExporter exportador = new JRPdfExporter();
            exportador.setExporterInput(new SimpleExporterInput(print));
            exportador.setExporterOutput(new SimpleOutputStreamExporterOutput(salida));
            SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
            config.setMetadataTitle(datos.titulo());
            config.setMetadataAuthor("LittleStyle");
            exportador.setConfiguration(config);
            exportador.exportReport();
            return salida.toByteArray();
        } catch (JRException e) {
            throw new ReporteGeneracionException("Fallo al renderizar el reporte " + datos.tipo(), e);
        }
    }
}