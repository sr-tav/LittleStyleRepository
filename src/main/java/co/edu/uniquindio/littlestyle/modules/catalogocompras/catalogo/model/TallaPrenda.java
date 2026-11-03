package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import jakarta.persistence.*;
import lombok.*;
import java.util.Locale;
import java.math.BigDecimal;

/**
 * Fila de la tabla de tallas de una prenda: rangos de estatura y peso que cubre la talla
 * (los usa el motor de recomendaciones de US-07) y las existencias disponibles de esa talla.
 */
@Entity
@Table(name = "prenda_tallas", uniqueConstraints = @UniqueConstraint(
        name = "uk_prenda_talla_color", columnNames = {"prenda_id", "talla", "color"}))
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

    /**
     * Edad orientativa en meses que cubre la talla (criterio de la US-09).
     * Nula = la talla no declara rango de edad y coincide con cualquier filtro.
     */
    @Column(name = "edad_min_meses")
    private Integer edadMinMeses;

    @Column(name = "edad_max_meses")
    private Integer edadMaxMeses;

    /**
     * Mínimo de esta talla para alertas de reabastecimiento. Nulo = la talla
     * no alerta (cada talla es un SKU con demanda propia).
     */
    @Column(name = "stock_minimo")
    private Integer stockMinimo;

    @Column(nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ColorPreferido color;

    public static String etiqueta(String talla, ColorPreferido color) {
        if (color == null) return talla;
        String nombre = color.name();
        return talla + " · " + nombre.charAt(0) + nombre.substring(1).toLowerCase(Locale.ROOT);
    }

    public String etiqueta() {
        return etiqueta(talla, color);
    }
}
