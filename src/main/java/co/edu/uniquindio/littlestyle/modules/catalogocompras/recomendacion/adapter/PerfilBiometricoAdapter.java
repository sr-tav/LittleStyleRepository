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

@Component
@RequiredArgsConstructor
public class PerfilBiometricoAdapter implements PerfilBiometricoPort {
    private final PerfilInfantilRepository repo;
    private final RecomendacionProperties props;

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
                        () -> new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "El perfil no tiene mediciones"));
        if (m.getFechaMedicion().plusMonths(props.vigenciaMedidasMeses()).isBefore(LocalDate.now())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Medición desactualizada, registra una nueva");
        }
        DatosBiometricos bio = new DatosBiometricos(
                m.getEstaturaCm(), m.getPesoKg(), m.getFechaMedicion(), p.getHolgura(), p.getContextura());
        Set<AlergiaTextil> raw = p.getAlergias() == null ? Set.of() : p.getAlergias();
        Set<AlergiaTextil> al = p.isSinAlergias() ? Set.of() : Set.copyOf(raw);
        return new PerfilBiometrico(bio, al);
    }
}
