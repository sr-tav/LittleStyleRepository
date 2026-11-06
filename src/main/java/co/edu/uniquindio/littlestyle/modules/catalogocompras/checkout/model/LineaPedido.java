package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;
import jakarta.persistence.*;
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

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ColorPreferido color;

    /** Estampado elegido por el cliente al comprar; nulo en pedidos anteriores a la selección de estampado. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Estampado estampado;

    public String etiquetaTalla() {
        return TallaPrenda.etiqueta(talla, color);
    }

}
