package co.edu.uniquindio.littlestyle.modules.reportes.factory;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.FormatoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ReporteDatos;

public interface GeneradorReporte {
    FormatoReporte formato();
    byte[] generar(ReporteDatos datos);
}