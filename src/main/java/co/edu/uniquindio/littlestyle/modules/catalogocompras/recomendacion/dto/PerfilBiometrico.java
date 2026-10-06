package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;

import java.util.Set;

public record PerfilBiometrico(DatosBiometricos bio, Set<AlergiaTextil> alergias) {
}
