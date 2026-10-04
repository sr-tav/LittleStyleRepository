package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "perfil_datos_cifrado_version")
public class PerfilDatosCifradoVersion {

    @Id
    @Column(length = 40)
    private String version;

    @Column(name = "completada_en", nullable = false)
    private LocalDateTime completadaEn;
}
