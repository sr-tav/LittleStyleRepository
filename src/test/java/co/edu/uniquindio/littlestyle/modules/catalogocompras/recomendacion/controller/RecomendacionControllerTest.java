package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.service.MotorRecomendacionService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.*;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.*;
import co.edu.uniquindio.littlestyle.shared.exception.*;
import java.math.BigDecimal; import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RecomendacionControllerTest {

    @Mock PerfilBiometricoPort perfiles;
    @Mock MotorRecomendacionService motor;
    @InjectMocks RecomendacionController controller;
    private MockMvc mockMvc;

    @BeforeEach void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    private PerfilBiometrico pb() {
        return new PerfilBiometrico(
                new DatosBiometricos(new BigDecimal("105"), new BigDecimal("18"),
                        LocalDate.now(), Holgura.REGULAR, Contextura.MEDIA),
                Set.of(AlergiaTextil.NIQUEL));
    }

    @Test void listarDevuelve200ConHeaderTiempo() throws Exception {
        when(perfiles.obtenerActivos(1L)).thenReturn(pb());
        when(motor.recomendar(any(), any())).thenReturn(List.of(
                new RecomendacionItem(1L, "Remera", new BigDecimal("45900"), "4", 95, NivelRiesgo.APTA, List.of(), true, "https://co.pinterest.com/pin/7951736838995182/")));

        mockMvc.perform(get("/api/cliente/recomendaciones").param("perfilId", "1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Recomendacion-Ms"))
                .andExpect(jsonPath("$[0].tallaSugerida").value("4"))
                .andExpect(jsonPath("$[0].puntaje").value(95));
    }

    @Test void listarSinMedicionDevuelve422() throws Exception {
        when(perfiles.obtenerActivos(1L)).thenThrow(new BusinessException(
                HttpStatus.UNPROCESSABLE_ENTITY, "El perfil no tiene mediciones"));

        mockMvc.perform(get("/api/cliente/recomendaciones").param("perfilId", "1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").value("El perfil no tiene mediciones"));
    }

    @Test void porPrendaDevuelve200() throws Exception {
        when(perfiles.obtenerActivos(1L)).thenReturn(pb());
        when(motor.recomendarParaPrenda(any(), any(), eq(1L))).thenReturn(
                new RecomendacionItem(1L, "Remera", new BigDecimal("45900"), "4", 95, NivelRiesgo.APTA, List.of(), true, "https://co.pinterest.com/pin/7951736838995182/"));

        mockMvc.perform(get("/api/cliente/recomendaciones/prenda/1").param("perfilId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tallaSugerida").value("4"));
    }

    @Test void porPrendaExcluidaDevuelve422() throws Exception {
        when(perfiles.obtenerActivos(1L)).thenReturn(pb());
        when(motor.recomendarParaPrenda(any(), any(), eq(1L))).thenThrow(new BusinessException(
                HttpStatus.UNPROCESSABLE_ENTITY, "Prenda excluida por alergia: Contiene níquel"));

        mockMvc.perform(get("/api/cliente/recomendaciones/prenda/1").param("perfilId", "1"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test void porPrendaInexistenteDevuelve404() throws Exception {
        when(perfiles.obtenerActivos(1L)).thenReturn(pb());
        when(motor.recomendarParaPrenda(any(), any(), eq(99L))).thenThrow(new BusinessException(
                HttpStatus.NOT_FOUND, "Prenda no encontrada"));

        mockMvc.perform(get("/api/cliente/recomendaciones/prenda/99").param("perfilId", "1"))
                .andExpect(status().isNotFound());
    }
}
