package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Contextura;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Holgura;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record PerfilResponse(
        Long id,
        String nombre,
        LocalDate fechaNacimiento,
        int edadAnios,
        int mesesRestantes,
        Contextura contextura,
        Holgura holgura,
        Set<AlergiaTextil> alergias,
        String otraAlergia,
        boolean sinAlergias,
        Set<ColorPreferido> coloresPreferidos,
        Set<Estampado> estampadosPreferidos,
        String otroColor,
        String otroEstampado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        MedicionResponse ultimaMedicion,
        int porcentajeCompletitud
) {
}
