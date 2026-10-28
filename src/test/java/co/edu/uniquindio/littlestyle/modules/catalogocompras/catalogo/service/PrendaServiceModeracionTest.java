package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service.AlertasInventarioService;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PrendaServiceModeracionTest {

    private final PrendaRepository prendaRepository = mock(PrendaRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final ValidadorPrenda validador = mock(ValidadorPrenda.class);
    private final AlertasInventarioService alertas = mock(AlertasInventarioService.class);
    private final PrendaService servicio =
            new PrendaService(prendaRepository, usuarioRepository, validador, alertas);
    private final Prenda desactivada = Prenda.builder()
            .id(27L)
            .vendedor(Usuario.builder().id(5L).rol(Rol.VENDEDOR).build())
            .estado(EstadoPrenda.ACTIVA)
            .estadoModeracion(EstadoModeracion.DESACTIVADA)
            .motivoModeracion("Información de composición textil incompleta")
            .build();

    @BeforeEach
    void autenticarVendedor() {
        AuthenticatedUser vendedor = new AuthenticatedUser(5L, "vendedor@littlestyle.co", "Vendedor", Rol.VENDEDOR);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(vendedor, null, List.of()));
        when(prendaRepository.bloquearPropia(27L, 5L)).thenReturn(Optional.of(desactivada));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void vendedorNoPuedeReactivarPrendaDesactivadaPorModeracion() {
        assertThatThrownBy(() -> servicio.cambiarEstado(27L, EstadoPrenda.ACTIVA))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(
                        ((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("desactivada por moderación")
                .hasMessageContaining(desactivada.getMotivoModeracion())
                .hasMessageContaining("apelar");

        assertThatThrownBy(() -> servicio.cambiarEstado(27L, EstadoPrenda.INACTIVA))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no puede modificarse ni reactivarse");

        verify(prendaRepository, org.mockito.Mockito.never()).save(desactivada);
        verifyNoInteractions(alertas);
    }

    @Test
    void vendedorNoPuedeEditarPrendaDesactivadaPorModeracion() {
        assertThatThrownBy(() -> servicio.actualizar(27L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(desactivada.getMotivoModeracion())
                .hasMessageContaining("apelar");

        verify(prendaRepository).bloquearPropia(27L, 5L);
        verify(prendaRepository, org.mockito.Mockito.never()).save(desactivada);
        verifyNoInteractions(alertas);
    }
}
