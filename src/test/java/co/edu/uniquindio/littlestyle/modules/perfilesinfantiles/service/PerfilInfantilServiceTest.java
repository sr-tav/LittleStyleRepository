package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto.PerfilRequest;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Contextura;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Holgura;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.PerfilInfantil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.MedicionCrecimientoRepository;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.PerfilInfantilRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilInfantilServiceTest {

    @Mock
    private PerfilInfantilRepository perfilRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private MedicionCrecimientoRepository medicionRepository;
    @Mock
    private ValidadorMedidas validadorMedidas;
    @Mock
    private CalculadoraCompletitud calculadoraCompletitud;
    @InjectMocks
    private PerfilInfantilService service;

    @BeforeEach
    void autenticarCliente() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        new AuthenticatedUser(51L, "cliente@test.com", "Cliente", Rol.CLIENTE),
                        null, java.util.List.of()));
    }

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ocultaPerfilesDeOtrosClientesComoNoEncontrados() {
        when(perfilRepository.findByIdAndClienteId(7L, 51L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(7L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND));
        verify(perfilRepository).findByIdAndClienteId(7L, 51L);
    }

    @Test
    void eliminaSoloTrasEncontrarPerfilDelClienteAutenticado() {
        PerfilInfantil perfil = PerfilInfantil.builder().id(7L).build();
        when(usuarioRepository.findByIdForUpdate(51L)).thenReturn(
                Optional.of(Usuario.builder().id(51L).estado(EstadoUsuario.ACTIVO).build()));
        when(perfilRepository.findByIdAndClienteId(7L, 51L)).thenReturn(Optional.of(perfil));

        service.eliminar(7L);

        verify(perfilRepository).delete(perfil);
    }

    @Test
    void rechazaElPerfilNumeroOnceConErrorAsociadoAlCampo() {
        ReflectionTestUtils.setField(service, "maximoPerfiles", 10);
        when(usuarioRepository.findByIdForUpdate(51L)).thenReturn(
                Optional.of(Usuario.builder().id(51L).estado(EstadoUsuario.ACTIVO).build()));
        when(perfilRepository.countByClienteId(51L)).thenReturn(10L);
        PerfilRequest request = new PerfilRequest("Niño", LocalDate.now().minusYears(5),
                Contextura.MEDIA, Holgura.REGULAR, Set.of(AlergiaTextil.LANA),
                null, false, Set.of(), Set.of(), null, null);

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException error = (BusinessException) ex;
                    assertThat(error.getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                    assertThat(error.getCampo()).isEqualTo("perfiles");
                });
    }

    @Test
    void exigeDescripcionCuandoSeSeleccionaOtraAlergia() {
        ReflectionTestUtils.setField(service, "maximoPerfiles", 10);
        when(usuarioRepository.findByIdForUpdate(51L)).thenReturn(
                Optional.of(Usuario.builder().id(51L).estado(EstadoUsuario.ACTIVO).build()));
        PerfilRequest request = new PerfilRequest("Niño", LocalDate.now().minusYears(5),
                Contextura.MEDIA, Holgura.REGULAR, Set.of(AlergiaTextil.OTRA),
                " ", false, Set.of(), Set.of(), null, null);

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCampo()).isEqualTo("otraAlergia"));
    }

    @Test
    void rechazaTextoLibreQueExcedeLosDiezColoresIncluyendoElOtro() {
        ReflectionTestUtils.setField(service, "maximoPerfiles", 10);
        when(usuarioRepository.findByIdForUpdate(51L)).thenReturn(
                Optional.of(Usuario.builder().id(51L).estado(EstadoUsuario.ACTIVO).build()));
        PerfilRequest request = new PerfilRequest("Niño", LocalDate.now().minusYears(5),
                Contextura.MEDIA, Holgura.REGULAR, Set.of(AlergiaTextil.LANA),
                null, false, Set.of(ColorPreferido.ROSA, ColorPreferido.LILA,
                        ColorPreferido.AZUL, ColorPreferido.CELESTE, ColorPreferido.VERDE,
                        ColorPreferido.AMARILLO, ColorPreferido.ROJO, ColorPreferido.NARANJA,
                        ColorPreferido.MORADO, ColorPreferido.BLANCO), Set.of(), "Turquesa", null);

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCampo())
                        .isEqualTo("coloresPreferidos"));
    }
}
