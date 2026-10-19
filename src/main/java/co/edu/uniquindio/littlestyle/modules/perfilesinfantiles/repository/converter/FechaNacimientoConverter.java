package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

/** Guarda la fecha de nacimiento cifrada. */
@Converter
public class FechaNacimientoConverter implements AttributeConverter<LocalDate, String> {

    /** Cifra el valor con AES-GCM antes de guardarlo; {@code null} se guarda como {@code null}. */
    @Override
    public String convertToDatabaseColumn(LocalDate fecha) {
        return fecha == null ? null : CifradoPerfil.cifrar(fecha.toString());
    }

    /** Descifra la fecha de nacimiento. */
    @Override
    public LocalDate convertToEntityAttribute(String valor) {
        return valor == null ? null : LocalDate.parse(CifradoPerfil.descifrar(valor));
    }
}
