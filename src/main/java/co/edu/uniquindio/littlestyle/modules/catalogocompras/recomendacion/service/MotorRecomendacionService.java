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
    /** Estrategia por defecto (@Primary): HOLGURA. */
    private final EstrategiaTalla estrategia;
    /** Todas las estrategias para la selección configurable (ADR-12). */
    private final List<EstrategiaTalla> estrategias;
    private final FiltroAlergia filtro;
    private final RecomendacionProperties props;

    public List<RecomendacionItem> recomendar(PerfilBiometrico perfil) {
        return recomendar(perfil, true);
    }

    /**
     * Identificadores recomendados sin tope, para el filtro de servidor de la
     * vitrina ("Solo mi talla sugerida"). Sin límite porque la vitrina pagina
     * en la base de datos; el tope de {@code max-resultados} solo aplica a la
     * lista visible de recomendaciones.
     */
    public List<Long> idsRecomendados(PerfilBiometrico perfil) {
        return recomendar(perfil, false).stream().map(RecomendacionItem::prendaId).toList();
    }

    private List<RecomendacionItem> recomendar(PerfilBiometrico perfil, boolean limitar) {
        DatosBiometricos bio = perfil == null ? null : perfil.bio();
        Set<AlergiaTextil> al = perfil == null || perfil.alergias() == null ? Set.of() : perfil.alergias();
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
                var talla = elegirEstrategia(p).sugerir(bio, p.tablaTallas());
                if (talla.isEmpty()) continue;
                var eval = conOtraAlergia(al, perfil == null ? null : perfil.otraAlergia(), filtro.evaluar(al, p));
                if (eval.excluye()) continue;
                int puntaje = PuntajeRecomendacion.puntaje(bio, talla.get(), eval,
                        p.tieneStock(talla.get().talla()));
                int afinidad = afinidad(perfil, p);
                salida.add(new RecomendacionItem(p.id(), p.nombre(), p.precio(), talla.get().talla(),
                        puntaje, afinidad, eval.nivel(), eval.motivos(), p.tieneStock(talla.get().talla()), p.imagenUrl()));
            } catch (Exception ex) {
                log.warn("Prenda omitida por fallo aislado", ex); // una prenda nunca tumba el motor
            }
        }
        // Orden de prioridad: primero compatibilidad (talla+seguridad), la
        // afinidad solo desempata. Nunca compensa una advertencia.
        var ordenada = salida.stream()
                .sorted(Comparator.comparingInt(RecomendacionItem::puntaje)
                        .thenComparingInt(RecomendacionItem::afinidad).reversed()).toList();
        return limitar ? ordenada.stream().limit(props.maxResultados()).toList() : ordenada;
    }
    public RecomendacionItem recomendarParaPrenda(PerfilBiometrico perfil, Long prendaId) {
        DatosBiometricos bio = perfil == null ? null : perfil.bio();
        Set<AlergiaTextil> al = perfil == null || perfil.alergias() == null ? Set.of() : perfil.alergias();
        PrendaRecomendable prenda;
        try {
            prenda = catalogo.buscarPrendaPublicada(prendaId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Prenda no encontrada"));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Catálogo no disponible al consultar prenda {}", prendaId, ex); // RNF-29
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Catálogo no disponible, intenta de nuevo");
        }
        if (prenda.tablaTallas() == null || prenda.tablaTallas().estaVacia()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Sin talla compatible",
                    "prenda", "SIN_TALLA_COMPATIBLE");
        }
        RangoTalla talla = elegirEstrategia(prenda).sugerir(bio, prenda.tablaTallas())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Sin talla compatible",
                        "prenda", "SIN_TALLA_COMPATIBLE"));
        var eval = conOtraAlergia(al, perfil == null ? null : perfil.otraAlergia(), filtro.evaluar(al, prenda));
        if (eval.excluye()) throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Prenda excluida por alergia: " + String.join(", ", eval.motivos()),
                "prenda", "PRENDA_EXCLUIDA_ALERGIA");
        int puntaje = PuntajeRecomendacion.puntaje(bio, talla, eval, prenda.tieneStock(talla.talla()));
        int afinidad = afinidad(perfil, prenda);
        return new RecomendacionItem(prenda.id(), prenda.nombre(), prenda.precio(),
                talla.talla(), puntaje, afinidad, eval.nivel(), eval.motivos(), prenda.tieneStock(talla.talla()), prenda.imagenUrl());
    }

    /**
     * El texto libre de "otra alergia" antes se perdía: ahora se agrega como
     * motivo de ADVERTENCIA en todas las prendas (el alérgeno es desconocido,
     * así que no se puede descartar ninguna). Si el filtro ya excluyó, el nivel
     * se mantiene.
     */
    static EvaluacionAlergia conOtraAlergia(Set<AlergiaTextil> alergias, String otraAlergia,
                                           EvaluacionAlergia base) {
        if (otraAlergia == null || otraAlergia.isBlank()
                || alergias == null || !alergias.contains(AlergiaTextil.OTRA)) {
            return base;
        }
        List<String> motivos = new ArrayList<>(base.motivos());
        motivos.add("Otra alergia declarada: " + otraAlergia.trim());
        return new EvaluacionAlergia(base.nivel() == NivelRiesgo.EXCLUIDA ? NivelRiesgo.EXCLUIDA
                : NivelRiesgo.ADVERTENCIA, motivos);
    }

    /**
     * Selección de estrategia configurable (ADR-12): la marca manda sobre la
     * categoría y ambas sobre la estrategia por defecto. Los nombres se
     * comparan contra {@link EstrategiaTalla#nombre()} sin mayúsculas; un
     * nombre desconocido o ausente usa la estrategia por defecto sin romper.
     */
    EstrategiaTalla elegirEstrategia(PrendaRecomendable prenda) {
        String configurada = null;
        if (props.estrategiaPorMarca() != null && prenda.marca() != null) {
            configurada = props.estrategiaPorMarca().get(prenda.marca().trim().toUpperCase(Locale.ROOT));
        }
        if (configurada == null && props.estrategiaPorCategoria() != null && prenda.categoria() != null) {
            configurada = props.estrategiaPorCategoria().get(prenda.categoria().trim().toUpperCase(Locale.ROOT));
        }
        if (configurada == null) configurada = props.estrategiaPorDefecto();
        if (configurada == null || configurada.isBlank()) return estrategia;
        String buscada = configurada.trim();
        List<EstrategiaTalla> todas = estrategias == null ? List.of() : estrategias;
        for (EstrategiaTalla e : todas) {
            if (e != null && e.nombre() != null && e.nombre().equalsIgnoreCase(buscada)) return e;
        }
        log.warn("Estrategia de talla desconocida '{}', se usa la por defecto", buscada);
        return estrategia;
    }

    /**
     * Afinidad de estilo (Definición del Proyecto, proceso 1), campo aparte
     * del puntaje: +5 color, +5 estampado, con tope. Solo reordena; nunca
     * excluye ni compensa advertencias. La contextura se captura en el perfil
     * pero no puntúa.
     */
    static int afinidad(PerfilBiometrico perfil, PrendaRecomendable prenda) {
        if (perfil == null) return 0;
        return PuntajeRecomendacion.afinidad(
                perfil.coloresPreferidos(), perfil.estampadosPreferidos(), prenda);
    }

}
