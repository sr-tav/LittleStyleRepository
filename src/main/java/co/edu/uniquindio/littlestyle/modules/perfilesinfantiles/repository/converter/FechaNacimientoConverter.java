package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

@Converter
public class FechaNacimientoConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate fecha) {
        return fecha == null ? null : CifradoPerfil.cifrar(fecha.toString());
    }

    @Override
    public LocalDate convertToEntityAttribute(String valor) {
        return valor == null ? null : LocalDate.parse(CifradoPerfil.descifrar(valor));
    }
}
