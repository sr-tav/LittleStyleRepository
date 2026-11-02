package co.edu.uniquindio.littlestyle.modules.reportes.factory;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.FormatoReporte;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreadorGeneradorPdf extends CreadorGeneradorReporte {

    private final PlantillasReporte plantillas;

    @Override
    public FormatoReporte formato() {
        return FormatoReporte.PDF;
    }

    @Override
    protected GeneradorReporte crearGenerador() {
        return new GeneradorReportePdf(plantillas);
    }
}