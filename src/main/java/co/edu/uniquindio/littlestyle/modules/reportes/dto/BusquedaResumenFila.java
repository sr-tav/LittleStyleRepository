package co.edu.uniquindio.littlestyle.modules.reportes.dto;

import java.time.LocalDateTime;

/**
 * Fila agregada del reporte de búsquedas: un término normalizado con su volumen, cuántas veces no
 * devolvió resultados y la última vez que se buscó.
 */
public record BusquedaResumenFila(String termino, long busquedas, long sinResultados,
                                  LocalDateTime ultimaBusqueda) {
}
