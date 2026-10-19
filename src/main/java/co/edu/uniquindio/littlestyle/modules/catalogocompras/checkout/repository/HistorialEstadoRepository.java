package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.HistorialEstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso al historial de estados de los pedidos. */
public interface HistorialEstadoRepository extends JpaRepository<HistorialEstadoPedido, Long> {
    /** Historial de un pedido en orden cronológico. */
    List<HistorialEstadoPedido> findAllByPedidoIdOrderByFechaCambioAsc(Long pedidoId);
}
