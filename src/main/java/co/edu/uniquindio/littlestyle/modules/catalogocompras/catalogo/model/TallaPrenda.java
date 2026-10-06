package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;

/**
 * Fila de la tabla de tallas de una prenda: rangos de estatura y peso que cubre la talla
 * (los usa el motor de recomendaciones de US-07) y las existencias disponibles de esa talla.
 */
@Entity
@Table(name = "prenda_tallas", uniqueConstraints = @UniqueConstraint(columnNames = {"prenda_id", "talla"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TallaPrenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prenda_id", nullable = false)
    private Prenda prenda;

    @Column(nullable = false, length = 10)
    private String talla;

    @Column(name = "estatura_min_cm", nullable = false, precision = 5, scale = 1)
    private BigDecimal estaturaMinCm;

    @Column(name = "estatura_max_cm", nullable = false, precision = 5, scale = 1)
    private BigDecimal estaturaMaxCm;

    @Column(name = "peso_min_kg", nullable = false, precision = 5, scale = 1)
    private BigDecimal pesoMinKg;

    @Column(name = "peso_max_kg", nullable = false, precision = 5, scale = 1)
    private BigDecimal pesoMaxKg;

    @Column(nullable = false)
    private int stock;
}
