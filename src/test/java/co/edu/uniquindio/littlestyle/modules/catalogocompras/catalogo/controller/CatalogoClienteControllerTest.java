package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaVitrinaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service.CatalogoClienteService;
import co.edu.uniquindio.littlestyle.modules.dashboard.service.TrackingAnalyticsService;
import co.edu.uniquindio.littlestyle.shared.dto.PaginaResponse;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test unitario del controller con MockMvc standalone: se mockea CatalogoClienteService
 * y se valida el contrato HTTP de la vitrina (paginación, filtros y errores 400).
 */
@ExtendWith(MockitoExtension.class)
class CatalogoClienteControllerTest {

    // Mock del servicio: el controller nunca toca base de datos real
    @Mock
    private CatalogoClienteService catalogo;

    // Mock del registro de métricas (US-15): el controller no debe depender de que funcione
    @Mock
    private TrackingAnalyticsService tracking;

    @InjectMocks
    private CatalogoClienteController controller;

    private MockMvc mockMvc;

    // Levanta el controller solo, con el handler de errores global para probar los códigos HTTP
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    // La vitrina pagina bien y responde con datos públicos: sin stockMinimo ni stockTotal internos
    @Test
    void explorarDevuelvePaginaSinDatosInternos() throws Exception {
        when(catalogo.explorar(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), anyInt(), anyInt(), any()))
                .thenReturn(PaginaResponse.from(new PageImpl<>(List.of(
                        new PrendaVitrinaResponse(10L, "Pantalón Jogger Azul", CategoriaPrenda.PANTALONES,
                                GeneroPrenda.NINA, "Tienda Sol", new BigDecimal("32900"), "/media/a.png", true,
                                List.of("4", "6"))))));

        mockMvc.perform(get("/api/cliente/catalogo").param("categoria", "PANTALONES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nombre").value("Pantalón Jogger Azul"))
                .andExpect(jsonPath("$.contenido[0].precio").value(32900))
                .andExpect(jsonPath("$.contenido[0].stockMinimo").doesNotExist())
                .andExpect(jsonPath("$.contenido[0].stockTotal").doesNotExist())
                .andExpect(jsonPath("$.contenido[0].tallasDisponibles[0]").value("4"))
                .andExpect(jsonPath("$.contenido[0].tallasDisponibles[1]").value("6"));
    }

    // Cuando el servicio rechaza un filtro, la excepción de negocio se traduce en un 400 limpio
    @Test
    void filtroInvalidoDevuelve400() throws Exception {
        when(catalogo.explorar(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), anyInt(), anyInt(), any()))
                .thenThrow(new BusinessException(HttpStatus.BAD_REQUEST, "La categoría no es válida", "categoria"));

        mockMvc.perform(get("/api/cliente/catalogo").param("categoria", "ROPA"))
                .andExpect(status().isBadRequest());
    }

    // El parámetro "ids" de los productos recomendados se parsea en la lista y llega al servicio intacto
    @Test
    void idsDeRecomendadosLleganAlServicio() throws Exception {
        when(catalogo.explorar(any(), any(), any(), any(), any(), any(), any(), any(), eq(List.of(10L, 20L)),
                any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(PaginaResponse.from(new PageImpl<>(List.of())));

        mockMvc.perform(get("/api/cliente/catalogo").param("ids", "10,20"))
                .andExpect(status().isOk());
    }

    // perfilId + soloMiTalla se mapean a los parámetros correctos sin necesitar ids en la URL
    @Test
    void filtroMiTallaViajaComoPerfilIdSinIdsEnLaUrl() throws Exception {
        when(catalogo.explorar(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                eq(3L), eq(true), any(), anyInt(), anyInt(), any()))
                .thenReturn(PaginaResponse.from(new PageImpl<>(List.of())));

        mockMvc.perform(get("/api/cliente/catalogo").param("perfilId", "3").param("soloMiTalla", "true"))
                .andExpect(status().isOk());
    }

    // Una página no numérica no debe tirar 500: cae en el manejo de error de negocio con 400
    @Test
    void paginaNoNumericaDevuelve400EnVezDe500() throws Exception {
        mockMvc.perform(get("/api/cliente/catalogo").param("pagina", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.pagina").value("Revisa el valor enviado"));
    }

    // Ídem con el id del detalle: "abc" en la ruta se responde como 400 y no como error interno
    @Test
    void idNoNumericoDevuelve400EnVezDe500() throws Exception {
        mockMvc.perform(get("/api/cliente/catalogo/abc"))
                .andExpect(status().isBadRequest());
    }
}
