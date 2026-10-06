package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.ComponenteRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.model.NivelStock;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.service.AlertasInventarioService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.VENDEDOR_ID;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.autenticarVendedor;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.prenda;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.prendaRequest;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.prendaRequestValida;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.tallaRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrendaServiceTest {

    @Mock
    private PrendaRepository prendaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Spy
    private ValidadorPrenda validador = new ValidadorPrenda();
    @Mock
    private AlertasInventarioService alertas;
    @InjectMocks
    private PrendaService service;

    private static final List<ComponenteRequest> ALGODON = List.of(new ComponenteRequest(MaterialTextil.ALGODON, 100));

    @BeforeEach
    void setUp() {
        autenticarVendedor();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creaLaPrendaDelVendedorAutenticadoConTallasNormalizadasYEvaluaAlertas() {
        when(usuarioRepository.getReferenceById(VENDEDOR_ID)).thenReturn(Usuario.builder().id(VENDEDOR_ID).build());

        PrendaResponse respuesta = service.crear(prendaRequestValida());

        ArgumentCaptor<Prenda> guardada = ArgumentCaptor.forClass(Prenda.class);
        verify(prendaRepository).save(guardada.capture());
        Prenda p = guardada.getValue();
        assertThat(p.getVendedor().getId()).isEqualTo(VENDEDOR_ID);
        assertThat(p.getNombre()).isEqualTo("Vestido Floral Rosa");
        assertThat(p.getEstado()).isEqualTo(EstadoPrenda.ACTIVA);
        assertThat(p.getTallas()).extracting("talla").containsExactly("4", "6");
        assertThat(p.getTallas()).extracting("stock").containsExactly(7, 5);
        assertThat(respuesta.stockTotal()).isEqualTo(12);
        assertThat(respuesta.nivelStock()).isEqualTo(NivelStock.DISPONIBLE);
        verify(alertas).evaluar(p);
    }

    @Test
    void noGuardaNadaSiLaComposicionEsInvalida() {
        var request = prendaRequest(List.of(new ComponenteRequest(MaterialTextil.ALGODON, 60)),
                List.of(tallaRequest("4", 100, 110, 15, 20, 1)));

        assertThatThrownBy(() -> service.crear(request)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(prendaRepository, alertas);
    }

    @Test
    void editarConservaElStockDeTallasExistentesAgregaNuevasYQuitaLasVacias() {
        Prenda p = prenda(10L, 5, 8, 0);
        when(prendaRepository.bloquearPropia(10L, VENDEDOR_ID)).thenReturn(Optional.of(p));
        // El formulario envía stock 99 para la talla 4 (se ignora), quita la 6 (vacía) y agrega la 8
        var request = prendaRequest(ALGODON,
                List.of(tallaRequest("4", 98, 108, 14, 19, 99), tallaRequest("8", 123, 132, 25, 30, 3)));

        PrendaResponse respuesta = service.actualizar(10L, request);

        assertThat(respuesta.tallas()).extracting(PrendaResponse.Talla::talla).containsExactly("4", "8");
        assertThat(p.buscarTalla("4")).get().satisfies(t -> {
            assertThat(t.getStock()).isEqualTo(8);
            assertThat(t.getEstaturaMinCm()).isEqualByComparingTo("98");
        });
        assertThat(p.buscarTalla("8")).get().extracting("stock").isEqualTo(3);
        assertThat(p.getComposicion()).singleElement().extracting("material").isEqualTo(MaterialTextil.ALGODON);
        verify(alertas).evaluar(p);
    }

    @Test
    void noPermiteQuitarUnaTallaConExistencias() {
        Prenda p = prenda(10L, 5, 8, 4);
        when(prendaRepository.bloquearPropia(10L, VENDEDOR_ID)).thenReturn(Optional.of(p));
        var request = prendaRequest(ALGODON, List.of(tallaRequest("4", 100, 110, 15, 20, null)));

        assertThatThrownBy(() -> service.actualizar(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("talla 6 porque tiene 4 unidades")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(alertas, never()).evaluar(any());
    }

    @Test
    void unaPrendaDeOtroVendedorSeTrataComoInexistente() {
        when(prendaRepository.findByIdAndVendedorId(77L, VENDEDOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(77L))
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void desactivarReevaluaLasAlertas() {
        Prenda p = prenda(10L, 5, 0, 0);
        when(prendaRepository.bloquearPropia(10L, VENDEDOR_ID)).thenReturn(Optional.of(p));

        PrendaResponse respuesta = service.cambiarEstado(10L, EstadoPrenda.INACTIVA);

        assertThat(respuesta.estado()).isEqualTo(EstadoPrenda.INACTIVA);
        verify(alertas).evaluar(p);
    }

    @Test
    void buscaSinDistinguirTildesNiMayusculas() {
        Prenda pantalon = prenda(10L, 5, 1, 1);
        Prenda camisa = prenda(11L, 5, 1, 1);
        camisa.setNombre("Camisa Oxford");
        camisa.setCategoria(co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda.CAMISAS);
        when(prendaRepository.findAllByVendedorIdOrderByFechaActualizacionDesc(VENDEDOR_ID))
                .thenReturn(List.of(pantalon, camisa));

        assertThat(service.listar("PANTALÓN")).extracting(PrendaResponse::id).containsExactly(10L);
        assertThat(service.listar("camisas")).extracting(PrendaResponse::id).containsExactly(11L);
        assertThat(service.listar("  ")).hasSize(2);
    }
}
