package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
