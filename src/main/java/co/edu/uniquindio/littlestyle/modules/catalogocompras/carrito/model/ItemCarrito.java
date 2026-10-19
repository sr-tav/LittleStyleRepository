package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Línea del carrito: una prenda en una talla, única por carrito. */
@Entity
@Table(name = "carrito_items", uniqueConstraints = @UniqueConstraint(
        columnNames = {"carrito_id", "prenda_id", "talla"}))
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

}
