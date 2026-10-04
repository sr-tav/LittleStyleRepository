package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TextoLegadoCifradoConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String valor) {
        return valor == null ? null : CifradoPerfil.cifrar(valor);
    }

    @Override
    public String convertToEntityAttribute(String valor) {
        if (valor == null || !CifradoPerfil.estaCifradoEnFormatoActual(valor)) {
            return valor;
        }
        return CifradoPerfil.descifrar(valor);
    }
}
