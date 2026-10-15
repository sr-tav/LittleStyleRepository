package co.edu.uniquindio.littlestyle.shared.correo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Envío asíncrono de correos transaccionales. Un fallo de correo nunca rompe
 * la operación que lo originó: se registra y se continúa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CorreoService {

    public record ItemCorreo(String nombre, String talla, int cantidad) {
    }

    private final JavaMailSender envio;
    private final CorreoProperties props;

    @Async
    public void enviarPedidoEnviado(String para, String nombre, Long pedidoId,
                                    String guia, String total, List<ItemCorreo> items) {
        if (!props.enviosHabilitados()) {
            log.warn("Correo no configurado; se omite el aviso del pedido {}", pedidoId);
            return;
        }
        try {
            String plantilla = new String(
                    new ClassPathResource("correo/pedido-enviado.html").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            String filas = items.stream()
                    .map(i -> "<li>" + escaparHtml(i.nombre()) + " (talla " + escaparHtml(i.talla()) + ") x"
                            + i.cantidad() + "</li>")
                    .reduce("", String::concat);
            String html = plantilla
                    .replace("{{nombre}}", escaparHtml(nombre))
                    .replace("{{pedidoId}}", String.valueOf(pedidoId))
                    .replace("{{guia}}", escaparHtml(guia))
                    .replace("{{total}}", escaparHtml(total))
                    .replace("{{filas}}", filas);
            var mensaje = envio.createMimeMessage();
            var ayuda = new MimeMessageHelper(mensaje, false, "UTF-8");
            ayuda.setTo(para);
            ayuda.setFrom(props.usuario(), props.remitente());
            ayuda.setSubject("Tu pedido #" + pedidoId + " va en camino - guía " + guia);
            ayuda.setText(html, true);
            envio.send(mensaje);
        } catch (Exception ex) {
            log.error("No fue posible enviar el correo del pedido {}", pedidoId, ex);
        }
    }

    @Async
    public void enviarPedidoCreado(String para, String nombre, Long pedidoId,
                                   String total, List<ItemCorreo> items) {
        if (!props.enviosHabilitados()) {
            log.warn("Correo no configurado; se omite el aviso de creación del pedido {}", pedidoId);
            return;
        }
        try {
            String html = leerPlantilla("correo/pedido-creado.html")
                    .replace("{{nombre}}", escaparHtml(nombre))
                    .replace("{{pedidoId}}", String.valueOf(pedidoId))
                    .replace("{{total}}", escaparHtml(total))
                    .replace("{{filas}}", filasHtml(items));
            enviarHtml(para, "Recibimos tu pedido #" + pedidoId, html);
        } catch (Exception ex) {
            log.error("No fue posible enviar el correo de creación del pedido {}", pedidoId, ex);
        }
    }

    @Async
    public void enviarPagoConfirmado(String para, String nombre, Long pedidoId,
                                     String total, List<ItemCorreo> items) {
        if (!props.enviosHabilitados()) {
            log.warn("Correo no configurado; se omite el aviso de pago del pedido {}", pedidoId);
            return;
        }
        try {
            String html = leerPlantilla("correo/pedido-pagado.html")
                    .replace("{{nombre}}", escaparHtml(nombre))
                    .replace("{{pedidoId}}", String.valueOf(pedidoId))
                    .replace("{{total}}", escaparHtml(total))
                    .replace("{{filas}}", filasHtml(items));
            enviarHtml(para, "Tu pago del pedido #" + pedidoId + " fue aprobado", html);
        } catch (Exception ex) {
            log.error("No fue posible enviar el correo de pago del pedido {}", pedidoId, ex);
        }
    }

    @Async
    public void enviarCambioEstado(String para, String nombre, Long pedidoId,
                                   String estado, String total) {
        if (!props.enviosHabilitados()) {
            log.warn("Correo no configurado; se omite el aviso de estado del pedido {}", pedidoId);
            return;
        }
        try {
            String html = leerPlantilla("correo/pedido-estado.html")
                    .replace("{{nombre}}", escaparHtml(nombre))
                    .replace("{{pedidoId}}", String.valueOf(pedidoId))
                    .replace("{{estado}}", escaparHtml(estado))
                    .replace("{{total}}", escaparHtml(total));
            enviarHtml(para, "Tu pedido #" + pedidoId + " cambió a " + estado, html);
        } catch (Exception ex) {
            log.error("No fue posible enviar el correo de estado del pedido {}", pedidoId, ex);
        }
    }

    @Async
    public void enviarAlertaStock(String para, String nombre, String prenda,
                                  String nivel, int stock, int minimo) {
        if (!props.enviosHabilitados()) {
            log.warn("Correo no configurado; se omite la alerta de stock de {}", prenda);
            return;
        }
        try {
            String html = leerPlantilla("correo/alerta-stock.html")
                    .replace("{{nombre}}", escaparHtml(nombre))
                    .replace("{{prenda}}", escaparHtml(prenda))
                    .replace("{{nivel}}", escaparHtml(nivel))
                    .replace("{{stock}}", String.valueOf(stock))
                    .replace("{{minimo}}", String.valueOf(minimo));
            enviarHtml(para, "Alerta de inventario: " + prenda, html);
        } catch (Exception ex) {
            log.error("No fue posible enviar la alerta de stock de {}", prenda, ex);
        }
    }

    /** Escapa texto para inyectarlo seguro en la plantilla HTML del correo. */
    static String escaparHtml(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String leerPlantilla(String recurso) throws java.io.IOException {
        return new String(
                new ClassPathResource(recurso).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    private static String filasHtml(List<ItemCorreo> items) {
        return items.stream()
                .map(i -> "<li>" + escaparHtml(i.nombre()) + " (talla " + escaparHtml(i.talla()) + ") x"
                        + i.cantidad() + "</li>")
                .reduce("", String::concat);
    }

    private void enviarHtml(String para, String asunto, String html)
            throws jakarta.mail.MessagingException, java.io.UnsupportedEncodingException {
        var mensaje = envio.createMimeMessage();
        var ayuda = new MimeMessageHelper(mensaje, false, "UTF-8");
        ayuda.setTo(para);
        ayuda.setFrom(props.usuario(), props.remitente());
        ayuda.setSubject(asunto);
        ayuda.setText(html, true);
        envio.send(mensaje);
    }
}
