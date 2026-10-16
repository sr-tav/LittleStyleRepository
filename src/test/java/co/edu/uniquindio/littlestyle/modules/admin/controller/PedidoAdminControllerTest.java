package co.edu.uniquindio.littlestyle.modules.admin.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoEstadoService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PedidoAdminControllerTest {

    @Mock
    private PedidoEstadoService estados;

    @InjectMocks
    private PedidoAdminController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void avanzarDevuelve200AlCancelar() throws Exception {
        Pedido pedido = Pedido.builder().id(7L).estado(EstadoPedido.CANCELADO).build();
        when(estados.avanzar(eq(7L), eq(EstadoPedido.CANCELADO), any())).thenReturn(pedido);

        mockMvc.perform(put("/api/admin/pedidos/7/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"CANCELADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
    }

    @Test
    void transicionInvalidaDevuelve409() throws Exception {
        when(estados.avanzar(any(), any(), any())).thenThrow(new BusinessException(
                HttpStatus.CONFLICT, "No se puede cambiar el pedido"));

        mockMvc.perform(put("/api/admin/pedidos/7/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nuevo\":\"ENVIADO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void marcarPagadoAManoDevuelve409() throws Exception {
        when(estados.avanzar(eq(7L), eq(EstadoPedido.PAGADO), any())).thenThrow(new BusinessException(
                HttpStatus.CONFLICT, "El estado manual debe ser REGISTRADO, EN_PREPARACION, ENVIADO o CANCELADO"));

        mockMvc.perform(put("/api/admin/pedidos/7/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"PAGADO\"}"))
                .andExpect(status().isConflict());
    }
}
