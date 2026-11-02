package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Línea de un pedido. Guarda nombre y precio de la prenda al comprar, para que cambios posteriores en el
 * catálogo no alteren el pedido.
 */
@Entity
@Table(name = "pedido_items", indexes = {
        @Index(name = "idx_pedido_items_prenda", columnList = "prenda_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineaPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "despacho_id")
    private  Despacho despacho;

    @Column(name = "prenda_id", nullable = false)
    private Long prendaId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 10)
    private String talla;

    @Column(nullable = false)
    private int cantidad;

    /** Talla que sugirió el motor al agregar al carrito; base de la métrica RNF-11. */
    @Column(name = "talla_sugerida", length = 10)
    private String tallaSugerida;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

}
