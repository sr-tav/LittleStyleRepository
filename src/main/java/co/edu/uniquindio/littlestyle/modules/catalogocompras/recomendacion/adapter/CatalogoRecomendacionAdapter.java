package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.adapter;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.ComponenteMaterial;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.PrendaRecomendable;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.RangoTalla;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.TablaTallas;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port.CatalogoRecomendacionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Catálogo real de US-08 para el motor de recomendaciones de US-07: expone las prendas activas con su
 * tabla de tallas, composición y stock por talla. Una prenda que no se pueda convertir se omite sin
 * afectar a las demás (RNF-29).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogoRecomendacionAdapter implements CatalogoRecomendacionPort {

    private final PrendaRepository prendaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PrendaRecomendable> listarPrendasPublicadas() {
        return prendaRepository.findAllByEstado(EstadoPrenda.ACTIVA).stream()
                .map(this::convertirSeguro)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PrendaRecomendable> buscarPrendaPublicada(Long prendaId) {
        return prendaRepository.findByIdAndEstado(prendaId, EstadoPrenda.ACTIVA).map(this::convertirSeguro);
    }

    private PrendaRecomendable convertirSeguro(Prenda prenda) {
        try {
            return convertir(prenda);
        } catch (RuntimeException ex) {
            log.warn("Prenda {} omitida del catálogo de recomendaciones por datos inválidos", prenda.getId(), ex);
            return null;
        }
    }

    static PrendaRecomendable convertir(Prenda p) {
        List<RangoTalla> rangos = p.getTallas().stream()
                .map(t -> new RangoTalla(t.getTalla(), t.getEstaturaMinCm(), t.getEstaturaMaxCm(),
                        t.getPesoMinKg(), t.getPesoMaxKg()))
                .toList();
        Map<String, Integer> stockPorTalla = new LinkedHashMap<>();
        for (TallaPrenda t : p.getTallas()) {
            stockPorTalla.put(t.getTalla(), t.getStock());
        }
        String marca = p.getMarca();
        if (marca == null && p.getVendedor() != null && p.getVendedor().getNombreTienda() != null) {
            marca = p.getVendedor().getNombreTienda();
        }
        if (marca == null) marca = "Sin marca";
        return new PrendaRecomendable(
                p.getId(), p.getNombre(), p.getCategoria().name(), marca, p.getPrecio(),
                p.imagenPrincipal().map(ImagenPrenda::getUrl).orElse(null),
                new TablaTallas(rangos),
                p.getComposicion().stream().map(c -> new ComponenteMaterial(c.getMaterial(), c.getPorcentaje())).toList(),
                p.isTintesSinteticos(), p.isBrochesMetalicos(), p.isContieneNiquel(), p.isTratamientoFormaldehido(),
                p.getColores(), p.getEstampado(), stockPorTalla);
    }
}
