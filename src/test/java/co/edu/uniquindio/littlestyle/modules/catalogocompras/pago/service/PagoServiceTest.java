package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import co.edu.uniquindio.littlestyle.config.CheckoutProperties;
import co.edu.uniquindio.littlestyle.config.PagoProperties;
import co.edu.uniquindio.littlestyle.config.WompiProperties;
import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.LineaDisponibilidadPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoCheckoutService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoPagoService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.ResultadoConfirmacionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.EventoWompiRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.InicioPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.ResultadoPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.TransaccionWompi;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.EstadoTransaccionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.MotivoReembolso;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.TransaccionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.repository.TransaccionPagoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service.ProcesadorCheckoutPagadoService;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    private static final Long CLIENTE_ID = 42L;
    private static final Long PEDIDO_ID = 55L;
    private static final String REFERENCIA = "LS-55-ABCDEF0123";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-13T15:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private TransaccionPagoRepository transaccionPagoRepository;
    @Mock
    private PedidoPagoService pedidoPagoService;
    @Mock
    private PedidoCheckoutService pedidoCheckoutService;
    @Mock
    private ProcesadorCheckoutPagadoService procesadorCheckoutPagadoService;
    @Mock
    private FirmaWompiService firmaWompiService;
    @Mock
    private WompiClient wompiClient;

    private PagoService pagoService;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        pagoService = servicio(propiedadesWompi("pub_test_llave", "integridad"));
        pedido = pedido(CLIENTE_ID, EstadoPedido.PENDIENTE_PAGO, LocalDateTime.now(CLOCK).minusMinutes(5));
        autenticar(CLIENTE_ID);
    }

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void iniciaPagoConMontoEnCentavosYFirmaDeIntegridad() {
        when(pedidoRepository.bloquearPorId(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(firmaWompiService.firmaIntegridad(anyString(), eq(7280000L), eq("COP"))).thenReturn("firma-sha256");

        InicioPagoResponse respuesta = pagoService.iniciar(PEDIDO_ID);

        ArgumentCaptor<TransaccionPago> guardada = ArgumentCaptor.forClass(TransaccionPago.class);
        verify(transaccionPagoRepository).save(guardada.capture());
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoTransaccionPago.CREADA);
        assertThat(guardada.getValue().getMontoCentavos()).isEqualTo(7280000L);
        assertThat(guardada.getValue().getReferencia()).matches("LS-55-[0-9A-F]{10}");
        assertThat(respuesta.referencia()).isEqualTo(guardada.getValue().getReferencia());
        assertThat(respuesta.montoCentavos()).isEqualTo(7280000L);
        assertThat(respuesta.moneda()).isEqualTo("COP");
        assertThat(respuesta.llavePublica()).isEqualTo("pub_test_llave");
        assertThat(respuesta.firmaIntegridad()).isEqualTo("firma-sha256");
        assertThat(respuesta.emailCliente()).isEqualTo("cliente@test.com");
        assertThat(respuesta.telefonoCliente()).isEqualTo("3001234567");
    }

    @Test
    void cadaIntentoDePagoUsaUnaReferenciaDistinta() {
        when(pedidoRepository.bloquearPorId(PEDIDO_ID)).thenReturn(Optional.of(pedido));

        String primera = pagoService.iniciar(PEDIDO_ID).referencia();
        String segunda = pagoService.iniciar(PEDIDO_ID).referencia();

        assertThat(primera).isNotEqualTo(segunda);
    }

    @Test
    void noIniciaPagoDePedidoDeOtroCliente() {
        Pedido ajeno = pedido(99L, EstadoPedido.PENDIENTE_PAGO, LocalDateTime.now(CLOCK));
        when(pedidoRepository.bloquearPorId(PEDIDO_ID)).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> pagoService.iniciar(PEDIDO_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(transaccionPagoRepository, never()).save(any());
    }

    @Test
    void noIniciaPagoDePedidoQueNoEstaPendiente() {
        Pedido pagado = pedido(CLIENTE_ID, EstadoPedido.PAGADO, LocalDateTime.now(CLOCK));
        when(pedidoRepository.bloquearPorId(PEDIDO_ID)).thenReturn(Optional.of(pagado));

        assertThatThrownBy(() -> pagoService.iniciar(PEDIDO_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void noIniciaPagoDePedidoVencido() {
        Pedido vencido = pedido(CLIENTE_ID, EstadoPedido.PENDIENTE_PAGO, LocalDateTime.now(CLOCK).minusMinutes(31));
        when(pedidoRepository.bloquearPorId(PEDIDO_ID)).thenReturn(Optional.of(vencido));

        assertThatThrownBy(() -> pagoService.iniciar(PEDIDO_ID))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getMessage()).contains("venció");
                });
    }

    @Test
    void noIniciaOtroPagoMientrasHayaUnoPendiente() {
        when(pedidoRepository.bloquearPorId(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(transaccionPagoRepository.existsByPedidoIdAndEstado(PEDIDO_ID, EstadoTransaccionPago.PENDIENTE))
                .thenReturn(true);

        assertThatThrownBy(() -> pagoService.iniciar(PEDIDO_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(transaccionPagoRepository, never()).save(any());
    }

    @Test
    void limitaLosIntentosDePagoPorPedido() {
        when(pedidoRepository.bloquearPorId(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(transaccionPagoRepository.countByPedidoId(PEDIDO_ID)).thenReturn(4L, 5L);

        pagoService.iniciar(PEDIDO_ID);
        assertThatThrownBy(() -> pagoService.iniciar(PEDIDO_ID))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.getMessage()).contains("máximo de 5 intentos");
                });
        verify(transaccionPagoRepository).save(any());
    }

    @Test
    void siWompiNoRespondeATiempoNoTocaElPagoNiElPedido() {
        when(wompiClient.consultarTransaccion("wompi-tx-1")).thenThrow(new BusinessException(
                HttpStatus.GATEWAY_TIMEOUT, "Wompi está tardando en responder. Intenta más tarde."));

        assertThatThrownBy(() -> pagoService.verificar("wompi-tx-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
        verifyNoInteractions(transaccionPagoRepository, pedidoRepository, pedidoPagoService);
    }

    @Test
    void validarEventoNoTocaLaBaseDeDatos() {
        EventoWompiRequest evento = evento("APPROVED", 7280000L);
        when(firmaWompiService.eventoValido(evento)).thenReturn(true);

        pagoService.validarEvento(evento);

        verifyNoInteractions(transaccionPagoRepository, pedidoRepository, pedidoPagoService);
    }

    @Test
    void respondeServicioNoDisponibleSiWompiNoEstaConfigurado() {
        PagoService sinLlaves = servicio(propiedadesWompi("", ""));

        assertThatThrownBy(() -> sinLlaves.iniciar(PEDIDO_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaEventoConFirmaInvalidaSinTocarPedidos() {
        EventoWompiRequest evento = evento("APPROVED", 7280000L);
        when(firmaWompiService.eventoValido(evento)).thenReturn(false);

        assertThatThrownBy(() -> pagoService.procesarEvento(evento))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verifyNoInteractions(transaccionPagoRepository, pedidoRepository, pedidoPagoService);
    }

    @Test
    void eventoAprobadoConfirmaElPedidoYDescuentaStock() {
        TransaccionPago transaccion = transaccion(EstadoTransaccionPago.CREADA);
        EventoWompiRequest evento = evento("APPROVED", 7280000L);
        when(firmaWompiService.eventoValido(evento)).thenReturn(true);
        when(transaccionPagoRepository.bloquearPorReferencia(REFERENCIA)).thenReturn(Optional.of(transaccion));
        when(pedidoRepository.bloquearPorId(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(pedidoPagoService.confirmarPago(PEDIDO_ID)).thenReturn(
                new ResultadoConfirmacionPago(ResultadoConfirmacionPago.Estado.CONFIRMADO, List.of()));

        pagoService.procesarEvento(evento);

        verify(pedidoPagoService).confirmarPago(PEDIDO_ID);
        assertThat(transaccion.getEstado()).isEqualTo(EstadoTransaccionPago.APROBADA);
        assertThat(transaccion.getWompiTransaccionId()).isEqualTo("wompi-tx-1");
        assertThat(transaccion.getMetodoPago()).isEqualTo("CARD");
    }

    @Test
    void eventoAprobadoSinStockDejaReembolsoPendienteConPrendasAgotadas() {
        TransaccionPago transaccion = transaccion(EstadoTransaccionPago.CREADA);
        EventoWompiRequest evento = evento("APPROVED", 7280000L);
        when(firmaWompiService.eventoValido(evento)).thenReturn(true);
        when(transaccionPagoRepository.bloquearPorReferencia(REFERENCIA)).thenReturn(Optional.of(transaccion));
        when(pedidoRepository.bloquearPorId(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(pedidoPagoService.confirmarPago(PEDIDO_ID)).thenReturn(new ResultadoConfirmacionPago(
                ResultadoConfirmacionPago.Estado.STOCK_INSUFICIENTE,
                List.of(new LineaDisponibilidadPagoResponse(20L, "Pantalón Jogger", "4", 2, 1))));

        pagoService.procesarEvento(evento);

        assertThat(transaccion.getEstado()).isEqualTo(EstadoTransaccionPago.REEMBOLSO_PENDIENTE);
        assertThat(transaccion.getMotivoReembolso()).isEqualTo(MotivoReembolso.STOCK_INSUFICIENTE);
        assertThat(transaccion.getPrendasAgotadas()).singleElement().satisfies(prenda -> {
            assertThat(prenda.getNombre()).isEqualTo("Pantalón Jogger");
            assertThat(prenda.getCantidadSolicitada()).isEqualTo(2);
            assertThat(prenda.getStockDisponible()).isEqualTo(1);
        });
    }

    @Test
    void eventoAprobadoParaPedidoYaCanceladoNoLlamaALaConfirmacion() {
        TransaccionPago transaccion = transaccion(EstadoTransaccionPago.PENDIENTE);
        Pedido cancelado = pedido(CLIENTE_ID, EstadoPedido.CANCELADO, LocalDateTime.now(CLOCK));
        transaccion.setPedido(cancelado);
        EventoWompiRequest evento = evento("APPROVED", 7280000L);
        when(firmaWompiService.eventoValido(evento)).thenReturn(true);
        when(transaccionPagoRepository.bloquearPorReferencia(REFERENCIA)).thenReturn(Optional.of(transaccion));
        when(pedidoRepository.bloquearPorId(PEDIDO_ID)).thenReturn(Optional.of(cancelado));

        pagoService.procesarEvento(evento);

        verify(pedidoPagoService, never()).confirmarPago(anyLong());
        assertThat(transaccion.getEstado()).isEqualTo(EstadoTransaccionPago.REEMBOLSO_PENDIENTE);
        assertThat(transaccion.getMotivoReembolso()).isEqualTo(MotivoReembolso.PEDIDO_NO_PAGABLE);
    }

    @Test
    void eventoAprobadoParaPedidoVencidoDejaReembolsoPendiente() {
        TransaccionPago transaccion = transaccion(EstadoTransaccionPago.CREADA);
        EventoWompiRequest evento = evento("APPROVED", 7280000L);
        when(firmaWompiService.eventoValido(evento)).thenReturn(true);
        when(transaccionPagoRepository.bloquearPorReferencia(REFERENCIA)).thenReturn(Optional.of(transaccion));
        when(pedidoRepository.bloquearPorId(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(pedidoPagoService.confirmarPago(PEDIDO_ID)).thenReturn(
                new ResultadoConfirmacionPago(ResultadoConfirmacionPago.Estado.PEDIDO_VENCIDO, List.of()));

        pagoService.procesarEvento(evento);

        assertThat(transaccion.getEstado()).isEqualTo(EstadoTransaccionPago.REEMBOLSO_PENDIENTE);
        assertThat(transaccion.getMotivoReembolso()).isEqualTo(MotivoReembolso.PEDIDO_VENCIDO);
    }

    @Test
    void eventoRechazadoOPendienteNoConfirmaElPedido() {
        TransaccionPago transaccion = transaccion(EstadoTransaccionPago.CREADA);
        EventoWompiRequest pendiente = evento("PENDING", 7280000L);
        EventoWompiRequest rechazado = evento("DECLINED", 7280000L);
        when(firmaWompiService.eventoValido(any())).thenReturn(true);
        when(transaccionPagoRepository.bloquearPorReferencia(REFERENCIA)).thenReturn(Optional.of(transaccion));

        pagoService.procesarEvento(pendiente);
        assertThat(transaccion.getEstado()).isEqualTo(EstadoTransaccionPago.PENDIENTE);
        pagoService.procesarEvento(rechazado);
        assertThat(transaccion.getEstado()).isEqualTo(EstadoTransaccionPago.RECHAZADA);

        verify(pedidoPagoService, never()).confirmarPago(anyLong());
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE_PAGO);
    }

    @Test
    void eventoConMontoDistintoNoConfirmaElPedido() {
        TransaccionPago transaccion = transaccion(EstadoTransaccionPago.CREADA);
        EventoWompiRequest evento = evento("APPROVED", 100L);
        when(firmaWompiService.eventoValido(evento)).thenReturn(true);
        when(transaccionPagoRepository.bloquearPorReferencia(REFERENCIA)).thenReturn(Optional.of(transaccion));

        pagoService.procesarEvento(evento);

        verify(pedidoPagoService, never()).confirmarPago(anyLong());
        assertThat(transaccion.getEstado()).isEqualTo(EstadoTransaccionPago.ERROR);
    }

    @Test
    void eventosRepetidosNoConfirmanDosVecesElMismoPago() {
        TransaccionPago transaccion = transaccion(EstadoTransaccionPago.APROBADA);
        when(firmaWompiService.eventoValido(any())).thenReturn(true);
        when(transaccionPagoRepository.bloquearPorReferencia(REFERENCIA)).thenReturn(Optional.of(transaccion));

        pagoService.procesarEvento(evento("APPROVED", 7280000L));
        pagoService.procesarEvento(evento("DECLINED", 7280000L));

        verify(pedidoPagoService, never()).confirmarPago(anyLong());
        assertThat(transaccion.getEstado()).isEqualTo(EstadoTransaccionPago.APROBADA);
    }

    @Test
    void ignoraEventosQueNoSonDeTransaccionesYReferenciasDesconocidas() {
        EventoWompiRequest otroEvento = new EventoWompiRequest("nequi_token.updated", Map.of(), "test",
                new EventoWompiRequest.FirmaEvento(List.of("x"), "y"), 1L);
        EventoWompiRequest desconocido = evento("APPROVED", 7280000L);
        when(firmaWompiService.eventoValido(any())).thenReturn(true);
        when(transaccionPagoRepository.bloquearPorReferencia(REFERENCIA)).thenReturn(Optional.empty());

        pagoService.procesarEvento(otroEvento);
        pagoService.procesarEvento(desconocido);

        verifyNoInteractions(pedidoPagoService);
    }

    @Test
    void verificaConLaApiDeWompiYDevuelveLasLineasQueSiTienenStock() {
        TransaccionPago transaccion = transaccion(EstadoTransaccionPago.CREADA);
        when(wompiClient.consultarTransaccion("wompi-tx-1")).thenReturn(
                new TransaccionWompi("wompi-tx-1", "APPROVED", REFERENCIA, 7280000L, "COP", "PSE"));
        when(transaccionPagoRepository.bloquearPorReferencia(REFERENCIA)).thenReturn(Optional.of(transaccion));
        when(pedidoRepository.bloquearPorId(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(pedidoPagoService.confirmarPago(PEDIDO_ID)).thenReturn(new ResultadoConfirmacionPago(
                ResultadoConfirmacionPago.Estado.STOCK_INSUFICIENTE,
                List.of(new LineaDisponibilidadPagoResponse(20L, "Pantalón Jogger", "4", 2, 0))));

        ResultadoPagoResponse respuesta = pagoService.verificar("wompi-tx-1");

        assertThat(respuesta.pedidoId()).isEqualTo(PEDIDO_ID);
        assertThat(respuesta.estadoPago()).isEqualTo(EstadoTransaccionPago.REEMBOLSO_PENDIENTE);
        assertThat(respuesta.metodoPago()).isEqualTo("PSE");
        assertThat(respuesta.prendasAgotadas()).singleElement()
                .satisfies(linea -> assertThat(linea.prendaId()).isEqualTo(20L));
        assertThat(respuesta.lineasDisponibles()).singleElement().satisfies(linea -> {
            assertThat(linea.prendaId()).isEqualTo(21L);
            assertThat(linea.talla()).isEqualTo("6");
            assertThat(linea.cantidad()).isEqualTo(1);
        });
    }

    @Test
    void noVerificaPagosDeOtroCliente() {
        TransaccionPago transaccion = transaccion(EstadoTransaccionPago.CREADA);
        transaccion.setPedido(pedido(99L, EstadoPedido.PENDIENTE_PAGO, LocalDateTime.now(CLOCK)));
        when(wompiClient.consultarTransaccion("wompi-tx-1")).thenReturn(
                new TransaccionWompi("wompi-tx-1", "APPROVED", REFERENCIA, 7280000L, "COP", "CARD"));
        when(transaccionPagoRepository.bloquearPorReferencia(REFERENCIA)).thenReturn(Optional.of(transaccion));

        assertThatThrownBy(() -> pagoService.verificar("wompi-tx-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(pedidoPagoService);
    }

    @Test
    void convierteTotalesAPesosEnCentavos() {
        assertThat(PagoService.centavos(new BigDecimal("72800"))).isEqualTo(7280000L);
        assertThat(PagoService.centavos(new BigDecimal("72800.50"))).isEqualTo(7280050L);
    }

    private PagoService servicio(WompiProperties propiedades) {
        CheckoutProperties checkout = new CheckoutProperties(new BigDecimal("7000"), new BigDecimal("10000"),
                new BigDecimal("15000"), List.of(), Duration.ofMinutes(30));
        return new PagoService(pedidoRepository, transaccionPagoRepository, pedidoPagoService,
                pedidoCheckoutService, procesadorCheckoutPagadoService, new ObjectMapper(),
                firmaWompiService, wompiClient, propiedades, checkout, PagoProperties.porDefecto(),
                new TransactionTemplate(mock(PlatformTransactionManager.class)), CLOCK);
    }

    private static WompiProperties propiedadesWompi(String llavePublica, String secretoIntegridad) {
        return new WompiProperties("https://sandbox.wompi.co/v1", llavePublica, secretoIntegridad,
                "eventos", "http://localhost:4200/cliente/pago/resultado", "COP");
    }

    private static void autenticar(Long usuarioId) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(usuarioId, "cliente@test.com", "Cliente", Rol.CLIENTE), null, List.of()));
    }

    private static Pedido pedido(Long clienteId, EstadoPedido estado, LocalDateTime fechaCreacion) {
        Pedido pedido = Pedido.builder()
                .id(PEDIDO_ID)
                .cliente(Usuario.builder().id(clienteId).build())
                .perfilInfantilId(3L)
                .estado(estado)
                .destinatario("Ana Ruiz")
                .direccion("Calle 1 # 2-3")
                .ciudad("Armenia")
                .telefono("3001234567")
                .subtotal(new BigDecimal("65800"))
                .costoEnvio(new BigDecimal("7000"))
                .total(new BigDecimal("72800"))
                .fechaCreacion(fechaCreacion)
                .build();
        pedido.agregar(LineaPedido.builder().prendaId(20L).nombre("Pantalón Jogger").talla("4").cantidad(2)
                .precioUnitario(new BigDecimal("32900")).subtotal(new BigDecimal("65800")).build());
        pedido.agregar(LineaPedido.builder().prendaId(21L).nombre("Camiseta").talla("6").cantidad(1)
                .precioUnitario(new BigDecimal("0")).subtotal(new BigDecimal("0")).build());
        return pedido;
    }

    private TransaccionPago transaccion(EstadoTransaccionPago estado) {
        return TransaccionPago.builder()
                .id(1L)
                .pedido(pedido)
                .referencia(REFERENCIA)
                .montoCentavos(7280000L)
                .moneda("COP")
                .estado(estado)
                .fechaCreacion(LocalDateTime.now(CLOCK))
                .fechaActualizacion(LocalDateTime.now(CLOCK))
                .build();
    }

    private static EventoWompiRequest evento(String estado, long montoCentavos) {
        Map<String, Object> transaccion = new HashMap<>();
        transaccion.put("id", "wompi-tx-1");
        transaccion.put("status", estado);
        transaccion.put("reference", REFERENCIA);
        transaccion.put("amount_in_cents", montoCentavos);
        transaccion.put("currency", "COP");
        transaccion.put("payment_method_type", "CARD");
        return new EventoWompiRequest("transaction.updated", Map.of("transaction", transaccion), "test",
                new EventoWompiRequest.FirmaEvento(List.of("transaction.id"), "checksum"), 1530291411L);
    }
}
