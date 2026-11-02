package co.edu.uniquindio.littlestyle.modules.reportes.factory;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.FormatoReporte;
import org.springframework.stereotype.Component;

/** Crea generadores CSV (Factory Method) para la exportación de reportes. */
@Component
public class CreadorGeneradorCsv extends CreadorGeneradorReporte {

    @Override
    public FormatoReporte formato() {
        return FormatoReporte.CSV;
    }

    @Override
    protected GeneradorReporte crearGenerador() {
        return new GeneradorReporteCsv();
    }
}
