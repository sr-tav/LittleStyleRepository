package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Contextura;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Holgura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record PerfilRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
        String nombre,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento,

        @NotNull(message = "La contextura es obligatoria")
        Contextura contextura,

        @NotNull(message = "La holgura es obligatoria")
        Holgura holgura,

        @NotNull(message = "Debe declarar las alergias o indicar que no tiene alergias")
        @Size(max = 10, message = "La lista de alergias supera las opciones permitidas")
        Set<@NotNull(message = "Cada alergia debe ser una opción válida") AlergiaTextil> alergias,

        @Size(max = 60, message = "La descripción de otra alergia no puede superar 60 caracteres")
        String otraAlergia,

        boolean sinAlergias,

        @NotNull(message = "La lista de colores preferidos es obligatoria")
        @Size(max = 10, message = "Puede registrar máximo 10 colores preferidos")
        Set<@NotNull(message = "Cada color preferido debe ser una opción válida") ColorPreferido> coloresPreferidos,

        @NotNull(message = "La lista de estampados preferidos es obligatoria")
        @Size(max = 10, message = "Puede registrar máximo 10 estampados preferidos")
        Set<@NotNull(message = "Cada estampado preferido debe ser una opción válida") Estampado> estampadosPreferidos,

        @Size(min = 2, max = 30, message = "El otro color debe tener entre 2 y 30 caracteres")
        @Pattern(regexp = "(?U)[\\p{L} ]+", message = "El otro color solo puede contener letras y espacios")
        String otroColor,

        @Size(min = 2, max = 30, message = "El otro estampado debe tener entre 2 y 30 caracteres")
        @Pattern(regexp = "(?U)[\\p{L} ]+", message = "El otro estampado solo puede contener letras y espacios")
        String otroEstampado
) {
}
