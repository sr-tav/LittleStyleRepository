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
 * Búsqueda de texto hecha en el catálogo (US-15: "palabras más buscadas"). Se guarda el término normalizado y
 * el número de resultados, sin identificar al cliente: la métrica es agregada y no necesita datos personales.
 */
@Entity
@Table(name = "busquedas", indexes = {
        @Index(name = "idx_busquedas_fecha_termino", columnList = "fecha, termino")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Busqueda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Término en minúsculas, sin espacios repetidos. */
    @Column(nullable = false, length = 120)
    private String termino;

    @Column(name = "total_resultados", nullable = false)
    private long totalResultados;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
