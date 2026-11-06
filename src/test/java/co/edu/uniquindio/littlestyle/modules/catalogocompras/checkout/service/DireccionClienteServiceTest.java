package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DireccionEnvioRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.DireccionCliente;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DireccionClienteRepository;
import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DireccionClienteServiceTest {

    @Mock
    private DireccionClienteRepository repository;
    @InjectMocks
    private DireccionClienteService service;

    @BeforeEach
    void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        new AuthenticatedUser(5L, "c@test.com", "Cliente", Rol.CLIENTE), null, List.of()));
        when(repository.findAllByClienteIdOrderByIdAsc(5L)).thenReturn(List.of());
        when(repository.save(any(DireccionCliente.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void guardaYDevuelveLasPartesEstructuradas() {
        var request = new DireccionEnvioRequest(" Ana Ruiz ", null, "Calle", " 45A ", "12", "34",
                "Apto 302", "630001", "Quindío", "Armenia", "3001234567");

        var respuesta = service.guardar(request);

        assertThat(respuesta.guardada()).isTrue();
        var d = respuesta.direccion();
        assertThat(d.destinatario()).isEqualTo("Ana Ruiz");
        assertThat(d.tipoVia()).isEqualTo("Calle");
        assertThat(d.numeroVia()).isEqualTo("45A");
        assertThat(d.numeroCruce()).isEqualTo("12");
        assertThat(d.placa()).isEqualTo("34");
        assertThat(d.direccion()).isEqualTo("Calle 45A # 12 - 34");
        assertThat(d.complemento()).isEqualTo("Apto 302");
    }
}
