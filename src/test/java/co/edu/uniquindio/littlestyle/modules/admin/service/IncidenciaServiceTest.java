package co.edu.uniquindio.littlestyle.modules.admin.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.admin.dto.CrearIncidenciaRequest;
import co.edu.uniquindio.littlestyle.modules.admin.model.Incidencia;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoIncidencia;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoReferenciaIncidencia;
import co.edu.uniquindio.littlestyle.modules.admin.repository.IncidenciaRepository;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class IncidenciaServiceTest {

    private final IncidenciaRepository incidenciaRepository = mock(IncidenciaRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PrendaRepository prendaRepository = mock(PrendaRepository.class);
    private final AuditoriaService auditoriaService = mock(AuditoriaService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final IncidenciaService servicio = new IncidenciaService(incidenciaRepository, usuarioRepository,
            prendaRepository, auditoriaService, eventPublisher);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reporteClienteVinculaLaPrendaParaPriorizarLaModeracion() {
        autenticar(12L, Rol.CLIENTE);
        Prenda prenda = Prenda.builder().id(44L).nombre("Buzo infantil").build();
        when(prendaRepository.findById(44L)).thenReturn(Optional.of(prenda));
        when(incidenciaRepository.save(org.mockito.ArgumentMatchers.any(Incidencia.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        CrearIncidenciaRequest request = new CrearIncidenciaRequest(
                TipoIncidencia.REPORTE_PRODUCTO, "Información incorrecta",
                "La etiqueta de composición no coincide con los materiales publicados.", 44L, null);

        var respuesta = servicio.crear(request);

        ArgumentCaptor<Incidencia> incidencia = ArgumentCaptor.forClass(Incidencia.class);
        verify(incidenciaRepository).save(incidencia.capture());
        assertThat(respuesta.tipo()).isEqualTo(TipoIncidencia.REPORTE_PRODUCTO);
        assertThat(incidencia.getValue().getReferenciaId()).isEqualTo(44L);
        assertThat(incidencia.getValue().getReferenciaTipo()).isEqualTo(TipoReferenciaIncidencia.PRENDA);
        assertThat(incidencia.getValue().getReferenciaDetalle()).isEqualTo("Buzo infantil");
    }

    @Test
    void vendedorNoPuedeCrearReporteDeProductoEnNombreDeCliente() {
        autenticar(5L, Rol.VENDEDOR);
        CrearIncidenciaRequest request = new CrearIncidenciaRequest(
                TipoIncidencia.REPORTE_PRODUCTO, "Producto reportado",
                "La información publicada no parece corresponder con el producto.", 44L, null);

        assertThatThrownBy(() -> servicio.crear(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN))
                .hasMessageContaining("Solo un cliente");
    }

    @Test
    void apelacionDeProductoSoloSeCreaParaPrendaDesactivadaDelVendedor() {
        autenticar(5L, Rol.VENDEDOR);
        Prenda prenda = Prenda.builder().id(44L).nombre("Buzo infantil")
                .estadoModeracion(EstadoModeracion.DESACTIVADA).build();
        when(prendaRepository.findByIdAndVendedorId(44L, 5L)).thenReturn(Optional.of(prenda));
        when(incidenciaRepository.save(org.mockito.ArgumentMatchers.any(Incidencia.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        CrearIncidenciaRequest request = new CrearIncidenciaRequest(
                TipoIncidencia.APELACION_PRODUCTO, "Apelación de moderación",
                "Solicito revisar la composición de este producto, ya fue corregida.", 44L, null);

        servicio.crear(request);

        ArgumentCaptor<Incidencia> incidencia = ArgumentCaptor.forClass(Incidencia.class);
        verify(incidenciaRepository).save(incidencia.capture());
        assertThat(incidencia.getValue().getReferenciaId()).isEqualTo(44L);
        assertThat(incidencia.getValue().getReferenciaTipo()).isEqualTo(TipoReferenciaIncidencia.PRENDA);
    }

    private static void autenticar(Long id, Rol rol) {
        AuthenticatedUser actual = new AuthenticatedUser(id, "persona@littlestyle.co", "Persona", rol);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actual, null, List.of()));
    }
}
