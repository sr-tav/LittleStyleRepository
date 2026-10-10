package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter.TextoLegadoCifradoConverter;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @Column(name = "perfil_infantil_id", nullable = false)
    private Long perfilInfantilId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Setter(AccessLevel.NONE)
    private EstadoPedido estado;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(nullable = false, length = 512)
    private String destinatario;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(nullable = false, length = 512)
    private String direccion;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(length = 512)
    private String complemento;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(length = 512)
    private String codigoPostal;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(length = 512)
    private String departamento;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(nullable = false, length = 512)
    private String ciudad;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(nullable = false, length = 512)
    private String telefono;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "costo_envio", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoEnvio;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<LineaPedido> items = new ArrayList<>();

    public void agregar(LineaPedido item) {
        items.add(item);
        item.setPedido(this);
    }

    public void transicionarA(EstadoPedido siguiente) {
        if (estado == null || siguiente == null || !estado.permiteTransicionA(siguiente)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "No se puede cambiar el pedido de " + estado + " a " + siguiente);
        }
        estado = siguiente;
    }
}
