package co.edu.uniquindio.littlestyle.modules.admin.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.admin.model.AccionAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoEntidad;
import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.shared.event.CuentaEstadoCambiadaEvent;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class AdminUsuarioServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final AuditoriaService auditoriaService = mock(AuditoriaService.class);
    private final IncidenciaService incidenciaService = mock(IncidenciaService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final AdminUsuarioService servicio =
            new AdminUsuarioService(usuarioRepository, auditoriaService, incidenciaService, eventPublisher);

    @BeforeEach
    void autenticarAdministrador() {
        autenticar(9L, Rol.ADMINISTRADOR);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void suspendeCuentaYRegistraAuditoriaAntesDePublicarEvento() {
        Usuario vendedor = usuario(42L, Rol.VENDEDOR, EstadoUsuario.ACTIVO);
        when(usuarioRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(vendedor));
        when(usuarioRepository.save(vendedor)).thenReturn(vendedor);
        String motivo = "Incumplimiento confirmado de las políticas de publicación";

        var respuesta = servicio.cambiarEstado(42L, EstadoUsuario.SUSPENDIDO, motivo);

        assertThat(respuesta.estado()).isEqualTo(EstadoUsuario.SUSPENDIDO);
        ArgumentCaptor<AccionAuditoria> accion = ArgumentCaptor.forClass(AccionAuditoria.class);
        verify(auditoriaService).registrar(accion.capture(), eq(TipoEntidad.USUARIO),
                eq("42"), eq("ACTIVO"), eq("SUSPENDIDO"), eq(motivo));
        assertThat(accion.getValue()).isEqualTo(AccionAuditoria.SUSPENDER_USUARIO);
        verify(incidenciaService).abrirApelacionAutomatica(42L, EstadoUsuario.SUSPENDIDO, motivo);
        verify(eventPublisher).publishEvent(any(CuentaEstadoCambiadaEvent.class));
        verify(auditoriaService, never()).registrarIntentoFallido(any(), any(), any(), any());
    }

    @Test
    void motivoVacioSeRechazaYQuedaTrazadoComoIntentoFallido() {
        assertThatThrownBy(() -> servicio.cambiarEstado(42L, EstadoUsuario.BLOQUEADO, "  "))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("motivo");

        verify(auditoriaService).registrarIntentoFallido(AccionAuditoria.BLOQUEAR_USUARIO,
                TipoEntidad.USUARIO, "42",
                "Cambio de estado rechazado: el motivo debe tener entre 10 y 500 caracteres");
        verifyNoInteractions(usuarioRepository, eventPublisher);
    }

    @Test
    void noPermiteAutosuspensionAdminSobreAdminNiCuentaEliminada() {
        Usuario cliente = usuario(9L, Rol.CLIENTE, EstadoUsuario.ACTIVO);
        Usuario otroAdmin = usuario(10L, Rol.ADMINISTRADOR, EstadoUsuario.ACTIVO);
        Usuario eliminada = usuario(11L, Rol.CLIENTE, EstadoUsuario.ELIMINADO);
        when(usuarioRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(otroAdmin));
        when(usuarioRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(eliminada));

        assertThatThrownBy(() -> cambiar(9L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("propia cuenta");
        assertThatThrownBy(() -> cambiar(10L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("administradora");
        assertThatThrownBy(() -> cambiar(11L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("eliminada");

        verify(usuarioRepository, never()).save(any());
        verify(auditoriaService, org.mockito.Mockito.times(3))
                .registrarIntentoFallido(eq(AccionAuditoria.BLOQUEAR_USUARIO), eq(TipoEntidad.USUARIO),
                        any(), any());
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(eventPublisher);
    }

    private void cambiar(Long id) {
        servicio.cambiarEstado(id, EstadoUsuario.BLOQUEADO, "Motivo administrativo suficientemente claro");
    }

    private static Usuario usuario(Long id, Rol rol, EstadoUsuario estado) {
        return Usuario.builder()
                .id(id)
                .nombre("Usuario")
                .apellido("Prueba")
                .email("usuario" + id + "@littlestyle.co")
                .rol(rol)
                .estado(estado)
                .fechaRegistro(LocalDateTime.now())
                .build();
    }

    private static void autenticar(Long id, Rol rol) {
        AuthenticatedUser actual = new AuthenticatedUser(id, "admin@littlestyle.co", "Admin", rol);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actual, null, List.of()));
    }
}
