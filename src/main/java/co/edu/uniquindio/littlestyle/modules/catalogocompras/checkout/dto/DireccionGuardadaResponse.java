package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

/** Dirección guardada del cliente; {@code guardada} es falso y la dirección nula si no tiene una. */
public record DireccionGuardadaResponse(
        boolean guardada,
        DireccionEnvioRequest direccion
) {
}
