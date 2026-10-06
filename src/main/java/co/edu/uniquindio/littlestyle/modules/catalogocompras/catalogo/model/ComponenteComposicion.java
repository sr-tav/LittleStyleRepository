package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.MaterialTextil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Un material de la composición textil de una prenda con su porcentaje (la suma de la prenda es 100). */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ComponenteComposicion {

    @Enumerated(EnumType.STRING)
    @Column(name = "material", nullable = false, length = 20)
    private MaterialTextil material;

    @Column(name = "porcentaje", nullable = false)
    private int porcentaje;
}
