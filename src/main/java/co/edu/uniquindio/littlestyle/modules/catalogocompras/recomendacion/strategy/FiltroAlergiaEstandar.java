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

    private record Regla(AlergiaTextil alergia, boolean excluyeSiCoincide,
                         java.util.function.Function<PrendaRecomendable, Optional<String>> detectar) {}

    private static final List<Regla> REGLAS = List.of(
            new Regla(AlergiaTextil.LANA, false, p -> contiene(p, MaterialTextil.LANA) ? Optional.of("Contiene lana") : Optional.empty()),
            new Regla(AlergiaTextil.LATEX_ELASTICO, false, p -> contieneAlguno(p, MaterialTextil.LATEX, MaterialTextil.ELASTANO) ? Optional.of("Contiene látex/elastano") : Optional.empty()),
            new Regla(AlergiaTextil.CUERO, false, p -> contiene(p, MaterialTextil.CUERO) ? Optional.of("Contiene cuero") : Optional.empty()),
            new Regla(AlergiaTextil.ALGODON, false, p -> contiene(p, MaterialTextil.ALGODON) ? Optional.of("Contiene algodón") : Optional.empty()),
            new Regla(AlergiaTextil.TINTES, false, p -> p.tintesSinteticos() ? Optional.of("Tintes sintéticos") : Optional.empty()),
            new Regla(AlergiaTextil.BROCHES_METALICOS, false, p -> p.brochesMetalicos() ? Optional.of("Broches metálicos") : Optional.empty()),
            new Regla(AlergiaTextil.NIQUEL, true, p -> p.contieneNiquel() ? Optional.of("Contiene níquel") : Optional.empty()),
            new Regla(AlergiaTextil.FORMALDEHIDO, true, p -> p.tratamientoFormaldehido() ? Optional.of("Tratamiento con formaldehído") : Optional.empty()),
            new Regla(AlergiaTextil.OTRA, false, p -> !p.tieneComposicion() ? Optional.of("Composición no declarada, verificar otra alergia") : Optional.empty())
    );

    @Override
    public EvaluacionAlergia evaluar(Set<AlergiaTextil> alergias, PrendaRecomendable prenda) {
        if (alergias == null || alergias.isEmpty() || prenda == null) return EvaluacionAlergia.apta();
        List<String> motivos = new ArrayList<>();
        boolean excluye = false;

        if (alergias.contains(AlergiaTextil.FIBRAS_SINTETICAS)) {
            int pct = prenda.composicion().stream()
                    .filter(c -> SINTETICOS.contains(c.material())).mapToInt(ComponenteMaterial::porcentaje).sum();
            if (pct >= UMBRAL_SINTETICO) {
                motivos.add("Contiene " + pct + "% fibras sintéticas");
                excluye = true;
            }
        }
        for (Regla r : REGLAS) {
            if (!alergias.contains(r.alergia())) continue;
            Optional<String> m = r.detectar().apply(prenda);
            if (m.isPresent()) {
                motivos.add(m.get());
                if (r.excluyeSiCoincide()) excluye = true;
            }
        }
        if (motivos.isEmpty()) return EvaluacionAlergia.apta();
        return new EvaluacionAlergia(excluye ? NivelRiesgo.EXCLUIDA : NivelRiesgo.ADVERTENCIA, motivos);
    }

    private static boolean contiene(PrendaRecomendable p, MaterialTextil m) {
        return p.composicion().stream().anyMatch(c -> c.material() == m);
    }
    private static boolean contieneAlguno(PrendaRecomendable p, MaterialTextil... ms) {
        Set<MaterialTextil> set = EnumSet.noneOf(MaterialTextil.class);
        Collections.addAll(set, ms);
        return p.composicion().stream().anyMatch(c -> set.contains(c.material()));
    }
}
