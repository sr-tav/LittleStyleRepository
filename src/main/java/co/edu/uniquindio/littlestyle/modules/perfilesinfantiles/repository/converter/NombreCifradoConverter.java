package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el nombre del niño cifrado; los nombres antiguos en claro se leen tal cual. */
@Converter
public class NombreCifradoConverter implements AttributeConverter<String, String> {

    /** Cifra el valor con AES-GCM antes de guardarlo; {@code null} se guarda como {@code null}. */
    @Override
    public String convertToDatabaseColumn(String valor) {
        return valor == null ? null : CifradoPerfil.cifrar(valor);
    }

    /** Descifra el nombre si tiene el formato actual; si no, lo devuelve sin cambios. */
    @Override
    public String convertToEntityAttribute(String valor) {
        if (valor == null || !CifradoPerfil.estaCifradoEnFormatoActual(valor)) {
            return valor;
        }
        return CifradoPerfil.descifrar(valor);
    }
}
