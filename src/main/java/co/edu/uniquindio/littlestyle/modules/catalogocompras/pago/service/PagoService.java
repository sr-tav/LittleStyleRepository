package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service;

import co.edu.uniquindio.littlestyle.config.CheckoutProperties;
import co.edu.uniquindio.littlestyle.config.PagoProperties;
import co.edu.uniquindio.littlestyle.config.WompiProperties;
import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.LineaDisponibilidadPagoResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * procesa los eventos verificados del webhook y la consulta directa a la API, y conecta la
 * aprobación con {@link PedidoPagoService}, que descuenta el stock de forma atómica (US-10).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoService {

    static final String EVENTO_TRANSACCION = "transaction.updated";

    private final PedidoRepository pedidoRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;
    private final PedidoPagoService pedidoPagoService;
    private final FirmaWompiService firmaWompiService;
    private final WompiClient wompiClient;
    private final WompiProperties wompiProperties;
    private final CheckoutProperties checkoutProperties;
    private final PagoProperties pagoProperties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public InicioPagoResponse iniciar(Long pedidoId) {
        if (!wompiProperties.pagosConfigurados()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La pasarela de pagos no está disponible en este momento");
        }
        AuthenticatedUser usuario = usuarioActual();
        Pedido pedido = pedidoRepository.bloquearPorId(pedidoId)
                .filter(encontrado -> perteneceA(encontrado, usuario))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {
            throw new BusinessException(HttpStatus.CONFLICT, "El pedido no está pendiente de pago");
        }
        if (pedidoVencido(pedido)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "El plazo para pagar este pedido venció. Vuelve a agregar las prendas al carrito.");
        }
        if (transaccionPagoRepository.existsByPedidoIdAndEstado(pedidoId, EstadoTransaccionPago.PENDIENTE)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Ya hay un pago en proceso para este pedido. Espera su confirmación antes de intentarlo de nuevo.");
        }
        if (transaccionPagoRepository.countByPedidoId(pedidoId) >= pagoProperties.maxIntentosPorPedido()) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                    "Alcanzaste el máximo de " + pagoProperties.maxIntentosPorPedido()
                            + " intentos de pago para este pedido. Vuelve a crear el pedido desde tu carrito.");
        }

        long montoCentavos = centavos(pedido.getTotal());
        String moneda = wompiProperties.moneda();
        String referencia = generarReferencia(pedidoId);
        LocalDateTime ahora = LocalDateTime.now(clock);
        transaccionPagoRepository.save(TransaccionPago.builder()
                .pedido(pedido)
                .referencia(referencia)
                .montoCentavos(montoCentavos)
                .moneda(moneda)
                .estado(EstadoTransaccionPago.CREADA)
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .build());

        return new InicioPagoResponse(pedidoId, referencia, montoCentavos, moneda,
                wompiProperties.llavePublica(),
                firmaWompiService.firmaIntegridad(referencia, montoCentavos, moneda),
                wompiProperties.urlRedireccionPublica(), usuario.email(), pedido.getDestinatario(),
                pedido.getTelefono());
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
                    .filter(encontrada -> perteneceA(encontrada.getPedido(), usuario))
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

    @Transactional(readOnly = true)
    public ResultadoPagoResponse consultarPorReferencia(String referencia) {
        AuthenticatedUser usuario = usuarioActual();
        return transaccionPagoRepository.findByReferencia(referencia)
                .filter(transaccion -> perteneceA(transaccion.getPedido(), usuario))
                .map(this::respuesta)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
    }

    private void aplicar(TransaccionPago transaccion, TransaccionWompi transaccionWompi) {
        if (transaccion.getEstado().esFinal()) {
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
        }
        transaccion.setFechaActualizacion(LocalDateTime.now(clock));
    }

    private void confirmarPedido(TransaccionPago transaccion) {
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
            case PEDIDO_VENCIDO -> {
                log.warn("Pago aprobado {} para el pedido vencido {}; requiere reembolso",
                        transaccion.getReferencia(), pedidoId);
                transaccion.marcarReembolsoPendiente(MotivoReembolso.PEDIDO_VENCIDO, List.of());
            }
        }
    }

    private ResultadoPagoResponse respuesta(TransaccionPago transaccion) {
        Pedido pedido = transaccion.getPedido();
        List<LineaDisponibilidadPagoResponse> agotadas = transaccion.getPrendasAgotadas().stream()
                .map(prenda -> new LineaDisponibilidadPagoResponse(prenda.getPrendaId(), prenda.getNombre(),
                        prenda.getTalla(), prenda.getCantidadSolicitada(), prenda.getStockDisponible()))
                .toList();
        List<LineaRecompraResponse> disponibles = List.of();
        if (transaccion.getMotivoReembolso() == MotivoReembolso.STOCK_INSUFICIENTE) {
            Set<String> clavesAgotadas = agotadas.stream()
                    .map(linea -> clave(linea.prendaId(), linea.talla()))
                    .collect(Collectors.toSet());
            disponibles = pedido.getItems().stream()
                    .filter(linea -> !clavesAgotadas.contains(clave(linea.getPrendaId(), linea.getTalla())))
                    .map(linea -> new LineaRecompraResponse(linea.getPrendaId(), linea.getNombre(),
                            linea.getTalla(), linea.getCantidad()))
                    .toList();
        }
        return new ResultadoPagoResponse(pedido.getId(), transaccion.getReferencia(), transaccion.getEstado(),
                pedido.getEstado(), transaccion.getMotivoReembolso(), transaccion.getMetodoPago(),
                pedido.getTotal(), agotadas, disponibles);
    }

    private boolean pedidoVencido(Pedido pedido) {
        LocalDateTime limite = LocalDateTime.now(clock).minus(checkoutProperties.tiempoExpiracionPedido());
        return !pedido.getFechaCreacion().isAfter(limite);
    }

    private boolean perteneceA(Pedido pedido, AuthenticatedUser usuario) {
        return pedido.getCliente() != null && usuario.id().equals(pedido.getCliente().getId());
    }

    private AuthenticatedUser usuarioActual() {
        return SecurityUtils.usuarioActual()
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
    }

    private String generarReferencia(Long pedidoId) {
        byte[] aleatorio = new byte[5];
        random.nextBytes(aleatorio);
        return "LS-" + pedidoId + "-" + HexFormat.of().withUpperCase().formatHex(aleatorio);
    }

    static long centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    private static String clave(Long prendaId, String talla) {
        return prendaId + "|" + talla.trim().toUpperCase(Locale.ROOT);
    }
}
