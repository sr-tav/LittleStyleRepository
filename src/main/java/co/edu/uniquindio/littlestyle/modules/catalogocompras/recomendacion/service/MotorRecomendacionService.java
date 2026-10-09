package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.service;

import co.edu.uniquindio.littlestyle.config.RecomendacionProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.PrendaRecomendable;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.RangoTalla;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port.CatalogoRecomendacionPort;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.*;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import java.util.*;

import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MotorRecomendacionService {
    private final CatalogoRecomendacionPort catalogo;
    private final EstrategiaTalla estrategia;
    private final FiltroAlergia filtro;
    private final RecomendacionProperties props;

    public List<RecomendacionItem> recomendar(DatosBiometricos bio, Set<AlergiaTextil> alergias) {
        Set<AlergiaTextil> al = alergias == null ? Set.of() : alergias;
        List<PrendaRecomendable> prendas;
        try {
            prendas = catalogo.listarPrendasPublicadas();
        } catch (Exception ex) {
            log.warn("Catálogo no disponible, degradación a lista vacía", ex); // RNF-29
            return List.of();
        }
        if (prendas == null || prendas.isEmpty()) return List.of();
        List<RecomendacionItem> salida = new ArrayList<>();
        for (PrendaRecomendable p : prendas) {
            try {
                if (p == null || p.tablaTallas().estaVacia()) continue;
                var talla = estrategia.sugerir(bio, p.tablaTallas());
                if (talla.isEmpty()) continue;
                var eval = filtro.evaluar(al, p);
                if (eval.excluye()) continue;
                int puntaje = PuntajeRecomendacion.puntaje(bio, talla.get(), eval, p.tieneStock(talla.get().talla()));
                salida.add(new RecomendacionItem(p.id(), p.nombre(), p.precio(), talla.get().talla(),
                        puntaje, eval.nivel(), eval.motivos(), p.tieneStock(talla.get().talla()), p.imagenUrl()));
            } catch (Exception ex) {
                log.warn("Prenda omitida por fallo aislado", ex); // una prenda nunca tumba el motor
            }
        }
        return salida.stream()
                .sorted(Comparator.comparingInt(RecomendacionItem::puntaje).reversed())
                .limit(props.maxResultados()).toList();
    }
    public RecomendacionItem recomendarParaPrenda(DatosBiometricos bio, Set<AlergiaTextil> alergias, Long prendaId) {
        Set<AlergiaTextil> al = alergias == null ? Set.of() : alergias;
        PrendaRecomendable prenda = catalogo.buscarPrendaPublicada(prendaId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Prenda no encontrada"));
        RangoTalla talla = estrategia.sugerir(bio, prenda.tablaTallas())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Sin talla compatible"));
        var eval = filtro.evaluar(al, prenda);
        if (eval.excluye()) throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Prenda excluida por alergia: " + String.join(", ", eval.motivos()));
        int puntaje = PuntajeRecomendacion.puntaje(bio, talla, eval, prenda.tieneStock(talla.talla()));
        return new RecomendacionItem(prenda.id(), prenda.nombre(), prenda.precio(),
                talla.talla(), puntaje, eval.nivel(), eval.motivos(), prenda.tieneStock(talla.talla()), prenda.imagenUrl());
    }

}
