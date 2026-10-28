package co.edu.uniquindio.littlestyle.modules.admin.service;

import co.edu.uniquindio.littlestyle.modules.admin.dto.ResumenAdminResponse;
import co.edu.uniquindio.littlestyle.modules.admin.model.EstadoIncidencia;
import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.admin.repository.IncidenciaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * KPIs del panel del administrador (US-14): cuentas por estado, contenido publicado vs. rechazado,
 * incidencias en curso y la metrica de resolucion del RNF-17 (ultimos 30 dias).
 */
@Service
@RequiredArgsConstructor
public class ResumenAdminService {

    private final UsuarioRepository usuarioRepository;
    private final PrendaRepository prendaRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final IncidenciaService incidenciaService;

    /** Compone el resumen con conteos individuales: cada KPI es una consulta barata por indice. */
    @Transactional(readOnly = true)
    public ResumenAdminResponse resumen() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime desde = ahora.minusDays(IncidenciaService.DIAS_METRICA);
        return new ResumenAdminResponse(
                usuarioRepository.countByEstado(EstadoUsuario.ACTIVO),
                usuarioRepository.countByEstado(EstadoUsuario.SUSPENDIDO),
                usuarioRepository.countByEstado(EstadoUsuario.BLOQUEADO),
                usuarioRepository.countByEstado(EstadoUsuario.ELIMINADO),
                prendaRepository.countByEstadoModeracion(EstadoModeracion.SIN_REVISAR),
                prendaRepository.countByEstadoAndEstadoModeracion(
                        EstadoPrenda.ACTIVA, EstadoModeracion.APROBADA),
                prendaRepository.countByEstadoModeracion(EstadoModeracion.DESACTIVADA),
                incidenciaRepository.countByEstado(EstadoIncidencia.ABIERTA),
                incidenciaRepository.countByEstado(EstadoIncidencia.EN_REVISION),
                incidenciaService.tasaResolucion(desde, ahora));
    }
}
