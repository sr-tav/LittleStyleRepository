package co.edu.uniquindio.littlestyle.modules.reportes.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.*;
import co.edu.uniquindio.littlestyle.modules.reportes.factory.*;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    private static final SolicitudReporte SOLICITUD =
            new SolicitudReporte(TipoReporte.VENTAS, FormatoReporte.PDF, null, null, null);
    private static final ReporteDatos DATOS =
            new ReporteDatos(TipoReporte.VENTAS, "t", "reporte-ventas", Map.of(), List.of());
    private static final ArchivoReporte ARCHIVO =
            new ArchivoReporte(new byte[]{1, 2, 3}, "reporte-ventas.pdf", MediaType.APPLICATION_PDF);

    @Mock
    private ConstructorDatosReporte constructor;
    @Mock private GeneradorReporteFactory factory;
    @Mock private CreadorGeneradorReporte creador;
    @InjectMocks
    private ReporteService service;

    @Test
    void vendedorSoloExportaSuPropioNegocio() {
        AuthenticatedUser vendedor = new AuthenticatedUser(7L, "v@t.com", "Vendedor", Rol.VENDEDOR);
        when(constructor.construir(SOLICITUD, 7L, vendedor)).thenReturn(DATOS);
        when(factory.obtener(FormatoReporte.PDF)).thenReturn(creador);
        when(creador.exportar(DATOS)).thenReturn(ARCHIVO);

        assertThat(service.exportar(SOLICITUD, vendedor)).isSameAs(ARCHIVO);
        verify(constructor).construir(SOLICITUD, 7L, vendedor);
    }

    @Test
    void administradorExportaConAlcanceGlobal() {
        AuthenticatedUser admin = new AuthenticatedUser(1L, "a@t.com", "Admin", Rol.ADMINISTRADOR);
        when(constructor.construir(SOLICITUD, null, admin)).thenReturn(DATOS);
        when(factory.obtener(FormatoReporte.PDF)).thenReturn(creador);
        when(creador.exportar(DATOS)).thenReturn(ARCHIVO);

        service.exportar(SOLICITUD, admin);

        verify(constructor).construir(SOLICITUD, null, admin);
    }

    @Test
    void clienteOAnonimoSonRechazadosSinTocarDatos() {
        AuthenticatedUser cliente = new AuthenticatedUser(2L, "c@t.com", "Cliente", Rol.CLIENTE);
        assertThatThrownBy(() -> service.exportar(SOLICITUD, cliente)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.exportar(SOLICITUD, null)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(constructor, factory);
    }

    @Test
    void unFalloDeRenderizadoSeConvierteEn500SinExponerDetalles() {
        AuthenticatedUser admin = new AuthenticatedUser(1L, "a@t.com", "Admin", Rol.ADMINISTRADOR);
        when(constructor.construir(any(), any(), any())).thenReturn(DATOS);
        when(factory.obtener(FormatoReporte.PDF)).thenReturn(creador);
        when(creador.exportar(DATOS)).thenThrow(new ReporteGeneracionException("detalle interno", new RuntimeException()));

        assertThatThrownBy(() -> service.exportar(SOLICITUD, admin))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(e.getMessage()).doesNotContain("detalle interno");
                });
    }

    @Test
    void loserroresDeFiltrosSePropaganTalCual() {
        AuthenticatedUser admin = new AuthenticatedUser(1L, "a@t.com", "Admin", Rol.ADMINISTRADOR);
        BusinessException invalido = new BusinessException(HttpStatus.BAD_REQUEST, "fechas", "startDate");
        when(constructor.construir(any(), any(), any())).thenThrow(invalido);

        assertThatThrownBy(() -> service.exportar(SOLICITUD, admin)).isSameAs(invalido);
    }
}
