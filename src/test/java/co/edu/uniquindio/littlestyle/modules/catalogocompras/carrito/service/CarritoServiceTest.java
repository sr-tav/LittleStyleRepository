package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.dto.AgregarItemCarritoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.model.Carrito;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ComponenteComposicion;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.repository.CarritoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.service.ValidadorAlergiaCompra;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.FiltroAlergiaEstandar;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto.PerfilResponse;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Contextura;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Holgura;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.service.PerfilInfantilService;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Valida el bloqueo de agregado al carrito por alergias graves (excluyentes).
 * Usa el filtro de alergias real: solo las reglas excluyentes (níquel,
 * formaldehído y ≥30% fibras sintéticas) bloquean; las demás son advertencia.
 */
@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    private static final Long CLIENTE_ID = 42L;

    @Mock
    private CarritoRepository carritoRepository;
    @Mock
    private PrendaRepository prendaRepository;
    @Mock
    private PerfilInfantilService perfilInfantilService;

    private CarritoService servicio;
    private Carrito carrito;

    @BeforeEach
    void setUp() {
        servicio = new CarritoService(carritoRepository, prendaRepository,
                new DisponibilidadStockService(), perfilInfantilService,
                new ValidadorAlergiaCompra(new FiltroAlergiaEstandar()));
        carrito = Carrito.builder()
                .cliente(Usuario.builder().id(CLIENTE_ID).build())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        new AuthenticatedUser(CLIENTE_ID, "cliente@test.com", "Cliente", Rol.CLIENTE),
                        null, List.of()));
    }

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bloqueaAgregarLaPrendaExcluidaPorLaAlergiaGraveDelPerfilIndicado() {
        Prenda prenda = CatalogoFixtures.prenda(7L, 2, 8, 4);
        prenda.setContieneNiquel(true);
        prepararCarrito(prenda);
        when(perfilInfantilService.obtener(11L)).thenReturn(perfil(11L, Set.of(AlergiaTextil.NIQUEL), false));

        assertThatThrownBy(() -> servicio.agregar(solicitud(11L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException excepcion = (BusinessException) error;
                    assertThat(excepcion.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(excepcion.getCodigo()).isEqualTo("PRENDA_EXCLUIDA_ALERGIA");
                    assertThat(excepcion.getCampo()).isEqualTo("items");
                });
        verify(carritoRepository, never()).saveAndFlush(any(Carrito.class));
    }

    @Test
    void bloqueaAgregarLaPrendaConElUmbralDeFibrasSinteticasGraves() {
        Prenda prenda = CatalogoFixtures.prenda(7L, 2, 8, 4);
        prenda.getComposicion().clear();
        prenda.getComposicion().add(new ComponenteComposicion(MaterialTextil.POLIESTER, 40));
        prenda.getComposicion().add(new ComponenteComposicion(MaterialTextil.ALGODON, 60));
        prepararCarrito(prenda);
        when(perfilInfantilService.obtener(11L))
                .thenReturn(perfil(11L, Set.of(AlergiaTextil.FIBRAS_SINTETICAS), false));

        assertThatThrownBy(() -> servicio.agregar(solicitud(11L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        verify(carritoRepository, never()).saveAndFlush(any(Carrito.class));
    }

    @Test
    void sinPerfilEnLaSolicitudValidaTodosLosPerfilesDelCliente() {
        Prenda prenda = CatalogoFixtures.prenda(7L, 2, 8, 4);
        prenda.setContieneNiquel(true);
        prepararCarrito(prenda);
        when(perfilInfantilService.listar()).thenReturn(List.of(
                perfil(11L, Set.of(), true),
                perfil(12L, Set.of(AlergiaTextil.NIQUEL), false)));

        assertThatThrownBy(() -> servicio.agregar(solicitud(null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        verify(carritoRepository, never()).saveAndFlush(any(Carrito.class));
    }

    @Test
    void noBloqueaAgregarCuandoLaCoincidenciaDeAlergiaSoloAdvierte() {
        Prenda prenda = CatalogoFixtures.prenda(7L, 2, 8, 4);
        prepararCarrito(prenda);
        when(perfilInfantilService.obtener(11L))
                .thenReturn(perfil(11L, Set.of(AlergiaTextil.ALGODON), false));
        when(prendaRepository.findAllById(any())).thenReturn(List.of(prenda));
        when(carritoRepository.saveAndFlush(any(Carrito.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = servicio.agregar(solicitud(11L));

        assertThat(respuesta.cantidad()).isEqualTo(1);
        assertThat(respuesta.subtotal()).isEqualByComparingTo("32900");
    }

    @Test
    void permiteAgregarCuandoElPerfilNoDeclaroAlergias() {
        Prenda prenda = CatalogoFixtures.prenda(7L, 2, 8, 4);
        prepararCarrito(prenda);
        when(perfilInfantilService.obtener(11L)).thenReturn(perfil(11L, Set.of(), true));
        when(prendaRepository.findAllById(any())).thenReturn(List.of(prenda));
        when(carritoRepository.saveAndFlush(any(Carrito.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = servicio.agregar(solicitud(11L));

        assertThat(respuesta.cantidad()).isEqualTo(1);
    }

    @Test
    void rechazaUnPerfilInfantilInvalido() {
        assertThatThrownBy(() -> servicio.agregar(new AgregarItemCarritoRequest(7L, "4", 1, null, -5L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private AgregarItemCarritoRequest solicitud(Long perfilInfantilId) {
        return new AgregarItemCarritoRequest(7L, "4", 1, null, perfilInfantilId);
    }

    private void prepararCarrito(Prenda prenda) {
        when(carritoRepository.bloquearPorClienteId(CLIENTE_ID)).thenReturn(Optional.of(carrito));
        when(prendaRepository.bloquearPorIdYEstado(7L, EstadoPrenda.ACTIVA)).thenReturn(Optional.of(prenda));
    }

    private PerfilResponse perfil(Long id, Set<AlergiaTextil> alergias, boolean sinAlergias) {
        return new PerfilResponse(id, "Sofía", LocalDate.of(2021, 1, 1), 5, 0,
                Contextura.MEDIA, Holgura.REGULAR, alergias, null, sinAlergias,
                Set.of(), Set.of(), null, null, LocalDateTime.now(), LocalDateTime.now(), null, 100, List.of());
    }
}
