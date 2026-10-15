package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.HistorialEstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstadoPedido, Long> {
    List<HistorialEstadoPedido> findAllByPedidoIdOrderByFechaCambioAsc(Long pedidoId);
}
