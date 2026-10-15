package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service.CarritoService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoPagoSnapshot;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoCheckoutService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoPagoService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.ResultadoConfirmacionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.TransaccionPago;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcesadorCheckoutPagadoService {

    private final ObjectMapper objectMapper;
    private final CarritoService carritoService;
    private final PedidoCheckoutService pedidoCheckoutService;
    private final PedidoPagoService pedidoPagoService;

    @Transactional
    public ResultadoConfirmacionPago confirmar(TransaccionPago transaccion) {
        if (transaccion.getCliente() == null || transaccion.getSnapshotCheckout() == null) {
            throw new IllegalStateException("La transacción no contiene los datos del checkout");
        }
        PedidoPagoSnapshot snapshot;
        try {
            snapshot = objectMapper.readValue(transaccion.getSnapshotCheckout(), PedidoPagoSnapshot.class);
        } catch (JacksonException error) {
            throw new IllegalStateException("No fue posible recuperar los datos del checkout pagado", error);
        }

        Long clienteId = transaccion.getCliente().getId();
        carritoService.bloquearParaConfirmarPago(clienteId);
        var pedido = pedidoCheckoutService.crearPedidoDesdePago(clienteId, snapshot);
        transaccion.setPedido(pedido);
        ResultadoConfirmacionPago resultado = pedidoPagoService.confirmarPago(pedido.getId());
        if (resultado.estado() == ResultadoConfirmacionPago.Estado.CONFIRMADO) {
            carritoService.retirarComprados(clienteId, snapshot.items());
        }
        return resultado;
    }
}
