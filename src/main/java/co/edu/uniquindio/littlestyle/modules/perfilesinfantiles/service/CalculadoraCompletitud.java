package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.service;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.MedicionCrecimiento;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.PerfilInfantil;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraCompletitud {

    private static final int TOTAL_CAMPOS = 8;

    public int calcular(PerfilInfantil perfil, MedicionCrecimiento ultimaMedicion) {
        int diligenciados = 0;
        diligenciados += perfil.getNombre() == null || perfil.getNombre().isBlank() ? 0 : 1;
        diligenciados += perfil.getFechaNacimiento() == null ? 0 : 1;
        diligenciados += perfil.getContextura() == null ? 0 : 1;
        diligenciados += perfil.getHolgura() == null ? 0 : 1;
        diligenciados += ultimaMedicion == null || ultimaMedicion.getEstaturaCm() == null ? 0 : 1;
        diligenciados += ultimaMedicion == null || ultimaMedicion.getPesoKg() == null ? 0 : 1;
        diligenciados += perfil.isSinAlergias() || (perfil.getAlergias() != null && !perfil.getAlergias().isEmpty())
                ? 1 : 0;
        diligenciados += !perfil.getColoresPreferidos().isEmpty()
                || !perfil.getEstampadosPreferidos().isEmpty()
                || (perfil.getOtroColor() != null && !perfil.getOtroColor().isBlank())
                || (perfil.getOtroEstampado() != null && !perfil.getOtroEstampado().isBlank())
                ? 1 : 0;
        return (int) Math.round(diligenciados * 100.0 / TOTAL_CAMPOS);
    }
}
