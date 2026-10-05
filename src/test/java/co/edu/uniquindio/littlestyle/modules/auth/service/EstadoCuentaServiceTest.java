package co.edu.uniquindio.littlestyle.modules.auth.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstadoCuentaServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EstadoCuentaService estadoCuentaService;

    @Test
    void cuentaActiva() {
        when(usuarioRepository.findEstadoById(1L)).thenReturn(Optional.of(EstadoUsuario.ACTIVO));

        assertThat(estadoCuentaService.estaActiva(1L)).isTrue();
    }

    @Test
    void cuentaSuspendida() {
        when(usuarioRepository.findEstadoById(1L)).thenReturn(Optional.of(EstadoUsuario.SUSPENDIDO));

        assertThat(estadoCuentaService.estaActiva(1L)).isFalse();
    }

    @Test
    void cuentaInexistente() {
        when(usuarioRepository.findEstadoById(1L)).thenReturn(Optional.empty());

        assertThat(estadoCuentaService.estaActiva(1L)).isFalse();
    }

    @Test
    void tokenSinIdDeUsuario() {
        assertThat(estadoCuentaService.estaActiva(null)).isFalse();
        verifyNoInteractions(usuarioRepository);
    }
}
