package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Prenda del catálogo de un vendedor. El stock se lleva por talla ({@link TallaPrenda}) y el
 * {@code stockMinimo} se compara contra el total de la prenda para emitir alertas de reabastecimiento.
 */
@Entity
@Table(name = "prendas", indexes = {
        @Index(name = "idx_prendas_vendedor", columnList = "vendedor_id"),
        @Index(name = "idx_prendas_estado", columnList = "estado"),
        @Index(name = "idx_prendas_categoria", columnList = "categoria"),
        @Index(name = "idx_prendas_precio", columnList = "precio"),
        @Index(name = "idx_prendas_actualizacion", columnList = "fecha_actualizacion")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaPrenda categoria;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private GeneroPrenda genero;

    @Column(length = 2000)
    private String descripcion;

    @Column(length = 80)
    private String marca;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoPrenda estado;

    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Estampado estampado;

    @Column(name = "tintes_sinteticos", nullable = false)
    private boolean tintesSinteticos;

    @Column(name = "broches_metalicos", nullable = false)
    private boolean brochesMetalicos;

    @Column(name = "contiene_niquel", nullable = false)
    private boolean contieneNiquel;

    @Column(name = "tratamiento_formaldehido", nullable = false)
    private boolean tratamientoFormaldehido;

    @ElementCollection(targetClass = ColorPreferido.class)
    @CollectionTable(name = "prenda_colores", joinColumns = @JoinColumn(name = "prenda_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false, length = 20)
    @BatchSize(size = 50)
    @Builder.Default
    private Set<ColorPreferido> colores = EnumSet.noneOf(ColorPreferido.class);

    @ElementCollection
    @CollectionTable(name = "prenda_composicion", joinColumns = @JoinColumn(name = "prenda_id"))
    @BatchSize(size = 50)
    @Builder.Default
    private List<ComponenteComposicion> composicion = new ArrayList<>();

    @OneToMany(mappedBy = "prenda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("estaturaMinCm ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<TallaPrenda> tallas = new ArrayList<>();

    @OneToMany(mappedBy = "prenda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<ImagenPrenda> imagenes = new ArrayList<>();

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    public int stockTotal() {
        return tallas.stream().mapToInt(TallaPrenda::getStock).sum();
    }

    public Optional<TallaPrenda> buscarTalla(String talla) {
        return tallas.stream().filter(t -> t.getTalla().equalsIgnoreCase(talla)).findFirst();
    }

    public Optional<ImagenPrenda> imagenPrincipal() {
        return imagenes.stream().findFirst();
    }
}
