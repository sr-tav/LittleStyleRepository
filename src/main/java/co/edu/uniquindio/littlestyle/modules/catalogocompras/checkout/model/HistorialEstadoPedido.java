package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de un cambio de estado del pedido o de uno de sus despachos ({@code despacho} nulo si fue del
 * pedido).
 */
@Entity
@Table(name = "pedido_estados", indexes = @Index(name = "idx_historial_pedido", columnList = "pedido_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistorialEstadoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "despacho_id", nullable = true)
    private Despacho despacho;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 30)
    private EstadoPedido anterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private EstadoPedido nuevo;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    /**
     * Guía registrada en la transición a ENVIADO (RNF-13): el historial
     * conserva la guía por transición para trazabilidad sin inconsistencias.
     */
    @Column(name = "numero_guia", length = 60)
    private String numeroGuia;
}
