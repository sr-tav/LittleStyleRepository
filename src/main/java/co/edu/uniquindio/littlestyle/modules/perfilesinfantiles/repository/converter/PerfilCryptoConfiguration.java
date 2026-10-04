package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PerfilCryptoConfiguration {

    public PerfilCryptoConfiguration(@Value("${app.crypto.perfil-key}") String claveBase64) {
        CifradoPerfil.configurarClave(claveBase64);
    }
}
