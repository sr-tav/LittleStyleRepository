package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Converter
public class AlergiasConverter implements AttributeConverter<Set<AlergiaTextil>, String> {

    @Override
    public String convertToDatabaseColumn(Set<AlergiaTextil> alergias) {
        if (alergias == null) {
            return null;
        }
        String serializado = new TreeSet<>(alergias).stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
        return CifradoPerfil.cifrar(serializado);
    }

    @Override
    public Set<AlergiaTextil> convertToEntityAttribute(String valor) {
        if (valor == null) {
            return Set.of();
        }
        String descifrado = CifradoPerfil.descifrar(valor);
        if (descifrado.isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(descifrado.split(","))
                .map(AlergiaTextil::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }
}
