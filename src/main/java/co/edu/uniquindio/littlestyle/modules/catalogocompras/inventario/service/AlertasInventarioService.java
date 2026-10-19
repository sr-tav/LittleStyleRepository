package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.event.AlertaReabastecimientoEmitida;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.AlertaReabastecimiento;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.NivelStock;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.repository.AlertaReabastecimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reglas de emisión de alertas de reabastecimiento POR TALLA: cada talla con
 * mínimo declarado alerta por su cuenta, porque las tallas venden a ritmos
 * distintos y el total ocultaba agotados. Se ejecuta dentro de la misma
 * transacción que modifica el stock o el mínimo, de modo que el cambio y sus
 * alertas se confirman o se revierten juntos.
 * <ul>
 *   <li>Stock de la talla &le; su mínimo → alerta {@code STOCK_BAJO}; stock 0 → {@code AGOTADO}.</li>
 *   <li>Talla sin mínimo declarado nunca alerta.</li>
 *   <li>Solo hay una alerta activa por (prenda, talla); no se repite mientras el nivel no cambie.</li>
 *   <li>Pasar de stock bajo a agotado emite una nueva alerta; reabastecer parcialmente (agotado → bajo)
 *       la reemplaza sin volver a notificar.</li>
 *   <li>Superar el mínimo, quitar el mínimo, quitar la talla o desactivar la prenda resuelve la alerta
 *       activa.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AlertasInventarioService {

    private final AlertaReabastecimientoRepository alertaRepository;
    private final ApplicationEventPublisher eventos;

    /** Devuelve las alertas emitidas en esta evaluación que deben notificarse. */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<AlertaReabastecimiento> evaluar(Prenda prenda) {
        List<AlertaReabastecimiento> emitidas = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();
        resolverHeradadas(prenda.getId(), ahora);
        resolverDeTallasQuitadas(prenda, ahora);
        boolean publicada = prenda.getEstado() == EstadoPrenda.ACTIVA;
        for (TallaPrenda talla : prenda.getTallas()) {
            evaluarTalla(prenda, talla, publicada, ahora).ifPresent(emitidas::add);
        }
        return emitidas;
    }

    /**
     * Aplica las reglas a una talla: resuelve la alerta activa si ya no hace falta, la reemplaza si cambió el
     * nivel y publica {@link AlertaReabastecimientoEmitida} si es la primera alerta o si la talla se agotó.
     */
    private Optional<AlertaReabastecimiento> evaluarTalla(Prenda prenda, TallaPrenda talla,
                                                         boolean publicada, LocalDateTime ahora) {
        Integer minimo = talla.getStockMinimo();
        Optional<AlertaReabastecimiento> activa = alertaRepository
                .findFirstByPrendaIdAndTallaAndFechaResolucionIsNull(prenda.getId(), talla.getTalla());
        if (!publicada || minimo == null) {
            activa.ifPresent(alerta -> alerta.setFechaResolucion(ahora));
            return Optional.empty();
        }
        NivelStock nivel = NivelStock.calcular(talla.getStock(), minimo);
        if (!nivel.requiereReabastecimiento()) {
            activa.ifPresent(alerta -> alerta.setFechaResolucion(ahora));
            return Optional.empty();
        }
        if (activa.isPresent() && activa.get().getNivel() == nivel) {
            return Optional.empty();
        }
        activa.ifPresent(alerta -> alerta.setFechaResolucion(ahora));
        AlertaReabastecimiento nueva = alertaRepository.save(AlertaReabastecimiento.builder()
                .prenda(prenda)
                .talla(talla.getTalla())
                .nivel(nivel)
                .stockAlEmitir(talla.getStock())
                .stockMinimoAlEmitir(minimo)
                .fechaEmision(ahora)
                .build());

        boolean notificar = activa.isEmpty() || nivel == NivelStock.AGOTADO;
        if (!notificar) {
            return Optional.empty();
        }
        eventos.publishEvent(new AlertaReabastecimientoEmitida(nueva.getId(), prenda.getId(),
                prenda.getVendedor().getId(), prenda.getNombre(), talla.getTalla(), nivel,
                talla.getStock(), minimo));
        return Optional.of(nueva);
    }

    /**
     * Alertas de tallas que ya no existen en la prenda (se quitaron al editarla): nadie las volvería a
     * evaluar y quedarían activas apuntando a una talla inexistente.
     */
    private void resolverDeTallasQuitadas(Prenda prenda, LocalDateTime ahora) {
        Set<String> vigentes = prenda.getTallas().stream().map(TallaPrenda::getTalla).collect(Collectors.toSet());
        alertaRepository.findAllByPrendaIdAndFechaResolucionIsNull(prenda.getId()).stream()
                .filter(alerta -> alerta.getTalla() != null && !vigentes.contains(alerta.getTalla()))
                .forEach(alerta -> alerta.setFechaResolucion(ahora));
    }

    /**
     * Alertas anteriores al mínimo por talla (sin talla informada): se resuelven
     * una vez para no duplicar ni dejar activas eternas.
     */
    private void resolverHeradadas(Long prendaId, LocalDateTime ahora) {
        alertaRepository.findAllByPrendaIdAndTallaIsNullAndFechaResolucionIsNull(prendaId)
                .forEach(alerta -> alerta.setFechaResolucion(ahora));
    }
}
