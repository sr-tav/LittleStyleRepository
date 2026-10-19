package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda textos opcionales del perfil (otra alergia, otro color, otro estampado) cifrados. */
@Converter
public class StringCifradoConverter implements AttributeConverter<String, String> {

    /** Cifra el valor con AES-GCM antes de guardarlo; {@code null} se guarda como {@code null}. */
    @Override
    public String convertToDatabaseColumn(String valor) {
        return valor == null ? null : CifradoPerfil.cifrar(valor);
    }

    /** Descifra el texto. */
    @Override
    public String convertToEntityAttribute(String valor) {
        return valor == null ? null : CifradoPerfil.descifrar(valor);
    }
}
