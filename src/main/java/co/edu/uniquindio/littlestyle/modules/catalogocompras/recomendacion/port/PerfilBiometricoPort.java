package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.PerfilBiometrico;

/** Puerto del motor hacia los perfiles infantiles. */
public interface PerfilBiometricoPort {
    /** Datos vigentes del perfil del cliente autenticado para recomendar tallas. */
    PerfilBiometrico obtenerActivos(Long perfilId);
}
