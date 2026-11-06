package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.model;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;
import jakarta.persistence.*;
import lombok.*;

/** Línea del carrito: una prenda en una talla, única por carrito. */
@Entity
@Table(name = "carrito_items", uniqueConstraints = @UniqueConstraint(
        name = "uk_carrito_item_color", columnNames = {"carrito_id", "prenda_id", "talla", "color", "estampado"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @Column(name = "prenda_id", nullable = false)
    private Long prendaId;

    @Column(nullable = false, length = 10)
    private String talla;

    @Column(nullable = false)
    private int cantidad;

    /** Talla sugerida por el motor al agregar; nula si se eligió sin sugerencia (RNF-11). */
    @Column(name = "talla_sugerida", length = 10)
    private String tallaSugerida;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ColorPreferido color;

    /** Estampado elegido por el cliente en el detalle; nulo en líneas anteriores a la selección de estampado. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Estampado estampado;

}
