package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CifradoPerfilConverterTest {

    private static final String CLAVE_DESARROLLO =
            "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    @BeforeEach
    void configurarClave() {
        new PerfilCryptoConfiguration(CLAVE_DESARROLLO);
    }

    @Test
    void cifraYDescifraFechaNacimientoSinGuardarTextoPlano() {
        FechaNacimientoConverter converter = new FechaNacimientoConverter();
        LocalDate fecha = LocalDate.of(2020, 2, 29);

        String guardado = converter.convertToDatabaseColumn(fecha);
        String otroCifrado = converter.convertToDatabaseColumn(fecha);

        assertThat(guardado).isNotEqualTo(fecha.toString());
        assertThat(otroCifrado).isNotEqualTo(guardado);
        assertThat(converter.convertToEntityAttribute(guardado)).isEqualTo(fecha);
    }

    @Test
    void cifraYDescifraFechaMedicionSinGuardarTextoPlano() {
        FechaMedicionConverter converter = new FechaMedicionConverter();
        LocalDate fecha = LocalDate.of(2025, 4, 15);

        String guardado = converter.convertToDatabaseColumn(fecha);

        assertThat(guardado).startsWith("v1:").isNotEqualTo(fecha.toString());
        assertThat(converter.convertToEntityAttribute(guardado)).isEqualTo(fecha);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void cifraYDescifraValoresDecimales() {
        BigDecimalCifradoConverter converter = new BigDecimalCifradoConverter();
        BigDecimal valor = new BigDecimal("123.45");

        String guardado = converter.convertToDatabaseColumn(valor);

        assertThat(guardado).startsWith("v1:").isNotEqualTo("123.45");
        assertThat(converter.convertToEntityAttribute(guardado)).isEqualByComparingTo(valor);
    }

    @Test
    void cifraYDescifraTextoLibreSinGuardarElValorEnClaro() {
        StringCifradoConverter converter = new StringCifradoConverter();
        String valor = "Piel sensible";

        String guardado = converter.convertToDatabaseColumn(valor);

        assertThat(guardado).startsWith("v1:").isNotEqualTo(valor);
        assertThat(converter.convertToEntityAttribute(guardado)).isEqualTo(valor);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void leeCifradoAnteriorYTextoPlanoLegadoParaMigracion() {
        StringCifradoConverter converter = new StringCifradoConverter();
        TextoLegadoCifradoConverter textoLegado = new TextoLegadoCifradoConverter();
        String cifradoActual = converter.convertToDatabaseColumn("Piel sensible");
        String cifradoAnterior = cifradoActual.substring("v1:".length());

        assertThat(converter.convertToEntityAttribute(cifradoAnterior)).isEqualTo("Piel sensible");
        assertThat(textoLegado.convertToEntityAttribute("MEDIA")).isEqualTo("MEDIA");
        assertThat(textoLegado.convertToEntityAttribute("B".repeat(40))).isEqualTo("B".repeat(40));
    }

    @Test
    void rechazaCifradoAlteradoEnLugarDeTratarloComoTextoPlano() {
        StringCifradoConverter converter = new StringCifradoConverter();
        String cifrado = converter.convertToDatabaseColumn("Piel sensible");
        char primerCaracter = cifrado.charAt(3) == 'A' ? 'B' : 'A';
        String alterado = cifrado.substring(0, 3) + primerCaracter + cifrado.substring(4);

        assertThatThrownBy(() -> converter.convertToEntityAttribute(alterado))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No fue posible descifrar un dato del perfil infantil");
    }

    @Test
    void cifraYDescifraAlergias() {
        AlergiasConverter converter = new AlergiasConverter();
        Set<AlergiaTextil> alergias = Set.of(AlergiaTextil.LANA, AlergiaTextil.TINTES);

        String guardado = converter.convertToDatabaseColumn(alergias);

        assertThat(guardado).startsWith("v1:").isNotEqualTo("LANA,TINTES");
        assertThat(converter.convertToEntityAttribute(guardado)).containsExactlyInAnyOrderElementsOf(alergias);
    }
}
