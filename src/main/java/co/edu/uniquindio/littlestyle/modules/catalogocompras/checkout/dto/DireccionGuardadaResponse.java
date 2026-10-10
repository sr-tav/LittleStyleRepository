package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto;

public record DireccionGuardadaResponse(
        boolean guardada,
        DireccionEnvioRequest direccion
) {
}
