package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

@Converter
public class BigDecimalCifradoConverter implements AttributeConverter<BigDecimal, String> {

    @Override
    public String convertToDatabaseColumn(BigDecimal valor) {
        return valor == null ? null : CifradoPerfil.cifrar(valor.toPlainString());
    }

    @Override
    public BigDecimal convertToEntityAttribute(String valor) {
        return valor == null ? null : new BigDecimal(CifradoPerfil.descifrar(valor));
    }
}
