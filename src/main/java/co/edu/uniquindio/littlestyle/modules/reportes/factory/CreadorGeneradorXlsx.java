package co.edu.uniquindio.littlestyle.modules.reportes.factory;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.FormatoReporte;
import org.springframework.stereotype.Component;

/** Crea generadores XLSX (Factory Method) para la exportación de reportes. */
@Component
public class CreadorGeneradorXlsx extends CreadorGeneradorReporte {

    @Override
    public FormatoReporte formato() {
        return FormatoReporte.XLSX;
    }

    @Override
    protected GeneradorReporte crearGenerador() {
        return new GeneradorReporteXlsx();
    }
}
