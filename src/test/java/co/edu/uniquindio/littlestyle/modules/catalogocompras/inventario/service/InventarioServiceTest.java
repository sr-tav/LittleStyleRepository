package co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto.ActualizarStockRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto.ActualizarStockRequest.StockTalla;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto.AlertaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.dto.InventarioItemResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.AlertaReabastecimiento;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.NivelStock;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.repository.AlertaReabastecimientoRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.VENDEDOR_ID;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.autenticarVendedor;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.prenda;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock
    private PrendaRepository prendaRepository;
    @Mock
    private AlertaReabastecimientoRepository alertaRepository;
    @Mock
    private AlertasInventarioService alertas;
    @InjectMocks
    private InventarioService service;

    @BeforeEach
    void setUp() {
        autenticarVendedor();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void fijaElStockDeLasTallasIndicadasConBloqueoYEvaluaAlertas() {
        Prenda p = prenda(10L, 5, 8, 4);
        when(prendaRepository.bloquearPropia(10L, VENDEDOR_ID)).thenReturn(Optional.of(p));

        InventarioItemResponse respuesta = service.actualizarStock(10L,
                new ActualizarStockRequest(List.of(new StockTalla(" 4 ", 2)), null));

        assertThat(respuesta.tallas()).extracting(InventarioItemResponse.StockTalla::stock).containsExactly(2, 4);
        assertThat(respuesta.stockTotal()).isEqualTo(6);
        assertThat(respuesta.stockMinimo()).isEqualTo(5);
        assertThat(respuesta.nivel()).isEqualTo(NivelStock.DISPONIBLE);
        verify(alertas).evaluar(p);
    }

    @Test
    void actualizaElMinimoSiSeEnvia() {
        Prenda p = prenda(10L, 5, 3, 3);
        when(prendaRepository.bloquearPropia(10L, VENDEDOR_ID)).thenReturn(Optional.of(p));

        InventarioItemResponse respuesta = service.actualizarStock(10L,
                new ActualizarStockRequest(List.of(new StockTalla("6", 3)), 6));

        assertThat(respuesta.stockMinimo()).isEqualTo(6);
        assertThat(respuesta.nivel()).isEqualTo(NivelStock.STOCK_BAJO);
    }

    @Test
    void rechazaUnaTallaQueLaPrendaNoTieneSinEvaluarAlertas() {
        Prenda p = prenda(10L, 5, 3, 3);
        when(prendaRepository.bloquearPropia(10L, VENDEDOR_ID)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.actualizarStock(10L,
                new ActualizarStockRequest(List.of(new StockTalla("12", 3)), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La prenda no tiene la talla 12");
        verify(alertas, never()).evaluar(any());
    }

    @Test
    void rechazaTallasRepetidasEnLaMismaPeticion() {
        when(prendaRepository.bloquearPropia(10L, VENDEDOR_ID)).thenReturn(Optional.of(prenda(10L, 5, 3, 3)));

        assertThatThrownBy(() -> service.actualizarStock(10L, new ActualizarStockRequest(
                List.of(new StockTalla("4", 1), new StockTalla("4", 2)), null)))
                .hasMessage("La talla 4 está repetida");
    }

    @Test
    void noPermiteModificarElInventarioDeOtroVendedor() {
        when(prendaRepository.bloquearPropia(99L, VENDEDOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarStock(99L,
                new ActualizarStockRequest(List.of(new StockTalla("4", 1)), null)))
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void lasAlertasMuestranLosValoresActualesYLaDiferencia() {
        Prenda p = prenda(10L, 5, 2, 1);
        AlertaReabastecimiento alerta = AlertaReabastecimiento.builder().id(3L).prenda(p).nivel(NivelStock.STOCK_BAJO)
                .stockAlEmitir(4).stockMinimoAlEmitir(5).fechaEmision(LocalDateTime.now()).build();
        when(alertaRepository.findAllByPrendaVendedorIdAndFechaResolucionIsNullOrderByFechaEmisionDesc(VENDEDOR_ID))
                .thenReturn(List.of(alerta));

        AlertaResponse respuesta = service.alertasActivas().getFirst();

        assertThat(respuesta.stockActual()).isEqualTo(3);
        assertThat(respuesta.stockMinimo()).isEqualTo(5);
        assertThat(respuesta.diferencia()).isEqualTo(-2);
    }
}
