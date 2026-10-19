package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import co.edu.uniquindio.littlestyle.config.WompiProperties;
import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.CrearPedidoRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.event.PedidoPagoRechazadoEvent;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.LineaDisponibilidadPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.LineaResumenCheckoutResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.PedidoPagoSnapshot;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoCheckoutService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoPagoService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.ResultadoConfirmacionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.EventoWompiRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.InicioPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.LineaRecompraResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.ResultadoPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.TransaccionWompi;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.EstadoTransaccionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.MotivoReembolso;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.PrendaAgotadaPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.TransaccionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.repository.TransaccionPagoRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Coordina el pago de pedidos con Wompi (US-11): genera los datos firmados para el Widget,
 * procesa los eventos verificados del webhook y la consulta directa a la API, permite al
 * cliente anular un pago en CREADA, y conecta la aprobación con {@link PedidoPagoService},
 * que descuenta el stock de forma atómica (US-10).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoService {

    static final String EVENTO_TRANSACCION = "transaction.updated";

    private final PedidoRepository pedidoRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;
    private final PedidoPagoService pedidoPagoService;
    private final PedidoCheckoutService pedidoCheckoutService;
    private final ProcesadorCheckoutPagadoService procesadorCheckoutPagadoService;
    private final ObjectMapper objectMapper;
    private final FirmaWompiService firmaWompiService;
    private final WompiClient wompiClient;
    private final WompiProperties wompiProperties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final ApplicationEventPublisher eventos;
    private final SecureRandom random = new SecureRandom();

    /**
     * Inicia el pago de la compra actual. Reutiliza el intento con la misma clave de idempotencia o el que
     * sigue en curso para la misma compra, rechaza con 409 si hay otro pago en curso para una compra distinta
     * y, si no, guarda una transacción {@code CREADA} con el snapshot del checkout.
     */
    @Transactional
    public InicioPagoResponse iniciar(CrearPedidoRequest request, String claveIdempotencia) {
        if (!wompiProperties.pagosConfigurados()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La pasarela de pagos no está disponible en este momento");
        }
        AuthenticatedUser usuario = usuarioActual();
        String clave = normalizarClave(claveIdempotencia);
        if (clave != null) {
            Optional<TransaccionPago> existente = transaccionPagoRepository
                    .findByClienteIdAndClaveIdempotencia(usuario.id(), clave);
            if (existente.isPresent()) {
                return respuestaInicio(existente.get(), usuario);
            }
        }

        PedidoPagoSnapshot snapshot = pedidoCheckoutService.prepararPago(request);
        Optional<TransaccionPago> activa = transaccionPagoRepository
                .findFirstByClienteIdAndEstadoInOrderByFechaCreacionDesc(usuario.id(),
                        List.of(EstadoTransaccionPago.CREADA, EstadoTransaccionPago.PENDIENTE));
        if (activa.isPresent()) {
            PedidoPagoSnapshot snapshotActivo = deserializar(activa.get().getSnapshotCheckout());
            if (!snapshot.equals(snapshotActivo)) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "Ya hay un pago en proceso para otra compra. Espera su confirmación antes de iniciar otro.");
            }
            return respuestaInicio(activa.get(), usuario);
        }
        long montoCentavos = centavos(snapshot.total());
        String referencia = generarReferenciaCheckout(usuario.id());
        LocalDateTime ahora = LocalDateTime.now(clock);
        TransaccionPago transaccion = TransaccionPago.builder()
                .cliente(Usuario.builder().id(usuario.id()).build())
                .snapshotCheckout(serializar(snapshot))
                .claveIdempotencia(clave)
                .referencia(referencia)
                .montoCentavos(montoCentavos)
                .moneda(wompiProperties.moneda())
                .estado(EstadoTransaccionPago.CREADA)
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .build();
        transaccionPagoRepository.save(transaccion);
        return respuestaInicio(transaccion, usuario);
    }

    /**
     * Anula un pago del cliente que aún está en CREADA: suelta la compra y deja al cliente
     * iniciar otra sin esperar confirmación de Wompi (idempotente si ya está ANULADA).
     * Si Wompi más tarde aprueba un cobro de esta referencia, el evento pasa a
     * REEMBOLSO_PENDIENTE en {@link #aplicar} para devolver el dinero.
     */
    @Transactional
    public ResultadoPagoResponse cancelar(String referencia) {
        AuthenticatedUser usuario = usuarioActual();
        return transaccionPagoRepository.bloquearPorReferencia(referencia)
                .filter(encontrada -> perteneceA(encontrada, usuario))
                .map(transaccion -> {
                    if (transaccion.getEstado() == EstadoTransaccionPago.CREADA) {
                        transaccion.setEstado(EstadoTransaccionPago.ANULADA);
                        transaccion.setFechaActualizacion(LocalDateTime.now(clock));
                    } else if (transaccion.getEstado() != EstadoTransaccionPago.ANULADA) {
                        throw new BusinessException(HttpStatus.CONFLICT,
                                "Este pago ya no se puede cancelar");
                    }
                    return respuesta(transaccion);
                })
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
    }

    /**
     * Consulta la transacción directamente en Wompi y aplica su estado si pertenece al cliente.
     * La consulta HTTP se hace fuera de la transacción de base de datos: si Wompi tarda, no se
     * retienen conexiones ni bloqueos que hagan esperar a otros pagos (RNF-05, RNF-28).
     */
    public ResultadoPagoResponse verificar(String wompiTransaccionId) {
        AuthenticatedUser usuario = usuarioActual();
        TransaccionWompi transaccionWompi = wompiClient.consultarTransaccion(wompiTransaccionId);
        return transactionTemplate.execute(estado -> {
            TransaccionPago transaccion = Optional.ofNullable(transaccionWompi.referencia())
                    .flatMap(transaccionPagoRepository::bloquearPorReferencia)
                    .filter(encontrada -> perteneceA(encontrada, usuario))
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
            aplicar(transaccion, transaccionWompi);
            return respuesta(transaccion);
        });
    }

    /** Valida y aplica un evento del webhook en la misma llamada. Lanza 401 si no es válido. */
    @Transactional
    public void procesarEvento(EventoWompiRequest evento) {
        validarEvento(evento);
        aplicarEvento(evento);
    }

    /**
     * Rechaza con 401 el evento cuya firma SHA-256 no coincide o cuya fecha está fuera de la
     * vigencia configurada (RNF-07). No toca la base de datos, así que responde de inmediato.
     */
    public void validarEvento(EventoWompiRequest evento) {
        if (!firmaWompiService.eventoValido(evento)) {
            log.warn("Evento de Wompi rechazado por firma inválida o vencida (evento={}, timestamp={})",
                    evento == null ? null : evento.event(), evento == null ? null : evento.timestampTexto());
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Firma del evento inválida o vencida");
        }
        // Junto con el warn de arriba da la métrica de RNF-07: válidos / (válidos + rechazados)
        log.info("Evento de Wompi con firma válida (evento={}, timestamp={})",
                evento.event(), evento.timestampTexto());
    }

    /** Aplica un evento ya validado con {@link #validarEvento}. Es idempotente ante reenvíos. */
    @Transactional
    public void aplicarEvento(EventoWompiRequest evento) {
        if (!EVENTO_TRANSACCION.equals(evento.event())) {
            log.info("Evento de Wompi ignorado: {}", evento.event());
            return;
        }
        TransaccionWompi transaccionWompi;
        try {
            transaccionWompi = TransaccionWompi.desdeMapa(evento.data().get("transaction"));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        if (transaccionWompi.referencia() == null) {
            log.warn("Evento de Wompi sin referencia (transacción {})", transaccionWompi.id());
            return;
        }
        transaccionPagoRepository.bloquearPorReferencia(transaccionWompi.referencia())
                .ifPresentOrElse(transaccion -> aplicar(transaccion, transaccionWompi),
                        () -> log.warn("Evento de Wompi con referencia desconocida: {}",
                                transaccionWompi.referencia()));
    }

    /** Estado de un pago del cliente autenticado; 404 si no existe o es de otro cliente. */
    @Transactional(readOnly = true)
    public ResultadoPagoResponse consultarPorReferencia(String referencia) {
        AuthenticatedUser usuario = usuarioActual();
        return transaccionPagoRepository.findByReferencia(referencia)
                .filter(transaccion -> perteneceA(transaccion, usuario))
                .map(this::respuesta)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
    }

    /**
     * Aplica el estado reportado por Wompi. Ignora transacciones ya finalizadas (salvo un cobro aprobado
     * sobre una anulada, que queda para reembolso), marca error si el monto o la moneda no coinciden,
     * confirma el pedido si se aprobó y avisa al cliente si se rechazó.
     */
    private void aplicar(TransaccionPago transaccion, TransaccionWompi transaccionWompi) {
        EstadoTransaccionPago estadoAnterior = transaccion.getEstado();
        if (estadoAnterior.esFinal()) {
            // El cliente anuló el pago, pero Wompi aprobó un cobro tardío: hay que devolverle
            // el dinero. Cualquier otro estado final se ignora (evento idempotente u tardío).
            if (estadoAnterior == EstadoTransaccionPago.ANULADA
                    && EstadoTransaccionPago.desdeWompi(transaccionWompi.estado()) == EstadoTransaccionPago.APROBADA) {
                log.warn("Cobro aprobado {} sobre una transacción anulada; requiere reembolso "
                                + "(informado {} {}, esperado {} {})",
                        transaccion.getReferencia(), transaccionWompi.montoCentavos(),
                        transaccionWompi.moneda(), transaccion.getMontoCentavos(), transaccion.getMoneda());
                transaccion.marcarReembolsoPendiente(MotivoReembolso.PAGO_ANULADO, List.of());
                transaccion.setFechaActualizacion(LocalDateTime.now(clock));
            }
            return;
        }
        if (!Objects.equals(transaccionWompi.montoCentavos(), transaccion.getMontoCentavos())
                || !transaccion.getMoneda().equals(transaccionWompi.moneda())) {
            log.warn("Monto o moneda de Wompi no coinciden con la transacción {} (esperado {} {}, recibido {} {})",
                    transaccion.getReferencia(), transaccion.getMontoCentavos(), transaccion.getMoneda(),
                    transaccionWompi.montoCentavos(), transaccionWompi.moneda());
            transaccion.setEstado(EstadoTransaccionPago.ERROR);
            transaccion.setFechaActualizacion(LocalDateTime.now(clock));
            return;
        }

        transaccion.setWompiTransaccionId(transaccionWompi.id());
        transaccion.setMetodoPago(transaccionWompi.metodoPago());
        EstadoTransaccionPago nuevoEstado = EstadoTransaccionPago.desdeWompi(transaccionWompi.estado());
        if (nuevoEstado == EstadoTransaccionPago.APROBADA) {
            confirmarPedido(transaccion);
        } else {
            transaccion.setEstado(nuevoEstado);
            if (nuevoEstado == EstadoTransaccionPago.RECHAZADA) {
                // Sin cargo ni pedido: se avisa al cliente para que reintente.
                eventos.publishEvent(new PedidoPagoRechazadoEvent(transaccion.getId()));
            }
        }
        transaccion.setFechaActualizacion(LocalDateTime.now(clock));
    }

    /**
     * Crea el pedido desde el snapshot o confirma el existente. Si el pedido ya no está pendiente de pago, la
     * transacción queda para reembolso.
     */
    private void confirmarPedido(TransaccionPago transaccion) {
        if (transaccion.getPedido() == null) {
            ResultadoConfirmacionPago resultado = procesadorCheckoutPagadoService.confirmar(transaccion);
            procesarResultadoConfirmacion(transaccion, resultado);
            return;
        }
        Long pedidoId = transaccion.getPedido().getId();
        Pedido pedido = pedidoRepository.bloquearPorId(pedidoId)
                .orElseThrow(() -> new IllegalStateException("No se encontró el pedido " + pedidoId));
        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {
            log.warn("Pago aprobado {} para el pedido {} en estado {}; requiere reembolso",
                    transaccion.getReferencia(), pedidoId, pedido.getEstado());
            transaccion.marcarReembolsoPendiente(MotivoReembolso.PEDIDO_NO_PAGABLE, List.of());
            return;
        }

        ResultadoConfirmacionPago resultado = pedidoPagoService.confirmarPago(pedidoId);
        procesarResultadoConfirmacion(transaccion, resultado);
    }

    /** Marca la transacción como aprobada o, si faltó stock, para reembolso con las prendas agotadas. */
    private void procesarResultadoConfirmacion(TransaccionPago transaccion, ResultadoConfirmacionPago resultado) {
        Long pedidoId = transaccion.getPedido().getId();
        switch (resultado.estado()) {
            case CONFIRMADO -> transaccion.setEstado(EstadoTransaccionPago.APROBADA);
            case STOCK_INSUFICIENTE -> {
                log.warn("Pago aprobado {} sin stock suficiente; pedido {} cancelado, requiere reembolso",
                        transaccion.getReferencia(), pedidoId);
                transaccion.marcarReembolsoPendiente(MotivoReembolso.STOCK_INSUFICIENTE,
                        resultado.prendasAgotadas().stream()
                                .map(linea -> new PrendaAgotadaPago(linea.prendaId(), linea.nombre(),
                                        linea.talla(), linea.cantidadSolicitada(), linea.stockDisponible()))
                                .toList());
            }
        }
    }

    /** Arma el resultado del pago con datos del pedido o, si aún no existe, del snapshot. */
    private ResultadoPagoResponse respuesta(TransaccionPago transaccion) {
        Pedido pedido = transaccion.getPedido();
        PedidoPagoSnapshot snapshot = pedido == null ? deserializar(transaccion.getSnapshotCheckout()) : null;
        List<LineaDisponibilidadPagoResponse> agotadas = transaccion.getPrendasAgotadas().stream()
                .map(prenda -> new LineaDisponibilidadPagoResponse(prenda.getPrendaId(), prenda.getNombre(),
                        prenda.getTalla(), prenda.getCantidadSolicitada(), prenda.getStockDisponible()))
                .toList();
        List<LineaRecompraResponse> disponibles = List.of();
        if (transaccion.getMotivoReembolso() == MotivoReembolso.STOCK_INSUFICIENTE) {
            Set<String> clavesAgotadas = agotadas.stream()
                    .map(linea -> clave(linea.prendaId(), linea.talla()))
                    .collect(Collectors.toSet());
            if (pedido != null) {
                disponibles = pedido.getItems().stream()
                        .filter(linea -> !clavesAgotadas.contains(clave(linea.getPrendaId(), linea.getTalla())))
                        .map(linea -> new LineaRecompraResponse(linea.getPrendaId(), linea.getNombre(),
                                linea.getTalla(), linea.getCantidad()))
                        .toList();
            } else if (snapshot != null) {
                disponibles = snapshot.items().stream()
                        .filter(linea -> !clavesAgotadas.contains(clave(linea.prendaId(), linea.talla())))
                        .map(linea -> new LineaRecompraResponse(linea.prendaId(), linea.nombre(),
                                linea.talla(), linea.cantidad()))
                        .toList();
            }
        }
        return new ResultadoPagoResponse(
                pedido == null ? null : pedido.getId(),
                transaccion.getReferencia(),
                transaccion.getEstado(),
                pedido == null ? null : pedido.getEstado(),
                transaccion.getMotivoReembolso(),
                transaccion.getMetodoPago(),
                pedido == null ? BigDecimal.valueOf(transaccion.getMontoCentavos(), 2) : pedido.getTotal(),
                agotadas,
                disponibles);
    }

    /** Indica si la transacción (o su pedido) es del usuario. */
    private boolean perteneceA(TransaccionPago transaccion, AuthenticatedUser usuario) {
        if (transaccion.getCliente() != null) {
            return usuario.id().equals(transaccion.getCliente().getId());
        }
        Pedido pedido = transaccion.getPedido();
        return pedido != null && pedido.getCliente() != null
                && usuario.id().equals(pedido.getCliente().getId());
    }

    /** Usuario autenticado; 401 si no hay sesión. */
    private AuthenticatedUser usuarioActual() {
        return SecurityUtils.usuarioActual()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
    }

    /** Referencia única para Wompi: {@code LS-C<cliente>-<10 hex aleatorios>}. */
    private String generarReferenciaCheckout(Long clienteId) {
        byte[] aleatorio = new byte[5];
        random.nextBytes(aleatorio);
        return "LS-C" + clienteId + "-" + HexFormat.of().withUpperCase().formatHex(aleatorio);
    }

    /**
     * Datos para abrir el Widget: referencia, monto, llave pública, firma de integridad y datos del cliente.
     */
    private InicioPagoResponse respuestaInicio(TransaccionPago transaccion, AuthenticatedUser usuario) {
        return new InicioPagoResponse(transaccion.getPedido() == null ? null : transaccion.getPedido().getId(),
                transaccion.getReferencia(), transaccion.getMontoCentavos(), transaccion.getMoneda(),
                wompiProperties.llavePublica(),
                firmaWompiService.firmaIntegridad(transaccion.getReferencia(),
                        transaccion.getMontoCentavos(), transaccion.getMoneda()),
                wompiProperties.urlRedireccionPublica(), usuario.email(), usuario.nombre(),
                transaccion.getPedido() == null ? telefonoSnapshot(transaccion) : transaccion.getPedido().getTelefono());
    }

    /** Teléfono de la dirección de envío guardada en el snapshot. */
    private String telefonoSnapshot(TransaccionPago transaccion) {
        PedidoPagoSnapshot snapshot = deserializar(transaccion.getSnapshotCheckout());
        return snapshot == null ? null : snapshot.direccion().telefono();
    }

    /** Convierte el snapshot del checkout a JSON. */
    private String serializar(PedidoPagoSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JacksonException error) {
            throw new IllegalStateException("No fue posible guardar los datos del checkout", error);
        }
    }

    /** Lee el snapshot del checkout desde JSON; {@code null} si no hay. */
    private PedidoPagoSnapshot deserializar(String snapshot) {
        if (snapshot == null) {
            return null;
        }
        try {
            return objectMapper.readValue(snapshot, PedidoPagoSnapshot.class);
        } catch (JacksonException error) {
            throw new IllegalStateException("No fue posible recuperar los datos del checkout", error);
        }
    }

    /** Clave de idempotencia sin espacios, o {@code null}; máximo 64 caracteres. */
    private static String normalizarClave(String clave) {
        if (clave == null || clave.isBlank()) {
            return null;
        }
        if (clave.trim().length() > 64) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La clave de idempotencia admite máximo 64 caracteres", "Idempotency-Key");
        }
        return clave.trim();
    }

    /** Convierte pesos a centavos redondeando a dos decimales. */
    static long centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    /** Llave prenda y talla para comparar líneas. */
    private static String clave(Long prendaId, String talla) {
        return prendaId + "|" + talla.trim().toUpperCase(Locale.ROOT);
    }
}
