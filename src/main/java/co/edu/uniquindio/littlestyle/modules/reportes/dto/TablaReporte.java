package co.edu.uniquindio.littlestyle.modules.reportes.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tabla tipada de un reporte: encabezados con su tipo lógico y filas de valores crudos
 * ({@link java.math.BigDecimal}, {@link Long}, {@link java.time.LocalDate} o {@link String}).
 *
 * <p>Es la vista que consumen XLSX y CSV. Las filas formateadas para el PDF se construyen en
 * paralelo desde las mismas agregaciones en {@code ConstructorDatosReporte}; esta tabla no deriva
 * de ellas ni viceversa.</p>
 */
public record TablaReporte(List<ColumnaReporte> columnas, List<List<Object>> filas) {

    public TablaReporte {
        columnas = columnas == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(columnas));
        List<List<Object>> copia = new ArrayList<>();
        if (filas != null) {
            for (List<Object> fila : filas) {
                copia.add(fila == null
                        ? List.of()
                        : Collections.unmodifiableList(new ArrayList<>(fila)));
            }
        }
        filas = Collections.unmodifiableList(copia);
    }

    public static TablaReporte vacia() {
        return new TablaReporte(List.of(), List.of());
    }

    public boolean esVacia() {
        return filas.isEmpty();
    }
}
