package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Paquete de un vendedor dentro de un pedido (uno por vendedor). Avanza de {@code REGISTRADO} a {@code
 * EN_PREPARACION} y luego a {@code ENVIADO} con guía.
 */
@Entity
@Table(name = "despachos", uniqueConstraints = @UniqueConstraint(columnNames = {"pedido_id", "vendedor_id"}))
@Getter @Setter @NoArgsConstructor
public class Despacho {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPedido estado = EstadoPedido.REGISTRADO;

    @Column(name = "numero_guia", length = 60)
    private String numeroGuia;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    /** Avanza el despacho un paso. Exige la guía para pasar a enviado y la rechaza en cualquier otro paso. */
    public void avanzarA(EstadoPedido siguiente, String guia, LocalDateTime ahora) {
        boolean valida = (estado == EstadoPedido.REGISTRADO && siguiente == EstadoPedido.EN_PREPARACION)
                || (estado == EstadoPedido.EN_PREPARACION && siguiente == EstadoPedido.ENVIADO);
        if (!valida) throw new BusinessException(HttpStatus.CONFLICT,
                "No se puede cambiar el despacho de " + estado + " a " + siguiente);
        if (siguiente == EstadoPedido.ENVIADO) {
            if (guia == null || guia.isBlank()) throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "El número de guía es obligatorio para enviar", "numeroGuia");
            numeroGuia = guia.trim();
        } else if (guia != null && !guia.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El número de guía solo aplica al enviar", "numeroGuia");
        }
        estado = siguiente;
        fechaActualizacion = ahora;
    }
}
