package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;

import java.util.Set;

/**
 * Vista del perfil que usa el motor: biometría, alergias declaradas, texto
 * libre de "otra alergia" y preferencias de estilo para el puntaje.
 */
public record PerfilBiometrico(DatosBiometricos bio, Set<AlergiaTextil> alergias, String otraAlergia,
                               Set<ColorPreferido> coloresPreferidos, Set<Estampado> estampadosPreferidos) {
    /** Reemplaza los conjuntos nulos por vacíos y guarda copias inmutables. */
    public PerfilBiometrico {
        alergias = alergias == null ? Set.of() : Set.copyOf(alergias);
        coloresPreferidos = coloresPreferidos == null ? Set.of() : Set.copyOf(coloresPreferidos);
        estampadosPreferidos = estampadosPreferidos == null ? Set.of() : Set.copyOf(estampadosPreferidos);
    }
}

