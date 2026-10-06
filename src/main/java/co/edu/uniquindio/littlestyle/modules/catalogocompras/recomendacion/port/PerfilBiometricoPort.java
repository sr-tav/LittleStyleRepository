package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.PerfilBiometrico;

public interface PerfilBiometricoPort {
    PerfilBiometrico obtenerActivos(Long perfilId);
}
