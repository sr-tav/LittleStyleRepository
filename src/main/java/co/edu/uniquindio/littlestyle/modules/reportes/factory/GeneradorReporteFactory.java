package co.edu.uniquindio.littlestyle.modules.reportes.factory;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.FormatoReporte;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class GeneradorReporteFactory {

    private final Map<FormatoReporte, CreadorGeneradorReporte> creadores = new EnumMap<>(FormatoReporte.class);

    public GeneradorReporteFactory(List<CreadorGeneradorReporte> disponibles) {
        for (CreadorGeneradorReporte c : disponibles) {
            if (creadores.putIfAbsent(c.formato(), c) != null) {
                throw new IllegalStateException("Hay dos creadores para el formato " + c.formato());
            }
        }
    }

    public CreadorGeneradorReporte obtener(FormatoReporte formato) {
        CreadorGeneradorReporte creador = formato == null ? null : creadores.get(formato);
        if (creador == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Formato de exportación no soportado", "formato");
        }
        return creador;
    }
}