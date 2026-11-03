package co.edu.uniquindio.littlestyle.modules.admin.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.admin.model.*;
import co.edu.uniquindio.littlestyle.modules.auth.model.*;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.shared.event.CuentaEstadoCambiadaEvent;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.modules.auth.service.AuthService;
import org.junit.jupiter.api.*;
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
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class AdminUsuarioServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final AuditoriaService auditoriaService = mock(AuditoriaService.class);
    private final IncidenciaService incidenciaService = mock(IncidenciaService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final AuthService authService = mock(AuthService.class);
    private final AdminUsuarioService servicio =
            new AdminUsuarioService(usuarioRepository, auditoriaService, incidenciaService, eventPublisher, authService);

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

    @Test
    void eliminaUnVendedorDelegandoEnAuthServiceYAuditando() {
        Usuario vendedor = usuario(42L, Rol.VENDEDOR, EstadoUsuario.ACTIVO);
        when(usuarioRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(vendedor));

        servicio.eliminar(42L, "Solicitud de cierre verificada por soporte");

        verify(authService).desactivarCuenta(42L);
        verify(auditoriaService).registrar(eq(AccionAuditoria.ELIMINAR_USUARIO), eq(TipoEntidad.USUARIO),
                eq("42"), eq("ACTIVO"), eq("ELIMINADO"), eq("Solicitud de cierre verificada por soporte"));
    }

    @Test
    void eliminarRechazaUnMotivoCorto() {
        assertThatThrownBy(() -> servicio.eliminar(42L, "corto"))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(authService);
    }

    @Test
    void eliminarRechazaCuentasAdministradorasLaPropiaYLasYaEliminadas() {
        when(usuarioRepository.findByIdForUpdate(7L))
                .thenReturn(Optional.of(usuario(7L, Rol.ADMINISTRADOR, EstadoUsuario.ACTIVO)));
        when(usuarioRepository.findByIdForUpdate(9L))
                .thenReturn(Optional.of(usuario(9L, Rol.VENDEDOR, EstadoUsuario.ACTIVO))); // id del admin autenticado
        when(usuarioRepository.findByIdForUpdate(8L))
                .thenReturn(Optional.of(usuario(8L, Rol.CLIENTE, EstadoUsuario.ELIMINADO)));

        for (long id : new long[] {7L, 9L, 8L}) {
            assertThatThrownBy(() -> servicio.eliminar(id, "Motivo suficientemente largo"))
                    .isInstanceOf(BusinessException.class);
        }
        verifyNoInteractions(authService);
        verify(auditoriaService, org.mockito.Mockito.times(3)).registrarIntentoFallido(
                eq(AccionAuditoria.ELIMINAR_USUARIO), eq(TipoEntidad.USUARIO), any(), any());
    }
}
