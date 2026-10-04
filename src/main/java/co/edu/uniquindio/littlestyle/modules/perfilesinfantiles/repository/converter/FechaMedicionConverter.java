package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

@Converter
public class FechaMedicionConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate fecha) {
        return fecha == null ? null : CifradoPerfil.cifrar(fecha.toString());
    }

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
