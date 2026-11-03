package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Prenda del catálogo de un vendedor, con su composición textil, atributos para alergias, colores,
 * tallas con stock e imágenes. La moderación es posterior a la publicación: solo una prenda
 * {@code DESACTIVADA} deja de ser visible (US-14).
 *
 * @see "US-14"
 * @see "RNF-08"
 */
@Entity
@Table(name = "prendas", indexes = {
        @Index(name = "idx_prendas_vendedor", columnList = "vendedor_id"),
        @Index(name = "idx_prendas_estado", columnList = "estado"),
        @Index(name = "idx_prendas_moderacion", columnList = "estado_moderacion"),
        @Index(name = "idx_prendas_genero", columnList = "genero"),
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

    /** Instrucciones de lavado y cuidado textil (Resolución Andina 2109 de 2019). */
    @Column(name = "instrucciones_cuidado", length = 1000)
    private String instruccionesCuidado;

    @Column(length = 80)
    private String marca;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoPrenda estado;

    /**
     * Moderacion del administrador (US-14). Queda SIN {@code nullable=false} a proposito: {@code ddl-auto=update}
     * agregaria la columna NOT NULL sin default y fallaria en bases con datos; la garantia de valor va por el
     * hook {@link #moderacionInicial()}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_moderacion", length = 20)
    private EstadoModeracion estadoModeracion;

    @Column(name = "motivo_moderacion", length =  500)
    private String motivoModeracion;

    @Column(name = "fecha_moderacion")
    private LocalDateTime fechaModeracion;

    @Column(name = "admin_moderacion_id")
    private Long adminModeracionId;

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

    /** Suma del stock de todas las tallas. */
    public int stockTotal() {
        return tallas.stream().mapToInt(TallaPrenda::getStock).sum();
    }

    /** Talla sin color (compatibilidad): equivale a {@code buscarVariante(talla, null)}. */
    public Optional<TallaPrenda> buscarTalla(String talla) {
        return buscarVariante(talla, null);
    }

    /** Primera imagen según su orden, que es la principal. */
    public Optional<ImagenPrenda> imagenPrincipal() {
        return imagenes.stream().findFirst();
    }

    /**
     * La revisión es posterior a la publicación (US-14): una prenda nueva nace SIN_REVISAR.
     */
    @PrePersist
    void moderacionInicial() {
        if (estadoModeracion == null) {
            estadoModeracion = EstadoModeracion.SIN_REVISAR;
        }
    }

    /**
     * Variante exacta (talla y color). Sin color pedido devuelve la única variante de esa talla; si hay varias
     * no elige por el cliente y devuelve vacío.
     */
    public Optional<TallaPrenda> buscarVariante(String talla, ColorPreferido color) {
        String nombre = talla.trim();
        List<TallaPrenda> candidatas = tallas.stream()
                .filter(t -> t.getTalla().equalsIgnoreCase(nombre)).toList();
        if (color != null) {
            return candidatas.stream().filter(t -> t.getColor() == color).findFirst();
        }
        return candidatas.size() == 1 ? Optional.of(candidatas.getFirst()) : Optional.empty();
    }
}
