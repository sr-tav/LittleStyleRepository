package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

/**
 * Dirección guardada del cliente, identificada para seleccionarla, editarla o eliminarla.
 * {@code predeterminada} indica la propuesta por defecto mostrada en el checkout.
 */
public record DireccionClienteResponse(
        Long id,
        DireccionEnvioRequest direccion,
        boolean predeterminada
) {
}
