package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

/** Guarda valores decimales (estatura, peso) cifrados. */
@Converter
public class BigDecimalCifradoConverter implements AttributeConverter<BigDecimal, String> {

    /** Cifra el valor con AES-GCM antes de guardarlo; {@code null} se guarda como {@code null}. */
    @Override
    public String convertToDatabaseColumn(BigDecimal valor) {
        return valor == null ? null : CifradoPerfil.cifrar(valor.toPlainString());
    }

    /** Descifra el valor y lo convierte a decimal. */
    @Override
    public BigDecimal convertToEntityAttribute(String valor) {
        return valor == null ? null : new BigDecimal(CifradoPerfil.descifrar(valor));
    }
}
