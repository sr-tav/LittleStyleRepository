package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

/** Dirección guardada del cliente, identificada para seleccionarla y editarla. */
public record DireccionClienteResponse(
        Long id,
        DireccionEnvioRequest direccion
) {
}
