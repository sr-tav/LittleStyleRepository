package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DespachoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.shared.event.CuentaEliminadaEvent;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Impode eliminar la cuenta de un vendedor con despachos sin enviar. Corre antes que los demás listeners: como el evento es síncrono, la excepción revierte toda la eliminación.
 */
@Component
@RequiredArgsConstructor
public class ValidacionPedidosPendientesCuentaListener {
    private static final Set<EstadoPedido> PENDIENTES =
            EnumSet.of(EstadoPedido.REGISTRADO, EstadoPedido.EN_PREPARACION);

    private final DespachoRepository despachoRepository;
    private final PrendaRepository prendaRepository;

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void alEliminarCuenta(CuentaEliminadaEvent evento) {
        // Primero se bloquean las prendas: si hay un pago confirmándose, se espera a que termine y su
        // despacho ya es visible; si aún no empieza, no encontrará la prenda y se reembolsa.
        prendaRepository.bloquearTodasDelVendedor(evento.usuarioId());
        if (despachoRepository.existsByVendedorIdAndEstadoIn(evento.usuarioId(), PENDIENTES)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Tienes pedidos pendientes de despachar. Envíalos antes de eliminar la cuenta");
        }
    }
}
