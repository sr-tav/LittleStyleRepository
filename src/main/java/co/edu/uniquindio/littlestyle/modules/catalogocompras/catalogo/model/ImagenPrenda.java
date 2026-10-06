package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model;

import jakarta.persistence.Column;
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

import java.time.LocalDateTime;

/**
 * Foto de una prenda. El archivo vive en el almacenamiento externo (S3, ADR-10); aquí solo se guardan
 * la URL pública y la clave del objeto, necesaria para borrarlo. La imagen con menor orden es la principal.
 */
@Entity
@Table(name = "prenda_imagenes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagenPrenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prenda_id", nullable = false)
    private Prenda prenda;

    @Column(nullable = false, length = 1024)
    private String url;

    @Column(name = "clave_almacenamiento", nullable = false, length = 512)
    private String clave;

    @Column(nullable = false)
    private int orden;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;
}
