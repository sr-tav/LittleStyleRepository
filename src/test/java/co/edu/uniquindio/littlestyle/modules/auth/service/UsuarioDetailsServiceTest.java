package co.edu.uniquindio.littlestyle.modules.auth.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioDetailsService service;

    private static Usuario usuario(EstadoUsuario estado) {
        return Usuario.builder()
                .id(1L).nombre("Ana").apellido("Ruiz").email("ana@correo.com")
                .password("$2a$hash").rol(Rol.VENDEDOR).estado(estado)
                .build();
    }

    @Test
    void cargaUsuarioActivoConSuRol() {
        when(usuarioRepository.findByEmail("ana@correo.com")).thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO)));

        UserDetails details = service.loadUserByUsername("ana@correo.com");

        assertThat(details.getUsername()).isEqualTo("ana@correo.com");
        assertThat(details.getPassword()).isEqualTo("$2a$hash");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_VENDEDOR");
    }

    @Test
    void usuarioSuspendidoQuedaDeshabilitado() {
        when(usuarioRepository.findByEmail("ana@correo.com"))
                .thenReturn(Optional.of(usuario(EstadoUsuario.SUSPENDIDO)));

        assertThat(service.loadUserByUsername("ana@correo.com").isEnabled()).isFalse();
    }

    @Test
    void usuarioInexistenteLanzaExcepcion() {
        when(usuarioRepository.findByEmail("nadie@correo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nadie@correo.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
