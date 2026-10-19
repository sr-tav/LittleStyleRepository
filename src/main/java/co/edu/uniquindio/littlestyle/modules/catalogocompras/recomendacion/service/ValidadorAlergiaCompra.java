package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.ComponenteMaterial;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.PrendaRecomendable;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.TablaTallas;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.EvaluacionAlergia;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.FiltroAlergia;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto.PerfilResponse;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Evalúa la compatibilidad de una prenda con las alergias de un perfil infantil
 * y bloquea la compra cuando la evaluación resulte EXCLUIDA. Solo bloquean las
 * alergias graves (las reglas excluyentes de {@code FiltroAlergiaEstandar}:
 * níquel, formaldehído y ≥30% de fibras sintéticas); las demás coincidencias
 * quedan como advertencia y no impiden la compra.
 */
@Service
@RequiredArgsConstructor
public class ValidadorAlergiaCompra {

    private static final String CODIGO_ALERGIA_EXCLUIDA = "PRENDA_EXCLUIDA_ALERGIA";

    private final FiltroAlergia filtroAlergia;

    /**
     * Lanza 422 con el código {@code PRENDA_EXCLUIDA_ALERGIA} si la prenda queda excluida por las alergias
     * dadas.
     */
    public void validarExcluida(Set<AlergiaTextil> alergias, Prenda prenda, Long perfilId) {
        EvaluacionAlergia evaluacion = filtroAlergia.evaluar(alergias, aEvaluable(prenda));
        if (!evaluacion.excluye()) {
            return;
        }
        throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                "La prenda " + prenda.getNombre()
                        + " no es compatible con las alergias del perfil infantil " + perfilId,
                "items", CODIGO_ALERGIA_EXCLUIDA);
    }

    /**
     * Alergias del perfil que se evalúan; un perfil con el nodo "sin alergias"
     * no aporta alergias aunque el conjunto venga vacío por serialización.
     */
    public static Set<AlergiaTextil> alergiasEvaluables(PerfilResponse perfil) {
        return perfil.sinAlergias() ? Set.of() : perfil.alergias();
    }

    /** Adapta la prenda del catálogo al modelo que consume el filtro de alergias. */
    public PrendaRecomendable aEvaluable(Prenda prenda) {
        return new PrendaRecomendable(
                prenda.getId(), prenda.getNombre(), prenda.getCategoria().name(), null,
                prenda.getPrecio(), null, new TablaTallas(List.of()),
                prenda.getComposicion().stream()
                        .map(componente -> new ComponenteMaterial(componente.getMaterial(),
                                componente.getPorcentaje()))
                        .toList(),
                prenda.isTintesSinteticos(), prenda.isBrochesMetalicos(),
                prenda.isContieneNiquel(), prenda.isTratamientoFormaldehido(), Set.of(),
                prenda.getEstampado(), Map.of());
    }
}
