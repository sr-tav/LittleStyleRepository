package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import jakarta.persistence.Column;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Alerta de reabastecimiento emitida cuando una prenda alcanzó su stock mínimo o se agotó.
 * Queda activa ({@code fechaResolucion == null}) hasta que el stock vuelve a superar el mínimo,
 * lo que deja trazabilidad de que la alerta se emitió antes del agotamiento (RNF-14).
 */
@Entity
@Table(name = "alertas_reabastecimiento", indexes = {
        @Index(name = "idx_alertas_prenda_activa", columnList = "prenda_id, fecha_resolucion")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertaReabastecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prenda_id", nullable = false)
    private Prenda prenda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private NivelStock nivel;

    /** Stock total de la prenda en el momento de emitir la alerta. */
    @Column(name = "stock_al_emitir", nullable = false)
    private int stockAlEmitir;

    @Column(name = "stock_minimo_al_emitir", nullable = false)
    private int stockMinimoAlEmitir;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    public boolean estaActiva() {
        return fechaResolucion == null;
    }
}
