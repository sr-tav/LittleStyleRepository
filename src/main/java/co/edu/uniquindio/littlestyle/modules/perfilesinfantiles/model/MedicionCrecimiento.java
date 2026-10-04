package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter.BigDecimalCifradoConverter;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter.FechaMedicionConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "mediciones_crecimiento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicionCrecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilInfantil perfil;

    @Convert(converter = FechaMedicionConverter.class)
    @Column(name = "fecha_medicion", nullable = false, length = 255)
    private LocalDate fechaMedicion;

    @Convert(converter = BigDecimalCifradoConverter.class)
    @Column(name = "estatura_cifrada", nullable = false, length = 255)
    private BigDecimal estaturaCm;

    @Convert(converter = BigDecimalCifradoConverter.class)
    @Column(name = "peso_cifrado", nullable = false, length = 255)
    private BigDecimal pesoKg;
}
