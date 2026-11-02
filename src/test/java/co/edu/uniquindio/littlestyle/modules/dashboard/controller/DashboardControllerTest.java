package co.edu.uniquindio.littlestyle.modules.dashboard.controller;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.dashboard.service.DashboardAnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * El alcance de los datos sale del usuario autenticado: vendedor = su id, administrador = global. El 401/403
 * con JWT real se prueba en {@code DashboardIntegrationTest}.
 */
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 30);

    @Mock
    private DashboardAnalyticsService service;

    @InjectMocks
    private DashboardController controller;

    @Test
    void vendedorConsultaSoloSuPropioNegocio() {
        AuthenticatedUser vendedor = new AuthenticatedUser(7L, "v@tienda.com", "Vendedor", Rol.VENDEDOR);

        controller.obtenerOverview(vendedor, DESDE, HASTA, "VESTIDOS");
        controller.obtenerProductosMasConsultados(vendedor, DESDE, HASTA, null);
        controller.obtenerProductosMasVendidos(vendedor, DESDE, HASTA, null);

        verify(service).obtenerResumen(DESDE, HASTA, "VESTIDOS", 7L);
        verify(service).obtenerProductosMasConsultados(DESDE, HASTA, null, 7L);
        verify(service).obtenerProductosMasVendidos(DESDE, HASTA, null, 7L);
    }

    @Test
    void administradorConsultaAlcanceGlobal() {
        AuthenticatedUser admin = new AuthenticatedUser(1L, "a@littlestyle.com", "Admin", Rol.ADMINISTRADOR);

        controller.obtenerOverview(admin, DESDE, HASTA, null);

        verify(service).obtenerResumen(DESDE, HASTA, null, null);
    }

    @Test
    void clienteOUsuarioAnonimoSonRechazadosSinConsultarDatos() {
        AuthenticatedUser cliente = new AuthenticatedUser(2L, "c@mail.com", "Cliente", Rol.CLIENTE);

        assertThatThrownBy(() -> controller.obtenerOverview(cliente, DESDE, HASTA, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.obtenerOverview(null, DESDE, HASTA, null))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(service);
    }

    @Test
    void cadaEndpointExigeRolAdministradorOVendedor() throws NoSuchMethodException {
        List<String> metodos = List.of("obtenerOverview", "obtenerProductosMasConsultados",
                "obtenerProductosMasVendidos");
        for (String nombre : metodos) {
            var metodo = java.util.Arrays.stream(DashboardController.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals(nombre)).findFirst().orElseThrow();
            PreAuthorize regla = metodo.getAnnotation(PreAuthorize.class);
            assertThat(regla).as(nombre).isNotNull();
            assertThat(regla.value()).contains("ADMINISTRADOR").contains("VENDEDOR").doesNotContain("CLIENTE");
        }
    }
}
