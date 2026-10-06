package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CrearPerfilConMedicionRequest(
        @NotNull @Valid PerfilRequest perfil,
        @NotNull @Valid MedicionRequest medicionInicial
) {
}
