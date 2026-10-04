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

    public Contextura getContextura() {
        return contexturaCifrada == null ? null : Contextura.valueOf(contexturaCifrada);
    }

    public void setContextura(Contextura contextura) {
        contexturaCifrada = contextura == null ? null : contextura.name();
    }

    public Holgura getHolgura() {
        return holguraCifrada == null ? null : Holgura.valueOf(holguraCifrada);
    }

    public void setHolgura(Holgura holgura) {
        holguraCifrada = holgura == null ? null : holgura.name();
    }

    public boolean isSinAlergias() {
        return Boolean.parseBoolean(sinAlergiasCifrada);
    }

    public void setSinAlergias(boolean sinAlergias) {
        sinAlergiasCifrada = Boolean.toString(sinAlergias);
    }

    public Set<ColorPreferido> getColoresPreferidos() {
        return coloresPreferidosCifrados.stream().map(ColorPreferido::valueOf)
                .collect(java.util.stream.Collectors.toCollection(
                        () -> EnumSet.noneOf(ColorPreferido.class)));
    }

    public void setColoresPreferidos(Set<ColorPreferido> coloresPreferidos) {
        coloresPreferidosCifrados = coloresPreferidos == null ? new HashSet<>()
                : coloresPreferidos.stream().map(Enum::name)
                        .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }

    public Set<Estampado> getEstampadosPreferidos() {
        return estampadosPreferidosCifrados.stream().map(Estampado::valueOf)
                .collect(java.util.stream.Collectors.toCollection(
                        () -> EnumSet.noneOf(Estampado.class)));
    }

    public void setEstampadosPreferidos(Set<Estampado> estampadosPreferidos) {
        estampadosPreferidosCifrados = estampadosPreferidos == null ? new HashSet<>()
                : estampadosPreferidos.stream().map(Enum::name)
                        .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }

    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        fechaCreacion = fechaCreacion == null ? ahora : fechaCreacion;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
