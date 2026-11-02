package co.edu.uniquindio.littlestyle.modules.reportes.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Evento que envía el cliente cuando termina de guardar o abrir un archivo exportado (RNF-24).
 *
 * <p>El servidor solo puede medir lo que él mismo respondió; un PDF que se descarga con 200 y
 * falla al abrirse en el equipo del usuario no aparece en los logs del backend. Por eso el cliente
 * reporta el desenlace real y la métrica de éxito combina ambos orígenes.</p>
 */
public record TelemetriaExportacionRequest(
        @NotNull TipoReporte tipo,
        @NotNull FormatoReporte formato,
        @NotNull ResultadoExportacion resultado,
        @Size(max = 300) String motivo) {
}
