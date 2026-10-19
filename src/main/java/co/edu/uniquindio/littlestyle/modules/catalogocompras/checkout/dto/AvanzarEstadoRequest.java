package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Estado al que se avanza y número de guía, obligatorio solo para {@code ENVIADO}. */
public record AvanzarEstadoRequest(
        @NotNull(message = "El estado es obligatorio") EstadoPedido nuevo,
        @Size(max = 60, message = "El número de guía no puede superar 60 caracteres")
        @Pattern(regexp = "[^\\r\\n]*", message = "El número de guía no puede contener saltos de línea")
        String numeroGuia) {
}
