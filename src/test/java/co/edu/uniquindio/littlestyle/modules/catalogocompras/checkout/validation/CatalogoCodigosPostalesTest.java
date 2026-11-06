package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.validation;

import org.junit.jupiter.api.Test;

import static co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.validation.CatalogoCodigosPostales.Resultado.NO_CORRESPONDE_AL_DEPARTAMENTO;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.validation.CatalogoCodigosPostales.Resultado.NO_CORRESPONDE_AL_MUNICIPIO;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.validation.CatalogoCodigosPostales.Resultado.VALIDO;
import static org.assertj.core.api.Assertions.assertThat;

class CatalogoCodigosPostalesTest {

    @Test
    void armeniaSoloAceptaSusCodigos() {
        assertThat(CatalogoCodigosPostales.codigosDe("Quindío", "Armenia"))
                .contains(java.util.List.of("630001", "630002", "630003", "630004", "630007", "630008"));
        assertThat(CatalogoCodigosPostales.validar("Quindío", "Armenia", "630001")).isEqualTo(VALIDO);
        assertThat(CatalogoCodigosPostales.validar("Quindío", "Armenia", "630005"))
                .isEqualTo(NO_CORRESPONDE_AL_MUNICIPIO);
    }

    @Test
    void codigoDeOtroMunicipioDelMismoDepartamentoSeRechaza() {
        assertThat(CatalogoCodigosPostales.validar("Quindío", "Armenia", "631001"))
                .isEqualTo(NO_CORRESPONDE_AL_MUNICIPIO);
        assertThat(CatalogoCodigosPostales.validar("Quindío", "Circasia", "631001")).isEqualTo(VALIDO);
    }

    @Test
    void codigoDeOtroDepartamentoSeRechaza() {
        assertThat(CatalogoCodigosPostales.validar("Quindío", "Armenia", "050001"))
                .isEqualTo(NO_CORRESPONDE_AL_DEPARTAMENTO);
    }

    @Test
    void ignoraTildesMayusculasYArticulos() {
        assertThat(CatalogoCodigosPostales.validar("QUINDIO", "calarca", "632001")).isEqualTo(VALIDO);
        assertThat(CatalogoCodigosPostales.validar("Quindío", "la Tebaida", "633020")).isEqualTo(VALIDO);
        assertThat(CatalogoCodigosPostales.validar("Quindío", "Génova", "632080")).isEqualTo(VALIDO);
    }

    @Test
    void todoQuindioEstaCatalogado() {
        for (String municipio : new String[] {"Armenia", "Buenavista", "Calarcá", "Circasia", "Córdoba",
                "Filandia", "Génova", "la Tebaida", "Montenegro", "Pijao", "Quimbaya", "Salento"}) {
            assertThat(CatalogoCodigosPostales.codigosDe("Quindío", municipio))
                    .as(municipio).isPresent();
        }
    }

    @Test
    void capitalesYMunicipiosPrincipalesTienenCodigosCatalogados() {
        assertThat(CatalogoCodigosPostales.codigosDe("Valle del Cauca", "Cali").orElseThrow())
                .contains("760001", "760002", "760003", "760004");
        assertThat(CatalogoCodigosPostales.codigosDe("Bolívar", "Clemencia"))
                .contains(java.util.List.of("130510", "130517"));
        assertThat(CatalogoCodigosPostales.codigosDe("Antioquia", "Medellín").orElseThrow())
                .contains("050001");
        assertThat(CatalogoCodigosPostales.codigosDe("Atlántico", "Barranquilla").orElseThrow())
                .contains("080001");
        assertThat(CatalogoCodigosPostales.codigosDe("Santander", "Bucaramanga").orElseThrow())
                .contains("680001");
        assertThat(CatalogoCodigosPostales.codigosDe("Nariño", "Pasto").orElseThrow())
                .contains("520001");
        assertThat(CatalogoCodigosPostales.codigosDe("Chocó", "Belén de Bajira").orElseThrow())
                .contains("277017");
        assertThat(CatalogoCodigosPostales.validar("Valle del Cauca", "Cali", "760001")).isEqualTo(VALIDO);
        assertThat(CatalogoCodigosPostales.validar("Valle del Cauca", "Cali", "760047"))
                .isEqualTo(NO_CORRESPONDE_AL_MUNICIPIO);
        assertThat(CatalogoCodigosPostales.validar("Valle del Cauca", "Cali", "110111"))
                .isEqualTo(NO_CORRESPONDE_AL_DEPARTAMENTO);
        assertThat(CatalogoCodigosPostales.codigosDe("Bogotá D.C.", "Bogotá D.C.").orElseThrow())
                .contains("110111");
    }
}
