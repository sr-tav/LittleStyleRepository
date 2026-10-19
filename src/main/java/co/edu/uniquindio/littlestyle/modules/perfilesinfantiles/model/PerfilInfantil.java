package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter.AlergiasConverter;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter.FechaNacimientoConverter;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter.NombreCifradoConverter;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter.StringCifradoConverter;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter.TextoLegadoCifradoConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Perfil de un niño asociado a un cliente. Los datos personales y de salud se guardan cifrados; por eso
 * contextura, holgura, el indicador de alergias y las preferencias se almacenan como texto y se exponen con
 * getters y setters tipados.
 */
@Entity
@Table(name = "perfiles_infantiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilInfantil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @Convert(converter = NombreCifradoConverter.class)
    @Column(nullable = false, length = 255)
    private String nombre;

    @Convert(converter = FechaNacimientoConverter.class)
    @Column(name = "fecha_nacimiento", nullable = false, length = 255)
    private LocalDate fechaNacimiento;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(name = "contextura", nullable = false, length = 255)
    private String contexturaCifrada;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(name = "holgura", nullable = false, length = 255)
    private String holguraCifrada;

    @Convert(converter = AlergiasConverter.class)
    @Column(name = "alergias_cifradas", nullable = false, length = 255)
    @Builder.Default
    private Set<AlergiaTextil> alergias = new HashSet<>();

    @Convert(converter = StringCifradoConverter.class)
    @Column(name = "otra_alergia_cifrada", length = 255)
    private String otraAlergia;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(name = "sin_alergias", nullable = false, length = 255)
    @Builder.Default
    private String sinAlergiasCifrada = "false";

    @jakarta.persistence.ElementCollection
    @Fetch(FetchMode.SUBSELECT)
    @jakarta.persistence.CollectionTable(name = "perfil_colores_preferidos",
            joinColumns = @JoinColumn(name = "perfil_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"perfil_id", "color"}))
    @jakarta.persistence.Column(name = "color", nullable = false, length = 255)
    @jakarta.persistence.Convert(converter = TextoLegadoCifradoConverter.class)
    @Builder.Default
    private Set<String> coloresPreferidosCifrados = new HashSet<>();

    @jakarta.persistence.ElementCollection
    @Fetch(FetchMode.SUBSELECT)
    @jakarta.persistence.CollectionTable(name = "perfil_estampados_preferidos",
            joinColumns = @JoinColumn(name = "perfil_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"perfil_id", "estampado"}))
    @jakarta.persistence.Column(name = "estampado", nullable = false, length = 255)
    @jakarta.persistence.Convert(converter = TextoLegadoCifradoConverter.class)
    @Builder.Default
    private Set<String> estampadosPreferidosCifrados = new HashSet<>();

    @Convert(converter = StringCifradoConverter.class)
    @Column(name = "otro_color_cifrado", length = 255)
    private String otroColor;

    @Convert(converter = StringCifradoConverter.class)
    @Column(name = "otro_estampado_cifrado", length = 255)
    private String otroEstampado;

    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MedicionCrecimiento> mediciones = new ArrayList<>();

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    /** Contextura guardada, o {@code null}. */
    public Contextura getContextura() {
        return contexturaCifrada == null ? null : Contextura.valueOf(contexturaCifrada);
    }

    /** Guarda la contextura como texto para cifrarla. */
    public void setContextura(Contextura contextura) {
        contexturaCifrada = contextura == null ? null : contextura.name();
    }

    /** Holgura guardada, o {@code null}. */
    public Holgura getHolgura() {
        return holguraCifrada == null ? null : Holgura.valueOf(holguraCifrada);
    }

    /** Guarda la holgura como texto para cifrarla. */
    public void setHolgura(Holgura holgura) {
        holguraCifrada = holgura == null ? null : holgura.name();
    }

    /** Indica si el cliente declaró que el niño no tiene alergias. */
    public boolean isSinAlergias() {
        return Boolean.parseBoolean(sinAlergiasCifrada);
    }

    /** Guarda el indicador como texto para cifrarlo. */
    public void setSinAlergias(boolean sinAlergias) {
        sinAlergiasCifrada = Boolean.toString(sinAlergias);
    }

    /** Colores preferidos como un conjunto nuevo (los cambios no afectan la entidad). */
    public Set<ColorPreferido> getColoresPreferidos() {
        return coloresPreferidosCifrados.stream().map(ColorPreferido::valueOf)
                .collect(java.util.stream.Collectors.toCollection(
                        () -> EnumSet.noneOf(ColorPreferido.class)));
    }

    /** Reemplaza los colores preferidos; {@code null} los deja vacíos. */
    public void setColoresPreferidos(Set<ColorPreferido> coloresPreferidos) {
        coloresPreferidosCifrados = coloresPreferidos == null ? new HashSet<>()
                : coloresPreferidos.stream().map(Enum::name)
                        .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }

    /** Estampados preferidos como un conjunto nuevo (los cambios no afectan la entidad). */
    public Set<Estampado> getEstampadosPreferidos() {
        return estampadosPreferidosCifrados.stream().map(Estampado::valueOf)
                .collect(java.util.stream.Collectors.toCollection(
                        () -> EnumSet.noneOf(Estampado.class)));
    }

    /** Reemplaza los estampados preferidos; {@code null} los deja vacíos. */
    public void setEstampadosPreferidos(Set<Estampado> estampadosPreferidos) {
        estampadosPreferidosCifrados = estampadosPreferidos == null ? new HashSet<>()
                : estampadosPreferidos.stream().map(Enum::name)
                        .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }

    /** Asigna las fechas de creación y actualización al crear el perfil. */
    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        fechaCreacion = fechaCreacion == null ? ahora : fechaCreacion;
        fechaActualizacion = ahora;
    }

    /** Actualiza la fecha de modificación en cada cambio. */
    @PreUpdate
    void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
