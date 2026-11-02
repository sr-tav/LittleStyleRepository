package co.edu.uniquindio.littlestyle.modules.reportes.dto;

import java.time.LocalDate;

public record SolicitudReporte(TipoReporte tipo, FormatoReporte formato,
                               LocalDate startDate, LocalDate endDate, String categoria) {
}