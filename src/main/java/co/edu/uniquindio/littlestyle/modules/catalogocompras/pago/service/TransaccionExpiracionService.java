package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import co.edu.uniquindio.littlestyle.config.PagoProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.EstadoTransaccionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.TransaccionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.repository.TransaccionPagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Cierra por antigüedad las compras a medio hacer: transacciones que el cliente abrió pero nunca
 * Pagó, sobre las que Wompi no volvió a avisar. Sin esto un {@code CREADA} queda para siempre
 * bloqueando nuevas compras del mismo cliente.
 *
 * <p>Pasar a {@code ANULADA} es seguro: si Wompi aprueba el cobro más tarde,
 * {@link PagoService} lo marca {@code REEMBOLSO_PENDIENTE} con motivo {@code PAGO_ANULADO} en vez
 * de ignorar el dinero (RNF-28).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransaccionExpiracionService {

    private static final List<EstadoTransaccionPago> ESTADOS_A_MEDIO_HACER =
            List.of(EstadoTransaccionPago.CREADA, EstadoTransaccionPago.PENDIENTE);

    private final TransaccionPagoRepository transaccionPagoRepository;
    private final PagoProperties pagoProperties;
    private final Clock clock;

    /** Anula las compras a medio hacer más antiguas que la ventana configurada. */
    @Transactional
    public int expirarTransaccionesVencidas() {
        LocalDateTime limite = LocalDateTime.now(clock).minus(pagoProperties.expiracionTransaccion());
        int expiradas = 0;

        for (Long id : transaccionPagoRepository.buscarIdsVencidos(ESTADOS_A_MEDIO_HACER, limite)) {
            Optional<TransaccionPago> encontrada = transaccionPagoRepository.bloquearPorId(id);
            if (encontrada.isEmpty()) {
                continue;
            }
            TransaccionPago transaccion = encontrada.get();
            // Revisa el estado y la fecha bajo el bloqueo: entre la búsqueda y ahora el webhook
            // pudo resolver el pago, y expirar una compra ya confirmada perdería la venta.
            if (!transaccion.getEstado().anulablePorCliente()
                    || transaccion.getFechaCreacion().isAfter(limite)) {
                continue;
            }
            transaccion.setEstado(EstadoTransaccionPago.ANULADA);
            transaccion.setFechaActualizacion(LocalDateTime.now(clock));
            log.info("Transacción de pago {} expirada por antigüedad ({} sin resolver); "
                            + "distinta de una anulación del cliente",
                    transaccion.getReferencia(), pagoProperties.expiracionTransaccion());
            expiradas++;
        }

        return expiradas;
    }
}