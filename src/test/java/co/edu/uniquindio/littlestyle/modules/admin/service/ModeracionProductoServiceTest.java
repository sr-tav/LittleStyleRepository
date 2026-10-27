package co.edu.uniquindio.littlestyle.modules.admin.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.admin.model.AccionAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoEntidad;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.shared.event.PrendaModeradaEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class ModeracionProductoServiceTest {

    private final PrendaRepository prendaRepository = mock(PrendaRepository.class);
    private final AuditoriaService auditoriaService = mock(AuditoriaService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ModeracionProductoService servicio =
            new ModeracionProductoService(prendaRepository, auditoriaService, eventPublisher);
    private final Prenda prenda = Prenda.builder()
            .id(44L)
            .nombre("Buzo infantil")
            .estado(EstadoPrenda.ACTIVA)
            .estadoModeracion(EstadoModeracion.SIN_REVISAR)
            .vendedor(Usuario.builder().id(5L).email("vendedor@littlestyle.co")
                    .nombreTienda("Tienda infantil").build())
            .build();

    @BeforeEach
    void autenticarAdministrador() {
        AuthenticatedUser admin = new AuthenticatedUser(99L, "admin@littlestyle.co", "Admin",
                Rol.ADMINISTRADOR);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of()));
        when(prendaRepository.bloquearPorId(44L)).thenReturn(Optional.of(prenda));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void desactivacionGuardaMotivoFechaAdminYAudita() {
        String motivo = "La ficha no informa la composición textil obligatoria";

        var respuesta = servicio.rechazar(44L, motivo);

        assertThat(respuesta.estadoModeracion()).isEqualTo(EstadoModeracion.DESACTIVADA);
        assertThat(prenda.getMotivoModeracion()).isEqualTo(motivo);
        assertThat(prenda.getFechaModeracion()).isNotNull();
        assertThat(prenda.getAdminModeracionId()).isEqualTo(99L);
        ArgumentCaptor<AccionAuditoria> accion = ArgumentCaptor.forClass(AccionAuditoria.class);
        verify(auditoriaService).registrar(accion.capture(), eq(TipoEntidad.PRENDA),
                eq("44"), eq("SIN_REVISAR"), eq("DESACTIVADA"), eq(motivo));
        assertThat(accion.getValue()).isEqualTo(AccionAuditoria.DESACTIVAR_PRODUCTO);
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(PrendaModeradaEvent.class));
    }

    @Test
    void decisionIlegalQuedaRegistradaComoIntentoFallido() {
        prenda.setEstadoModeracion(EstadoModeracion.APROBADA);

        assertThatThrownBy(() -> servicio.aprobar(44L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya esta aprobada");

        verify(auditoriaService).registrarIntentoFallido(AccionAuditoria.APROBAR_PRODUCTO,
                TipoEntidad.PRENDA, "44", "Intento de moderación fallido: La prenda ya esta aprobada");
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void motivoDeDesactivacionVacioQuedaRegistradoComoIntentoFallido() {
        assertThatThrownBy(() -> servicio.rechazar(44L, " "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("motivo de desactivación");

        verify(auditoriaService).registrarIntentoFallido(AccionAuditoria.DESACTIVAR_PRODUCTO,
                TipoEntidad.PRENDA, "44",
                "Intento de moderación fallido: El motivo de desactivación es obligatorio y debe tener entre 10 y 500 caracteres");
    }
}
