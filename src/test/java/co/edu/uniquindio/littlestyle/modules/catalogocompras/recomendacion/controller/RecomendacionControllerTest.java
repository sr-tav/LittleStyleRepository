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

/**
 * Suite del controlador de recomendaciones con MockMvc standalone.
 * Se mockean el puerto de perfiles biometricos y el motor, y se prueba
 * el contrato HTTP (200/400/404/422) junto con el manejo de errores.
 */
@ExtendWith(MockitoExtension.class)
class RecomendacionControllerTest {

    @Mock PerfilBiometricoPort perfiles;
    @Mock MotorRecomendacionService motor;
    @InjectMocks RecomendacionController controller;
    private MockMvc mockMvc;

    // Monta el controlador con el advice global para validar los códigos de error
    @BeforeEach void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    // Perfil de niña con medición reciente y alergia al níquel, listo para usar
    private PerfilBiometrico pb() {
        return new PerfilBiometrico(
                new DatosBiometricos(new BigDecimal("105"), new BigDecimal("18"),
                        LocalDate.now(), Holgura.REGULAR, Contextura.MEDIA),
                Set.of(AlergiaTextil.NIQUEL), null, Set.of(), Set.of());
    }

    // Camino feliz del listado: 200 con el header de tiempo delegado, la talla y el puntaje en el cuerpo
    @Test void listarDevuelve200ConHeaderTiempo() throws Exception {
        when(perfiles.obtenerActivos(1L)).thenReturn(pb());
        when(motor.recomendar(any())).thenReturn(List.of(
                new RecomendacionItem(1L, "Remera", new BigDecimal("45900"), "4", 95, 8, NivelRiesgo.APTA, List.of(), true, "https://co.pinterest.com/pin/7951736838995182/")));

        mockMvc.perform(get("/api/cliente/recomendaciones").param("perfilId", "1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Recomendacion-Ms"))
                .andExpect(jsonPath("$[0].tallaSugerida").value("4"))
                .andExpect(jsonPath("$[0].puntaje").value(95))
                .andExpect(jsonPath("$[0].afinidad").value(8));
    }

    // Si el perfil nunca fue medido no hay nada que recomendar: 422 con el mensaje del negocio
    @Test void listarSinMedicionDevuelve422() throws Exception {
        when(perfiles.obtenerActivos(1L)).thenThrow(new BusinessException(
                HttpStatus.UNPROCESSABLE_ENTITY, "El perfil no tiene mediciones"));

        mockMvc.perform(get("/api/cliente/recomendaciones").param("perfilId", "1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").value("El perfil no tiene mediciones"));
    }

    // La medición vieja también es 422, pero además viaja el código de error para que el frontend reaccione
    @Test void listarConMedicionDesactualizadaDevuelve422ConCodigo() throws Exception {
        when(perfiles.obtenerActivos(1L)).thenThrow(new BusinessException(
                HttpStatus.UNPROCESSABLE_ENTITY, "Medición desactualizada, registra una nueva",
                "perfilId", "MEDICION_DESACTUALIZADA"));

        mockMvc.perform(get("/api/cliente/recomendaciones").param("perfilId", "1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("MEDICION_DESACTUALIZADA"));
    }

    // Recomendación para una prenda puntual: respuesta 200 con la talla sugerida
    @Test void porPrendaDevuelve200() throws Exception {
        when(perfiles.obtenerActivos(1L)).thenReturn(pb());
        when(motor.recomendarParaPrenda(any(), eq(1L))).thenReturn(
                new RecomendacionItem(1L, "Remera", new BigDecimal("45900"), "4", 95, 8, NivelRiesgo.APTA, List.of(), true, "https://co.pinterest.com/pin/7951736838995182/"));

        mockMvc.perform(get("/api/cliente/recomendaciones/prenda/1").param("perfilId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tallaSugerida").value("4"));
    }

    // Si la prenda choca con una alergia activa, el motor la rechaza con 422
    @Test void porPrendaExcluidaDevuelve422() throws Exception {
        when(perfiles.obtenerActivos(1L)).thenReturn(pb());
        when(motor.recomendarParaPrenda(any(), eq(1L))).thenThrow(new BusinessException(
                HttpStatus.UNPROCESSABLE_ENTITY, "Prenda excluida por alergia: Contiene níquel"));

        mockMvc.perform(get("/api/cliente/recomendaciones/prenda/1").param("perfilId", "1"))
                .andExpect(status().isUnprocessableEntity());
    }

    // Consultar una prenda que no existe se traduce en 404, no en un 500 genérico
    @Test void porPrendaInexistenteDevuelve404() throws Exception {
        when(perfiles.obtenerActivos(1L)).thenReturn(pb());
        when(motor.recomendarParaPrenda(any(), eq(99L))).thenThrow(new BusinessException(
                HttpStatus.NOT_FOUND, "Prenda no encontrada"));

        mockMvc.perform(get("/api/cliente/recomendaciones/prenda/99").param("perfilId", "1"))
                .andExpect(status().isNotFound());
    }

    // Endpoint liviano de ids para el frontend: devuelve la lista tal cual, sin cortarla por tope
    @Test void idsDevuelveSoloIdentificadoresSinTope() throws Exception {
        when(perfiles.obtenerActivos(1L)).thenReturn(pb());
        when(motor.idsRecomendados(any())).thenReturn(List.of(3L, 1L));

        mockMvc.perform(get("/api/cliente/recomendaciones/ids").param("perfilId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(3))
                .andExpect(jsonPath("$[1]").value(1));
    }

    // Omitir el parámetro obligatorio es un error de petición (400), con el detalle por campo
    @Test void sinPerfilIdDevuelve400EnVezDe500() throws Exception {
        mockMvc.perform(get("/api/cliente/recomendaciones"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.perfilId").value("Este parámetro es obligatorio"));
    }

    // Un texto no numérico en perfilId también cae en 400 y no en un error interno
    @Test void perfilIdNoNumericoDevuelve400EnVezDe500() throws Exception {
        mockMvc.perform(get("/api/cliente/recomendaciones").param("perfilId", "abc"))
                .andExpect(status().isBadRequest());
    }
}
