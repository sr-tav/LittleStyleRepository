package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.adapter;

import co.edu.uniquindio.littlestyle.config.RecomendacionProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port.PerfilBiometricoPort;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.*;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.PerfilInfantilRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import java.time.LocalDate; import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Obtiene del módulo de perfiles infantiles los datos que necesita el motor de recomendación. */
@Component
@RequiredArgsConstructor
public class PerfilBiometricoAdapter implements PerfilBiometricoPort {
    private final PerfilInfantilRepository repo;
    private final RecomendacionProperties props;

    /**
     * Datos del perfil del cliente autenticado con su medición más reciente. Responde 422 si no tiene
     * mediciones o si la última supera la vigencia configurada. El texto de "otra alergia" solo se incluye si
     * se marcó esa alergia.
     */
    @Override
    @Transactional(readOnly = true)
    public PerfilBiometrico obtenerActivos(Long perfilId) {
        Long clienteId = SecurityUtils.usuarioActual()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado")).id();
        PerfilInfantil p = repo.findByIdAndClienteId(perfilId, clienteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Perfil infantil no encontrado"));
        List<MedicionCrecimiento> meds = p.getMediciones() == null ? List.of() : p.getMediciones();
        MedicionCrecimiento m = meds.stream()
                .max(Comparator.comparing(MedicionCrecimiento::getFechaMedicion)).orElseThrow(
                        () -> new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "El perfil no tiene mediciones",
                                "perfilId", "PERFIL_SIN_MEDICIONES"));
        if (m.getFechaMedicion().plusMonths(props.vigenciaMedidasMeses()).isBefore(LocalDate.now())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Medición desactualizada, registra una nueva",
                    "perfilId", "MEDICION_DESACTUALIZADA");
        }
        DatosBiometricos bio = new DatosBiometricos(
                m.getEstaturaCm(), m.getPesoKg(), m.getFechaMedicion(), p.getHolgura(), p.getContextura());
        Set<AlergiaTextil> raw = p.getAlergias() == null ? Set.of() : p.getAlergias();
        Set<AlergiaTextil> al = p.isSinAlergias() ? Set.of() : Set.copyOf(raw);
        // El texto libre solo aplica si declaró OTRA: sin ese marcador se ignora
        // para no advertir por un campo residual de otro perfil.
        String otra = !p.isSinAlergias() && al.contains(AlergiaTextil.OTRA)
                && p.getOtraAlergia() != null && !p.getOtraAlergia().isBlank()
                ? p.getOtraAlergia().trim() : null;
        return new PerfilBiometrico(bio, al, otra, p.getColoresPreferidos(), p.getEstampadosPreferidos());
    }
}
