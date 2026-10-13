package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter.TextoLegadoCifradoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "direcciones_cliente")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DireccionCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Usuario cliente;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(nullable = false, length = 512)
    private String destinatario;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(nullable = false, length = 512)
    private String direccion;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(length = 512)
    private String complemento;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(length = 512)
    private String codigoPostal;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(length = 512)
    private String departamento;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(nullable = false, length = 512)
    private String ciudad;

    @Convert(converter = TextoLegadoCifradoConverter.class)
    @Column(nullable = false, length = 512)
    private String telefono;
}
