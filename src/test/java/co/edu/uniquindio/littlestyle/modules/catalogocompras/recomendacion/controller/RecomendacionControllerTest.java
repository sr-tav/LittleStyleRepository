package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.*;
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
    @Mock CatalogoRecomendacionPort catalogo;
    @Mock EstrategiaTalla estrategia;
    @Mock FiltroAlergia filtro;
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
    private PrendaRecomendable prenda() {
        return new PrendaRecomendable(1L, "Remera", "C", "M", BigDecimal.TEN, null,
                new TablaTallas(List.of(new RangoTalla("4",
                        new BigDecimal("100"), new BigDecimal("110"),
                        new BigDecimal("15"), new BigDecimal("20")))),
                List.of(new ComponenteMaterial(MaterialTextil.ALGODON, 100)),
                false, false, false, false, null, null, Map.of("4", 5));
    }

    @Test void listarDevuelve200ConHeaderTiempo() throws Exception {
        when(perfiles.obtenerActivos(1L)).thenReturn(pb());
        when(motor.recomendar(any(), any())).thenReturn(List.of(
                new RecomendacionItem(1L, "Remera", "4", 95, NivelRiesgo.APTA, List.of(), true)));

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
        when(catalogo.buscarPrendaPublicada(1L)).thenReturn(Optional.of(prenda()));
        when(estrategia.sugerir(any(), any())).thenReturn(Optional.of(prenda().tablaTallas().rangos().get(0)));
        when(filtro.evaluar(any(), any())).thenReturn(EvaluacionAlergia.apta());

        mockMvc.perform(get("/api/cliente/recomendaciones/prenda/1").param("perfilId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tallaSugerida").value("4"));
    }

    @Test void porPrendaExcluidaDevuelve422() throws Exception {
        when(perfiles.obtenerActivos(1L)).thenReturn(pb());
        when(catalogo.buscarPrendaPublicada(1L)).thenReturn(Optional.of(prenda()));
        when(estrategia.sugerir(any(), any())).thenReturn(Optional.of(prenda().tablaTallas().rangos().get(0)));
        when(filtro.evaluar(any(), any())).thenReturn(new EvaluacionAlergia(
                NivelRiesgo.EXCLUIDA, List.of("Contiene níquel")));

        mockMvc.perform(get("/api/cliente/recomendaciones/prenda/1").param("perfilId", "1"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test void porPrendaInexistenteDevuelve404() throws Exception {
        when(perfiles.obtenerActivos(1L)).thenReturn(pb());
        when(catalogo.buscarPrendaPublicada(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/cliente/recomendaciones/prenda/99").param("perfilId", "1"))
                .andExpect(status().isNotFound());
    }
}
