package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DespachoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoVendedorService;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PedidoVendedorControllerTest {

    @Mock
    private PedidoVendedorService pedidos;

    @InjectMocks
    private PedidoVendedorController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void listarDevuelveSusDespachos() throws Exception {
        when(pedidos.misDespachos()).thenReturn(List.of(
                new DespachoResponse(5L, 7L, 31L, "Tienda", EstadoPedido.EN_PREPARACION, null,
                        LocalDateTime.now(), List.of())));

        mockMvc.perform(get("/api/vendedor/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].despachoId").value(5))
                .andExpect(jsonPath("$[0].pedidoId").value(7))
                .andExpect(jsonPath("$[0].stockMinimo").doesNotExist());
    }

    @Test
    void detalleAjenoDevuelve403() throws Exception {
        when(pedidos.detalle(9L)).thenThrow(new BusinessException(HttpStatus.FORBIDDEN,
                "El despacho pertenece a otro vendedor"));

        mockMvc.perform(get("/api/vendedor/pedidos/9"))
                .andExpect(status().isForbidden());
    }

    @Test
    void detalleInexistenteDevuelve404() throws Exception {
        when(pedidos.detalle(99L)).thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "Despacho no encontrado"));

        mockMvc.perform(get("/api/vendedor/pedidos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void avanzarAEnvioConGuiaDevuelve200() throws Exception {
        var detalle = new DespachoResponse(5L, 7L, 31L, "Tienda", EstadoPedido.ENVIADO, "COL123",
                LocalDateTime.now(), List.of());
        when(pedidos.avanzarEstado(eq(5L), any())).thenReturn(detalle);

        mockMvc.perform(put("/api/vendedor/pedidos/5/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"ENVIADO\",\"numeroGuia\":\"COL123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENVIADO"))
                .andExpect(jsonPath("$.numeroGuia").value("COL123"));
    }

    @Test
    void avanzarACanceladoDevuelve409() throws Exception {
        when(pedidos.avanzarEstado(eq(5L), any())).thenThrow(new BusinessException(
                HttpStatus.CONFLICT, "El vendedor solo puede avanzar a EN_PREPARACION o ENVIADO"));

        mockMvc.perform(put("/api/vendedor/pedidos/5/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"CANCELADO\"}"))
                .andExpect(status().isConflict());
    }
}
