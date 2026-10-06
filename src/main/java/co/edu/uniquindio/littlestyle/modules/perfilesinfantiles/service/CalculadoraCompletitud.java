package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.service;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.MedicionCrecimiento;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.PerfilInfantil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CalculadoraCompletitud {

    private static final int TOTAL_CAMPOS = 8;

    public int calcular(PerfilInfantil perfil, MedicionCrecimiento ultimaMedicion) {
        return (int) Math.round((TOTAL_CAMPOS - camposPendientes(perfil, ultimaMedicion).size())
                * 100.0 / TOTAL_CAMPOS);
    }

    public List<String> camposPendientes(PerfilInfantil perfil, MedicionCrecimiento ultimaMedicion) {
        List<String> pendientes = new ArrayList<>();
        if (perfil.getNombre() == null || perfil.getNombre().isBlank()) pendientes.add("Nombre");
        if (perfil.getFechaNacimiento() == null) pendientes.add("Fecha de nacimiento");
        if (perfil.getContextura() == null) pendientes.add("Contextura");
        if (perfil.getHolgura() == null) pendientes.add("Holgura");
        if (ultimaMedicion == null || ultimaMedicion.getEstaturaCm() == null) pendientes.add("Estatura");
        if (ultimaMedicion == null || ultimaMedicion.getPesoKg() == null) pendientes.add("Peso");
        if (!perfil.isSinAlergias() && (perfil.getAlergias() == null || perfil.getAlergias().isEmpty())) {
            pendientes.add("Declarar alergias o confirmar que no tiene");
        }
        if (!perfil.getColoresPreferidos().isEmpty()
                || !perfil.getEstampadosPreferidos().isEmpty()
                || (perfil.getOtroColor() != null && !perfil.getOtroColor().isBlank())
                || (perfil.getOtroEstampado() != null && !perfil.getOtroEstampado().isBlank())) {
            return List.copyOf(pendientes);
        }
        pendientes.add("Agrega al menos un color o estampado preferido");
        return List.copyOf(pendientes);
    }
}
