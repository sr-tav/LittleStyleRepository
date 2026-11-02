package co.edu.uniquindio.littlestyle.modules.reportes.controller;

import co.edu.uniquindio.littlestyle.config.SecurityConfig;
import co.edu.uniquindio.littlestyle.config.AlmacenamientoConfig;
import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.config.security.JwtService;
import co.edu.uniquindio.littlestyle.config.security.RestAccessDeniedHandler;
import co.edu.uniquindio.littlestyle.config.security.RestAuthenticationEntryPoint;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.service.EstadoCuentaService;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.ArchivoReporte;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.EstadoRnf24;
import co.edu.uniquindio.littlestyle.modules.reportes.dto.MetricaExitoResponse;
import co.edu.uniquindio.littlestyle.modules.reportes.service.ReporteMetricas;
import co.edu.uniquindio.littlestyle.modules.reportes.service.ReporteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica el contrato de seguridad real de US-16 con la cadena de filtros de Spring Security, no
 * solo con la anotación leída por reflexión.
 */
@WebMvcTest(controllers = ReporteController.class)
@Import({SecurityConfig.class, AlmacenamientoConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class})
class ReporteControllerSecurityTest {

    private static final AuthenticatedUser ADMIN =
            new AuthenticatedUser(1L, "admin@test.com", "Admin", Rol.ADMINISTRADOR);
    private static final AuthenticatedUser VENDEDOR =
            new AuthenticatedUser(7L, "vendedor@test.com", "Vendedor", Rol.VENDEDOR);
    private static final AuthenticatedUser CLIENTE =
            new AuthenticatedUser(9L, "cliente@test.com", "Cliente", Rol.CLIENTE);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReporteService service;
    @MockitoBean
    private ReporteMetricas metricas;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private EstadoCuentaService estadoCuentaService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void sinAutenticacionResponde401() throws Exception {
        mvc.perform(get("/api/v1/reportes/ventas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unClienteNoPuedeExportar() throws Exception {
        mvc.perform(get("/api/v1/reportes/ventas").with(autenticado(CLIENTE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unVendedorPuedeExportar() throws Exception {
        when(service.exportar(any(), any()))
                .thenReturn(new ArchivoReporte(new byte[]{1, 2, 3}, "reporte.pdf", MediaType.APPLICATION_PDF));

        mvc.perform(get("/api/v1/reportes/ventas").with(autenticado(VENDEDOR)))
                .andExpect(status().isOk());
    }

    @Test
    void unAdministradorPuedeExportar() throws Exception {
        when(service.exportar(any(), any()))
                .thenReturn(new ArchivoReporte(new byte[]{1}, "reporte.xlsx",
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));

        mvc.perform(get("/api/v1/reportes/ventas?formato=XLSX").with(autenticado(ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void unTipoDeReporteInvalidoResponde400() throws Exception {
        mvc.perform(get("/api/v1/reportes/ganancias").with(autenticado(VENDEDOR)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unFormatoInvalidoResponde400() throws Exception {
        mvc.perform(get("/api/v1/reportes/ventas?formato=DOCX").with(autenticado(VENDEDOR)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laTelemetriaConResultadoDesconocidoResponde400() throws Exception {
        mvc.perform(post("/api/v1/reportes/telemetria")
                        .with(autenticado(VENDEDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"ventas","formato":"PDF","resultado":"desconocido"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laMetricaDeExitoEsSoloParaAdministrador() throws Exception {
        when(metricas.resumen()).thenReturn(new MetricaExitoResponse(1, 0, 0, 1.0,
                EstadoRnf24.CUMPLE, true, Instant.now()));

        mvc.perform(get("/api/v1/reportes/metricas/exito").with(autenticado(VENDEDOR)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reportes/metricas/exito").with(autenticado(ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void elJsonSinEvidenciaExponeEstadoSinDatosYTasaNula() throws Exception {
        when(metricas.resumen()).thenReturn(new MetricaExitoResponse(0, 0, 0, null,
                EstadoRnf24.SIN_DATOS, false, Instant.now()));

        mvc.perform(get("/api/v1/reportes/metricas/exito").with(autenticado(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SIN_DATOS"))
                .andExpect(jsonPath("$.tasaExito").value(nullValue()))
                .andExpect(jsonPath("$.cumpleRnf24").value(false));
    }

    private static RequestPostProcessor autenticado(AuthenticatedUser usuario) {
        return authentication(new UsernamePasswordAuthenticationToken(usuario, null,
                List.of(new SimpleGrantedAuthority(usuario.rol().authority()))));
    }
}
