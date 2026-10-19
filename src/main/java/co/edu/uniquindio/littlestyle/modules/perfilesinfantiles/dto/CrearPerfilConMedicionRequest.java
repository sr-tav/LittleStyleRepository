package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Datos del perfil y de su primera medición. */
public record CrearPerfilConMedicionRequest(
        @NotNull @Valid PerfilRequest perfil,
        @NotNull @Valid MedicionRequest medicionInicial
) {
}
