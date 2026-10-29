package co.edu.uniquindio.littlestyle.modules.reportes.factory;

import co.edu.uniquindio.littlestyle.modules.reportes.dto.TipoReporte;
import jakarta.annotation.PostConstruct;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PlantillasReporte {

    private static final Map<TipoReporte, String> RUTAS = Map.of(
            TipoReporte.VENTAS, "/reportes/series.jrxml",
            TipoReporte.DEMANDA, "/reportes/series.jrxml",
            TipoReporte.INVENTARIO, "/reportes/inventario.jrxml");

    private final Map<String, JasperReport> compiladas = new ConcurrentHashMap<>();

    @PostConstruct
    public void precompilar() {
        RUTAS.values().stream().distinct().forEach(r -> compiladas.computeIfAbsent(r, this::compilar));
    }

    public JasperReport obtener(TipoReporte tipo) {
        return compiladas.computeIfAbsent(RUTAS.get(tipo), this::compilar);
    }

    private JasperReport compilar(String ruta) {
        try (InputStream in = PlantillasReporte.class.getResourceAsStream(ruta)) {
            if (in == null) {
                throw new IllegalStateException("No existe la plantilla " + ruta);
            }
            return JasperCompileManager.compileReport(in);
        } catch (IOException | JRException e) {
            throw new IllegalStateException("No se pudo compilar la plantilla " + ruta, e);
        }
    }
}