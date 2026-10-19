package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Configura al arrancar la clave de cifrado de los perfiles ({@code app.crypto.perfil-key}). */
@Component
public class PerfilCryptoConfiguration {

    /** Pasa la clave a {@link CifradoPerfil}; la aplicación no arranca si falta o no es válida. */
    public PerfilCryptoConfiguration(@Value("${app.crypto.perfil-key}") String claveBase64) {
        CifradoPerfil.configurarClave(claveBase64);
    }
}
