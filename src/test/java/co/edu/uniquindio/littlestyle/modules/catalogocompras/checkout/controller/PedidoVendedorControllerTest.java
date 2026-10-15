package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.controller;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoVendedorResumenResponse;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void listarDevuelveResumenPropio() throws Exception {
        when(pedidos.misPedidos()).thenReturn(List.of(
                new PedidoVendedorResumenResponse(7L, EstadoPedido.EN_PREPARACION, LocalDateTime.now(), 2,
                        new BigDecimal("100000"), null)));

        mockMvc.perform(get("/api/vendedor/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cantidadItemsPropios").value(2))
                .andExpect(jsonPath("$[0].stockMinimo").doesNotExist());
    }

    @Test
    void detalleAjenoDevuelve404() throws Exception {
        when(pedidos.detalle(99L)).thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        mockMvc.perform(get("/api/vendedor/pedidos/99"))
                .andExpect(status().isNotFound());
    }
}
