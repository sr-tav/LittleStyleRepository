package co.edu.uniquindio.littlestyle.modules.reportes.factory;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.ArchivoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.FormatoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ReporteDatos;

public abstract class CreadorGeneradorReporte {

    public abstract FormatoReporte formato();

    protected abstract GeneradorReporte crearGenerador();

    public ArchivoReporte exportar(ReporteDatos datos) {
        GeneradorReporte generador = crearGenerador();
        byte[] bytes = generador.generar(datos);
        FormatoReporte f = generador.formato();
        return new ArchivoReporte(bytes, datos.nombreArchivo() + "." + f.getExtension(), f.getTipoMedia());
    }
}