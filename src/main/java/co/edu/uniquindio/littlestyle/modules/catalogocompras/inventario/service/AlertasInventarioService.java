package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
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
import java.util.Optional;

/**
 * Reglas de emisión de alertas de reabastecimiento. Se ejecuta dentro de la misma transacción que modifica
 * el stock o el mínimo, de modo que el cambio y su alerta se confirman o se revierten juntos.
 * <ul>
 *   <li>Stock total &le; mínimo → alerta {@code STOCK_BAJO}; stock 0 → alerta {@code AGOTADO}.</li>
 *   <li>Solo hay una alerta activa por prenda; no se repite mientras el nivel no cambie.</li>
 *   <li>Pasar de stock bajo a agotado emite una nueva alerta; reabastecer parcialmente (agotado → bajo)
 *       la reemplaza sin volver a notificar.</li>
 *   <li>Superar el mínimo o desactivar la prenda resuelve la alerta activa.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AlertasInventarioService {

    private final AlertaReabastecimientoRepository alertaRepository;
    private final ApplicationEventPublisher eventos;

    /** Devuelve la alerta emitida en esta evaluación, si se emitió alguna que deba notificarse. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<AlertaReabastecimiento> evaluar(Prenda prenda) {
        int stockTotal = prenda.stockTotal();
        NivelStock nivel = NivelStock.calcular(stockTotal, prenda.getStockMinimo());
        Optional<AlertaReabastecimiento> activa = alertaRepository.findFirstByPrendaIdAndFechaResolucionIsNull(prenda.getId());
        LocalDateTime ahora = LocalDateTime.now();

        boolean publicada = prenda.getEstado() == EstadoPrenda.ACTIVA;
        if (!publicada || !nivel.requiereReabastecimiento()) {
            activa.ifPresent(alerta -> alerta.setFechaResolucion(ahora));
            return Optional.empty();
        }
        if (activa.isPresent() && activa.get().getNivel() == nivel) {
            return Optional.empty();
        }

        activa.ifPresent(alerta -> alerta.setFechaResolucion(ahora));
        AlertaReabastecimiento nueva = alertaRepository.save(AlertaReabastecimiento.builder()
                .prenda(prenda)
                .nivel(nivel)
                .stockAlEmitir(stockTotal)
                .stockMinimoAlEmitir(prenda.getStockMinimo())
                .fechaEmision(ahora)
                .build());

        boolean notificar = activa.isEmpty() || nivel == NivelStock.AGOTADO;
        if (!notificar) {
            return Optional.empty();
        }
        eventos.publishEvent(new AlertaReabastecimientoEmitida(nueva.getId(), prenda.getId(),
                prenda.getVendedor().getId(), prenda.getNombre(), nivel, stockTotal, prenda.getStockMinimo()));
        return Optional.of(nueva);
    }
}
