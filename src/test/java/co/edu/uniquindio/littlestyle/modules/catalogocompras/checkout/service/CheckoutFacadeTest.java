package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service.DisponibilidadStockService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ItemResumenCheckoutRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DireccionEnvioRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.ResumenCheckoutRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.EvaluacionAlergia;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.FiltroAlergia;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.NivelRiesgo;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CheckoutFacadeTest {

    private static final Long CLIENTE_ID = 42L;

    @Mock
    private PrendaRepository prendaRepository;
    @Mock
    private PerfilInfantilService perfilInfantilService;
    @Mock
    private FiltroAlergia filtroAlergia;
    @Mock
    private TarifaEnvioService tarifaEnvioService;

    private CheckoutFacade facade;
    private Prenda prenda;

    @BeforeEach
    void setUp() {
        lenient().when(tarifaEnvioService.calcular("Quindío", "Armenia"))
                .thenReturn(new BigDecimal("7000"));
        facade = new CheckoutFacade(prendaRepository, perfilInfantilService, filtroAlergia,
                new DisponibilidadStockService(), tarifaEnvioService);
        prenda = CatalogoFixtures.prenda(7L, 2, 8, 4);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        new AuthenticatedUser(CLIENTE_ID, "cliente@test.com", "Cliente", Rol.CLIENTE),
                        null, List.of()));
        when(perfilInfantilService.obtener(11L)).thenReturn(perfil(11L, Set.of(), true));
        when(prendaRepository.findByIdAndEstado(7L, EstadoPrenda.ACTIVA)).thenReturn(Optional.of(prenda));
        when(filtroAlergia.evaluar(any(), any())).thenReturn(EvaluacionAlergia.apta());
    }

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void calculaSubtotalesYTotalConTarifaSegunDireccion() {
        var respuesta = facade.calcularResumen(solicitud(), items(
                new ItemResumenCheckoutRequest(7L, "4", 2),
                new ItemResumenCheckoutRequest(7L, "6", 1)));

        assertThat(respuesta.subtotal()).isEqualByComparingTo("98700");
        assertThat(respuesta.costoEnvio()).isEqualByComparingTo("7000");
        assertThat(respuesta.total()).isEqualByComparingTo("105700");
        assertThat(respuesta.items()).extracting("subtotal")
                .containsExactly(new BigDecimal("65800"), new BigDecimal("32900"));
    }

    @Test
    void sumaLineasRepetidasAntesDeValidarStock() {
        assertThatThrownBy(() -> facade.calcularResumen(solicitud(), items(
                new ItemResumenCheckoutRequest(7L, "4", 5),
                new ItemResumenCheckoutRequest(7L, "4", 4))))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void rechazaPrendaIncompatibleConAlergiaExcluyente() {
        when(perfilInfantilService.obtener(11L))
                .thenReturn(perfil(11L, Set.of(AlergiaTextil.NIQUEL), false));
        when(filtroAlergia.evaluar(eq(Set.of(AlergiaTextil.NIQUEL)), any()))
                .thenReturn(new EvaluacionAlergia(NivelRiesgo.EXCLUIDA, List.of("Contiene níquel")));

        assertThatThrownBy(() -> facade.calcularResumen(
                solicitud(), items(new ItemResumenCheckoutRequest(7L, "4", 1))))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    private ResumenCheckoutRequest solicitud() {
        return new ResumenCheckoutRequest(11L,
                new DireccionEnvioRequest("Ana Ruiz", "Calle 1 # 2-3", null,
                        "630001", "Quindío", "Armenia", "3001234567"));
    }

    private List<ItemResumenCheckoutRequest> items(ItemResumenCheckoutRequest... items) {
        return List.of(items);
    }

    private PerfilResponse perfil(Long id, Set<AlergiaTextil> alergias, boolean sinAlergias) {
        return new PerfilResponse(id, "Sofía", LocalDate.of(2021, 1, 1), 5, 0,
                Contextura.MEDIA, Holgura.REGULAR, alergias, null, sinAlergias,
                Set.of(), Set.of(), null, null, LocalDateTime.now(), LocalDateTime.now(),
                null, 100, List.of());
    }
}
