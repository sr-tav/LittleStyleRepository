package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Prenda/talla que no alcanzó stock cuando Wompi aprobó el pago. */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PrendaAgotadaPago {

    @Column(name = "prenda_id", nullable = false)
    private Long prendaId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String talla;

    @Column(name = "cantidad_solicitada", nullable = false)
    private int cantidadSolicitada;

    @Column(name = "stock_disponible", nullable = false)
    private int stockDisponible;
}
