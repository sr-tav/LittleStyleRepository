package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Registro anónimo de una búsqueda en el catálogo (US-16, RNF-24).
 *
 * <p>Solo se guarda el término (normalizado y original), la categoría solicitada, el número de
 * coincidencias y, opcionalmente, el id del usuario autenticado. No se almacenan nombres, correos
 * ni direcciones: el id es el único dato personal y es opcional. La escritura es asíncrona y
 * tolerante a fallos, de modo que un problema de registro nunca rompe la búsqueda del cliente.</p>
 */
@Entity
@Table(name = "busqueda_catalogo", indexes = {
        @Index(name = "idx_busqueda_termino", columnList = "termino_normalizado"),
        @Index(name = "idx_busqueda_fecha", columnList = "creado_en"),
        @Index(name = "idx_busqueda_categoria", columnList = "categoria")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusquedaCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Término en minúsculas, sin tildes y con espacios colapsados: clave de agrupación. */
    @Column(name = "termino_normalizado", nullable = false, length = 200)
    private String terminoNormalizado;

    /** Texto tal como lo escribió el usuario, para mostrarlo en el reporte. */
    @Column(name = "termino_original", nullable = false, length = 200)
    private String terminoOriginal;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CategoriaPrenda categoria;

    @Column(name = "resultados_count", nullable = false)
    private int resultadosCount;

    /** Id del usuario autenticado; nunca su correo ni su nombre. */
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }
}
