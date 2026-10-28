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
import jakarta.persistence.Index;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido de un cliente con su dirección (cifrada), líneas, totales y estado global. Cuando tiene prendas de
 * varios vendedores, su estado se deriva de los despachos.
 */
@Entity
@Table(name = "pedidos", indexes = {
        @Index(name = "idx_pedidos_fecha_estado", columnList = "fecha_creacion, estado")
})
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

    @Column(name = "numero_guia", length = 60)
    private String numeroGuia;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

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

    /** Agrega la línea y la asocia a este pedido. */
    public void agregar(LineaPedido item) {
        items.add(item);
        item.setPedido(this);
    }

    /** Cambia el estado si la transición es válida; 409 en otro caso. */
    public void transicionarA(EstadoPedido siguiente) {
        if (estado == null || siguiente == null || !estado.permiteTransicionA(siguiente)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "No se puede cambiar el pedido de " + estado + " a " + siguiente);
        }
        estado = siguiente;
        fechaActualizacion = LocalDateTime.now();
    }

    /**
     * Aplica el estado global derivado de los despachos (US-12 multi-vendedor).
     * No es una transición manual: el pedido madre se recalcula cada vez que
     * cambia un despacho, dentro de la misma transacción y con bloqueo pesimista.
     */
    public void aplicarEstadoDerivado(EstadoPedido derivado) {
        if (derivado == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "El estado derivado no puede ser nulo");
        }
        if (estado == EstadoPedido.CANCELADO || estado == EstadoPedido.PENDIENTE_PAGO
                || estado == EstadoPedido.PAGADO) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "No se puede recalcular el pedido en estado " + estado);
        }
        if (estado != derivado && !estado.permiteTransicionA(derivado)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "No se puede cambiar el pedido de " + estado + " a " + derivado);
        }
        estado = derivado;
        fechaActualizacion = LocalDateTime.now();
    }

    /** Cambia el estado y registra la guía, que es obligatoria al enviar y no se admite en otro paso. */
    public void avanzarA(EstadoPedido siguiente, String guia) {
        transicionarA(siguiente);
        if (siguiente == EstadoPedido.ENVIADO) {
            if (guia == null || guia.isBlank()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "El número de guía es obligatorio para enviar el pedido", "numeroGuia");
            }
            numeroGuia = guia.trim();
        } else if (guia != null && !guia.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El número de guía solo aplica al enviar el pedido", "numeroGuia");
        }
    }
}
