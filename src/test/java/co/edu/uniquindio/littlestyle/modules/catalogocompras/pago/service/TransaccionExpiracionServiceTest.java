package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import co.edu.uniquindio.littlestyle.config.PagoProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.EstadoTransaccionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.TransaccionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.repository.TransaccionPagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Suite del servicio que expira transacciones de pago sin resolver.
 * Se mockea el repositorio; el reloj es fijo para que los tiempos
 * de vencimiento sean deterministas. Incluye la carrera con el webhook.
 */
@ExtendWith(MockitoExtension.class)
class TransaccionExpiracionServiceTest {

    private static final Long TRANSACCION_ID = 7L;
    private static final String REFERENCIA = "LS-C42-VENCIDA001";
    // Reloj congelado: la hora "actual" del test siempre es la misma
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-13T15:00:00Z"), ZoneOffset.UTC);

    @Mock
    private TransaccionPagoRepository transaccionPagoRepository;

    private TransaccionExpiracionService servicio;

    // Arma el servicio con propiedades por defecto y el reloj fijo
    @BeforeEach
    void setUp() {
        PagoProperties properties = PagoProperties.porDefecto();
        servicio = new TransaccionExpiracionService(transaccionPagoRepository, properties, CLOCK);
    }

    // Una compra quedó en CREADA y Wompi nunca confirmó nada: al vencer el
    // plazo se anula y se actualiza la fecha con el reloj fijo
    @Test
    void expiraUnaCompraACreadaQueWompiNuncaResolvio() {
        TransaccionPago transaccion = transaccion(EstadoTransaccionPago.CREADA,
                LocalDateTime.now(CLOCK).minus(Duration.ofMinutes(45)));
        when(transaccionPagoRepository.buscarIdsVencidos(anyList(), any()))
                .thenReturn(List.of(TRANSACCION_ID));
        when(transaccionPagoRepository.bloquearPorId(TRANSACCION_ID)).thenReturn(Optional.of(transaccion));

        int expiradas = servicio.expirarTransaccionesVencidas();

        assertThat(expiradas).isEqualTo(1);
        assertThat(transaccion.getEstado()).isEqualTo(EstadoTransaccionPago.ANULADA);
        assertThat(transaccion.getFechaActualizacion()).isEqualTo(LocalDateTime.now(CLOCK));
    }

    // PENDIENTE también cuenta como sin resolver: si quedó ahí por un estado
    // no mapeado, no debe quedarse pegada para siempre
    @Test
    void expiraTambienUnPagoQueQuedoPendientePorUnEstadoNoMapeado() {
        TransaccionPago transaccion = transaccion(EstadoTransaccionPago.PENDIENTE,
                LocalDateTime.now(CLOCK).minus(Duration.ofMinutes(45)));
        when(transaccionPagoRepository.buscarIdsVencidos(anyList(), any()))
                .thenReturn(List.of(TRANSACCION_ID));
        when(transaccionPagoRepository.bloquearPorId(TRANSACCION_ID)).thenReturn(Optional.of(transaccion));

        int expiradas = servicio.expirarTransaccionesVencidas();

        assertThat(expiradas).isEqualTo(1);
        assertThat(transaccion.getEstado()).isEqualTo(EstadoTransaccionPago.ANULADA);
    }

    // Si no hay nada vencido, no se toca ninguna transacción
    @Test
    void noExpiraUnaCompraQueElClienteTodaviaEstaCompletando() {
        when(transaccionPagoRepository.buscarIdsVencidos(anyList(), any())).thenReturn(List.of());

        int expiradas = servicio.expirarTransaccionesVencidas();

        assertThat(expiradas).isZero();
        verify(transaccionPagoRepository, never()).bloquearPorId(any());
    }

    // El estado cambió después de la consulta, la expiración respeta el
    // que tiene al momento de bloquear
    @Test
    void noExpiraUnPagoQueElWebhookResolvioMientrasBuscaba() {
        // Carrera real: la búsqueda lo listó como CREADA, pero entre esa consulta y el bloqueo
        // llegó el evento de Wompi y lo dejó en APROBADA. Expirarlo perdería la venta.
        TransaccionPago yaAprobada = transaccion(EstadoTransaccionPago.APROBADA,
                LocalDateTime.now(CLOCK).minus(Duration.ofMinutes(45)));
        when(transaccionPagoRepository.buscarIdsVencidos(anyList(), any()))
                .thenReturn(List.of(TRANSACCION_ID));
        when(transaccionPagoRepository.bloquearPorId(TRANSACCION_ID)).thenReturn(Optional.of(yaAprobada));

        int expiradas = servicio.expirarTransaccionesVencidas();

        assertThat(expiradas).isZero();
        assertThat(yaAprobada.getEstado()).isEqualTo(EstadoTransaccionPago.APROBADA);
    }

    // Si al bloquear la transacción ya no existe, se ignora sin fallar
    @Test
    void noExpiraUnaTransaccionQueYaNoExiste() {
        when(transaccionPagoRepository.buscarIdsVencidos(anyList(), any()))
                .thenReturn(List.of(TRANSACCION_ID));
        when(transaccionPagoRepository.bloquearPorId(TRANSACCION_ID)).thenReturn(Optional.empty());

        assertThat(servicio.expirarTransaccionesVencidas()).isZero();
    }

    // Builder de transacción con datos válidos, solo cambia estado y fecha
    private TransaccionPago transaccion(EstadoTransaccionPago estado, LocalDateTime fechaCreacion) {
        return TransaccionPago.builder()
                .id(TRANSACCION_ID)
                .referencia(REFERENCIA)
                .montoCentavos(7280000L)
                .moneda("COP")
                .estado(estado)
                .fechaCreacion(fechaCreacion)
                .fechaActualizacion(fechaCreacion)
                .build();
    }
}