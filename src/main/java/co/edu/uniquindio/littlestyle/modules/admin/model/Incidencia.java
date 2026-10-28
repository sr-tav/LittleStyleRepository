package co.edu.uniquindio.littlestyle.modules.admin.model;

import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Incidencia reportada por un cliente o vendedor, o generada por una apelación (US-14, RNF-17).
 */
@Entity
@Table(name = "incidencias", indexes = {
        @Index(name = "idx_incidencias_estado", columnList = "estado"),
        @Index(name = "idx_incidencias_reportante", columnList = "reportante_id"),
        @Index(name = "idx_incidencias_creacion", columnList = "fecha_creacion"),
        @Index(name = "idx_incidencias_resolucion", columnList = "fecha_resolucion"),
        @Index(name = "idx_incidencias_referencia", columnList = "referencia_tipo,referencia_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reportante_id", nullable = false)
    private Long reportanteId;

    @Column(name = "reportante_email", nullable = false, length = 120)
    private String reportanteEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol reportanteRol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoIncidencia tipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "referencia_tipo", length = 10)
    private TipoReferenciaIncidencia referenciaTipo;

    @Column(name = "referencia_detalle", length = 500)
    private String referenciaDetalle;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Column(nullable = false, length = 4000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoIncidencia estado;

    @Column(length = 4000)
    private String respuesta;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

}
