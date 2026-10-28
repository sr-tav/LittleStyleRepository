package co.edu.uniquindio.littlestyle.modules.admin.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * Registro inmutable de auditoría administrativa (US-14, RNF-08).
 */
@Entity
@Immutable
@Table(name = "auditoria_admin", indexes = {
        @Index(name = "idx_auditoria_fecha", columnList = "fecha_hora"),
        @Index(name = "idx_auditoria_admin", columnList = "admin_id"),
        @Index(name = "idx_auditoria_accion", columnList = "accion"),
        @Index(name = "idx_auditoria_entidad", columnList = "tipo_entidad,entidad_id")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private Instant fechaHora;

    @Column(name = "admin_id", nullable = false, updatable = false)
    private Long adminId;

    @Column(name = "admin_email", nullable = false, updatable = false, length = 120)
    private String adminEmail;

    @Column(nullable = false, updatable = false, length = 30)
    private String rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private AccionAuditoria accion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entidad", nullable = false, updatable = false, length = 20)
    private TipoEntidad tipoEntidad;

    @Column(name = "entidad_id", updatable = false, length = 60)
    private String entidadId;

    @Column(name = "valor_anterior", updatable = false, length = 4000)
    private String valorAnterior;

    @Column(name = "valor_nuevo", updatable = false, length = 4000)
    private String valorNuevo;

    @Column(updatable = false, length = 1000)
    private String motivo;

    @Column(name = "ip_origen", updatable = false, length = 45)
    private String ipOrigen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private ResultadoAuditoria resultado;

    @Column(name = "hash_sha256", nullable = false, updatable = false, length = 64)
    private String hash;

    @Column(name = "hash_anterior", nullable = false, updatable = false, length = 64)
    private String hashAnterior;

    /** Crea un registro nuevo con el hash calculado, sin ofrecer mutadores sobre el registro. */
    public RegistroAuditoria conHash(String hashCalculado) {
        return RegistroAuditoria.builder()
                .id(id)
                .fechaHora(fechaHora)
                .adminId(adminId)
                .adminEmail(adminEmail)
                .rol(rol)
                .accion(accion)
                .tipoEntidad(tipoEntidad)
                .entidadId(entidadId)
                .valorAnterior(valorAnterior)
                .valorNuevo(valorNuevo)
                .motivo(motivo)
                .ipOrigen(ipOrigen)
                .resultado(resultado)
                .hash(hashCalculado)
                .hashAnterior(hashAnterior)
                .build();
    }
}
