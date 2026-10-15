package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Clave de idempotencia para crear pedidos: si el cliente reintenta con la
 * misma clave (por ejemplo tras un timeout), recibe el pedido ya creado en
 * vez de duplicarlo. Las claves vencidas se limpian junto a la expiración.
 */
@Entity
@Table(name = "pedido_idempotencia")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaveIdempotenciaPedido {

    @Id
    @Column(length = 64)
    private String clave;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(nullable = false)
    private LocalDateTime expira;
}
