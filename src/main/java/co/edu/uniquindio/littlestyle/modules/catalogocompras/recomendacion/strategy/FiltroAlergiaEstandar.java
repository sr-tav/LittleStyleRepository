package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.ComponenteMaterial;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.PrendaRecomendable;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FiltroAlergiaEstandar implements FiltroAlergia{
    private static final Set<MaterialTextil> SINTETICOS = EnumSet.of(
            MaterialTextil.POLIESTER, MaterialTextil.NYLON, MaterialTextil.ACRILICO,
            MaterialTextil.ELASTANO, MaterialTextil.VISCOSA);
    private static final int UMBRAL_SINTETICO = 30;

    @Override
    public EvaluacionAlergia evaluar(Set<AlergiaTextil> alergias, PrendaRecomendable prenda) {
        if (alergias == null || alergias.isEmpty() || prenda == null) return EvaluacionAlergia.apta();
        List<String> motivos = new ArrayList<>();
        int pctSintetico = prenda.composicion().stream()
                .filter(c -> SINTETICOS.contains(c.material())).mapToInt(ComponenteMaterial::porcentaje).sum();
        if (alergias.contains(AlergiaTextil.FIBRAS_SINTETICAS) && pctSintetico >= UMBRAL_SINTETICO)
            motivos.add("Contiene " + pctSintetico + "% fibras sintéticas");
        if (alergias.contains(AlergiaTextil.LANA) && contiene(prenda, MaterialTextil.LANA)) motivos.add("Contiene lana");
        if (alergias.contains(AlergiaTextil.LATEX_ELASTICO)
                && (contiene(prenda, MaterialTextil.LATEX) || contiene(prenda, MaterialTextil.ELASTANO)))
            motivos.add("Contiene látex/elastano");
        if (alergias.contains(AlergiaTextil.CUERO) && contiene(prenda, MaterialTextil.CUERO)) motivos.add("Contiene cuero");
        if (alergias.contains(AlergiaTextil.ALGODON) && contiene(prenda, MaterialTextil.ALGODON)) motivos.add("Contiene algodón");
        if (alergias.contains(AlergiaTextil.TINTES) && prenda.tintesSinteticos()) motivos.add("Tintes sintéticos");
        if (alergias.contains(AlergiaTextil.BROCHES_METALICOS) && prenda.brochesMetalicos()) motivos.add("Broches metálicos");
        if (alergias.contains(AlergiaTextil.NIQUEL) && prenda.contieneNiquel()) motivos.add("Contiene níquel");
        if (alergias.contains(AlergiaTextil.FORMALDEHIDO) && prenda.tratamientoFormaldehido()) motivos.add("Tratamiento con formaldehído");
        if (alergias.contains(AlergiaTextil.OTRA) && !prenda.tieneComposicion()) motivos.add("Composición no declarada, verificar otra alergia");
        if (motivos.isEmpty()) return EvaluacionAlergia.apta();
        boolean excluye = motivos.stream().anyMatch(m ->
                m.contains("níquel") || m.contains("formaldehído") || m.contains("sintéticas"));
        return new EvaluacionAlergia(excluye ? NivelRiesgo.EXCLUIDA : NivelRiesgo.ADVERTENCIA, motivos);
    }
    private boolean contiene(PrendaRecomendable p, MaterialTextil m) {
        return p.composicion().stream().anyMatch(c -> c.material() == m);
    }
}
