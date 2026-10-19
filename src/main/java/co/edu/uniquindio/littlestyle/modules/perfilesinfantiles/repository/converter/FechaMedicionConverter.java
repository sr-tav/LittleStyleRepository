package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

/** Guarda la fecha de medición cifrada; también lee fechas antiguas guardadas en claro. */
@Converter
public class FechaMedicionConverter implements AttributeConverter<LocalDate, String> {

    /** Cifra el valor con AES-GCM antes de guardarlo; {@code null} se guarda como {@code null}. */
    @Override
    public String convertToDatabaseColumn(LocalDate fecha) {
        return fecha == null ? null : CifradoPerfil.cifrar(fecha.toString());
    }

    /** Descifra la fecha; si no tiene el formato actual, intenta leerla en claro antes de descifrarla. */
    @Override
    public LocalDate convertToEntityAttribute(String valor) {
        if (valor == null) {
            return null;
        }
        if (!CifradoPerfil.estaCifradoEnFormatoActual(valor)) {
            try {
                return LocalDate.parse(valor);
            } catch (java.time.format.DateTimeParseException ignored) {
                return LocalDate.parse(CifradoPerfil.descifrar(valor));
            }
        }
        return LocalDate.parse(CifradoPerfil.descifrar(valor));
    }
}
