package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.ComponenteMaterial;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.PrendaRecomendable;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Cada alergia del perfil debe poder dispararse con algún dato que el vendedor declara en la prenda. */
class FiltroAlergiaCoberturaTest {

    private final FiltroAlergiaEstandar filtro = new FiltroAlergiaEstandar();

    private static PrendaRecomendable prenda(MaterialTextil material, boolean tintes, boolean broches,
                                             boolean niquel, boolean formaldehido) {
        return new PrendaRecomendable(1L, "Prenda", "VESTIDOS", null, BigDecimal.TEN, null, null,
                List.of(new ComponenteMaterial(material, 100)), tintes, broches, niquel, formaldehido,
                Set.of(), null, null);
    }

    /** Prenda que declara justo el dato que activa la alergia; falla si la alergia no tiene contraparte. */
    private static PrendaRecomendable prendaQueActiva(AlergiaTextil alergia) {
        return switch (alergia) {
            case FIBRAS_SINTETICAS -> prenda(MaterialTextil.POLIESTER, false, false, false, false);
            case LANA -> prenda(MaterialTextil.LANA, false, false, false, false);
            case ALGODON -> prenda(MaterialTextil.ALGODON, false, false, false, false);
            case LATEX_ELASTICO -> prenda(MaterialTextil.LATEX, false, false, false, false);
            case CUERO -> prenda(MaterialTextil.CUERO, false, false, false, false);
            case OTRA -> prenda(MaterialTextil.OTRO, false, false, false, false);
            case TINTES -> prenda(MaterialTextil.ALGODON, true, false, false, false);
            case BROCHES_METALICOS -> prenda(MaterialTextil.ALGODON, false, true, false, false);
            case NIQUEL -> prenda(MaterialTextil.ALGODON, false, false, true, false);
            case FORMALDEHIDO -> prenda(MaterialTextil.ALGODON, false, false, false, true);
        };
    }

    @ParameterizedTest
    @EnumSource(AlergiaTextil.class)
    void todaAlergiaDelPerfilTieneUnaContrapartidaEnLaPrenda(AlergiaTextil alergia) {
        EvaluacionAlergia evaluacion = filtro.evaluar(Set.of(alergia), prendaQueActiva(alergia));

        assertThat(evaluacion.nivel()).isNotEqualTo(NivelRiesgo.APTA);
        assertThat(evaluacion.motivos()).isNotEmpty();
    }

    @Test
    void materialOtroAdvierteSoloConLaAlergiaOtra() {
        assertThat(filtro.evaluar(Set.of(AlergiaTextil.OTRA), prendaQueActiva(AlergiaTextil.OTRA)).nivel())
                .isEqualTo(NivelRiesgo.ADVERTENCIA);
        assertThat(filtro.evaluar(Set.of(AlergiaTextil.LANA), prendaQueActiva(AlergiaTextil.OTRA)).nivel())
                .isEqualTo(NivelRiesgo.APTA);
    }
}