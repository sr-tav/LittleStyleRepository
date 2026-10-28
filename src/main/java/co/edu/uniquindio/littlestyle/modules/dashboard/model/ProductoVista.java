package co.edu.uniquindio.littlestyle.modules.dashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Consulta de la ficha de una prenda por un cliente (US-15: "productos más consultados"). No guarda quién la
 * vio: solo cuenta vistas por prenda y fecha.
 */
@Entity
@Table(name = "producto_vistas", indexes = {
        @Index(name = "idx_producto_vistas_prenda_fecha", columnList = "prenda_id, fecha"),
        @Index(name = "idx_producto_vistas_fecha", columnList = "fecha")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoVista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prenda_id", nullable = false)
    private Long prendaId;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
