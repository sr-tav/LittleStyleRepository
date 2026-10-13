package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaVitrinaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service.CatalogoClienteService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogoClienteControllerTest {

    @Mock
    private CatalogoClienteService catalogo;

    @InjectMocks
    private CatalogoClienteController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void explorarDevuelvePaginaSinDatosInternos() throws Exception {
        when(catalogo.explorar(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
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

    @Test
    void filtroInvalidoDevuelve400() throws Exception {
        when(catalogo.explorar(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenThrow(new BusinessException(HttpStatus.BAD_REQUEST, "La categoría no es válida", "categoria"));

        mockMvc.perform(get("/api/cliente/catalogo").param("categoria", "ROPA"))
                .andExpect(status().isBadRequest());
    }
}
