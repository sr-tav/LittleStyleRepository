package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StringCifradoConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String valor) {
        return valor == null ? null : CifradoPerfil.cifrar(valor);
    }

    @Override
    public String convertToEntityAttribute(String valor) {
        return valor == null ? null : CifradoPerfil.descifrar(valor);
    }
}
