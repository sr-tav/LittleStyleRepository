package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter.TextoLegadoCifradoConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transacciones_pago", indexes = {
/**
 * Intento de pago con Wompi. Antes de aprobarse guarda la compra como snapshot cifrado; el pedido se crea
 * solo cuando Wompi aprueba el cobro.
 */
        @Index(name = "idx_transaccion_pago_pedido", columnList = "pedido_id"),
        @Index(name = "idx_transaccion_pago_wompi", columnList = "wompi_transaccion_id"),
        @Index(name = "idx_transaccion_pago_cliente", columnList = "cliente_id"),
        @Index(name = "uk_transaccion_cliente_idempotencia",
                columnList = "cliente_id, clave_idempotencia", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(name = "snapshot_checkout", columnDefinition = "TEXT")
    private String snapshotCheckout;

    @Column(name = "clave_idempotencia", length = 64)
    private String claveIdempotencia;

    @Column(nullable = false, unique = true, length = 60)
    private String referencia;

    @Column(name = "wompi_transaccion_id", length = 80)
    private String wompiTransaccionId;

    @Column(name = "monto_centavos", nullable = false)
    private long montoCentavos;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoTransaccionPago estado;

    @Column(name = "metodo_pago", length = 40)
    private String metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_reembolso", length = 30)
    @Setter(AccessLevel.NONE)
    private MotivoReembolso motivoReembolso;

    @ElementCollection
    @CollectionTable(name = "transacciones_pago_agotadas",
            joinColumns = @JoinColumn(name = "transaccion_id"))
    @Builder.Default
    @Setter(AccessLevel.NONE)
    private List<PrendaAgotadaPago> prendasAgotadas = new ArrayList<>();

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    /** El cobro fue aprobado por Wompi, pero el pedido no se pudo pagar; requiere devolver el dinero. */
    public void marcarReembolsoPendiente(MotivoReembolso motivo, List<PrendaAgotadaPago> agotadas) {
        estado = EstadoTransaccionPago.REEMBOLSO_PENDIENTE;
        motivoReembolso = motivo;
        prendasAgotadas.clear();
        prendasAgotadas.addAll(agotadas);
    }
}
