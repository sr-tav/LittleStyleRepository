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

    /**
     * Línea del pedido en el correo, como cualquier e-commerce: mini imagen
     * del producto más su información importante (nombre, talla, cantidad y
     * precios congelados al momento de la compra). La imagen puede ser nula
     * (prenda sin fotos o eliminada): en ese caso la fila sale sin imagen.
     */
    public record ItemCorreo(String nombre, String talla, int cantidad, String precioUnitario,
                             String subtotal, String imagenUrl) {
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
            String filas = filasHtml(items);
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
            enviarConReintentos(mensaje, "pedido " + pedidoId);
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
    public void enviarAlertaStock(String para, String nombre, String prenda, String talla,
                                  String nivel, int stock, int minimo) {
        if (!props.enviosHabilitados()) {
            log.warn("Correo no configurado; se omite la alerta de stock de {}", prenda);
            return;
        }
        try {
            String html = leerPlantilla("correo/alerta-stock.html")
                    .replace("{{nombre}}", escaparHtml(nombre))
                    .replace("{{prenda}}", escaparHtml(prenda))
                    .replace("{{talla}}", escaparHtml(talla))
                    .replace("{{nivel}}", escaparHtml(nivel))
                    .replace("{{stock}}", String.valueOf(stock))
                    .replace("{{minimo}}", String.valueOf(minimo));
            enviarHtml(para, "Alerta de inventario: " + prenda + " talla " + talla, html);
        } catch (Exception ex) {
            log.error("No fue posible enviar la alerta de stock de {}", prenda, ex);
        }
    }

    @Async
    public void enviarPagoRechazado(String para, String nombre, String referencia, String totalCentavos) {
        if (!props.enviosHabilitados()) {
            log.warn("Correo no configurado; se omite el aviso de pago rechazado {}", referencia);
            return;
        }
        try {
            String html = leerPlantilla("correo/pago-rechazado.html")
                    .replace("{{nombre}}", escaparHtml(nombre))
                    .replace("{{referencia}}", escaparHtml(referencia))
                    .replace("{{totalCentavos}}", escaparHtml(totalCentavos));
            enviarHtml(para, "Su pago no pudo ser aprobado - referencia " + referencia, html);
        } catch (Exception ex) {
            log.error("No fue posible enviar el correo de pago rechazado {}", referencia, ex);
        }
    }

    @Async
    public void enviarPedidoNuevoVendedor(String para, String tienda, Long pedidoId,
                                          String cliente, List<ItemCorreo> items) {
        if (!props.enviosHabilitados()) {
            log.warn("Correo no configurado; se omite el aviso al vendedor del pedido {}", pedidoId);
            return;
        }
        try {
            String html = leerPlantilla("correo/pedido-vendedor-nuevo.html")
                    .replace("{{tienda}}", escaparHtml(tienda))
                    .replace("{{pedidoId}}", String.valueOf(pedidoId))
                    .replace("{{cliente}}", escaparHtml(cliente))
                    .replace("{{filas}}", filasHtml(items));
            enviarHtml(para, "Nuevo pedido #" + pedidoId + " con tus productos", html);
        } catch (Exception ex) {
            log.error("No fue posible enviar el aviso al vendedor del pedido {}", pedidoId, ex);
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

    private String filasHtml(List<ItemCorreo> items) {
        return items.stream().map(this::filaHtml).reduce("", String::concat);
    }

    /**
     * Fila estilo e-commerce: miniatura 64×64, nombre, talla, cantidad y
     * precios congelados. Gmail no carga rutas relativas: se absolutizan.
     */
    private String filaHtml(ItemCorreo i) {
        String nombre = escaparHtml(i.nombre());
        String img = urlPublicaDe(i.imagenUrl());
        StringBuilder fila = new StringBuilder(
                "<li style=\"list-style:none;margin:0 0 12px;padding:0 0 12px;border-bottom:1px solid #e1e6c6;\">"
                        + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">"
                        + "<tr style=\"vertical-align:top;\">");
        fila.append("<td width=\"72\" style=\"width:72px;padding-right:12px;\">");
        if (img != null) {
            fila.append("<img src=\"").append(escaparHtml(img)).append("\" alt=\"").append(nombre)
                    .append("\" width=\"64\" height=\"64\" style=\"display:block;width:64px;height:64px;")
                    .append("object-fit:cover;border-radius:8px;border:1px solid #e1e6c6;\" />");
        } else {
            String inicial = nombre.isEmpty() ? "?" : nombre.substring(0, 1);
            fila.append("<div style=\"width:64px;height:64px;border-radius:8px;background:#d5f2ad;")
                    .append("color:#2b7d40;font-weight:700;text-align:center;line-height:64px;\">")
                    .append(inicial).append("</div>");
        }
        fila.append("</td><td style=\"font-family:Arial,sans-serif;color:#233127;font-size:14px;\">")
                .append("<strong>").append(nombre).append("</strong>")
                .append("<br />Talla ").append(escaparHtml(i.talla()))
                .append(" · Cantidad ").append(i.cantidad());
        if (i.precioUnitario() != null) {
            fila.append("<br />$").append(escaparHtml(i.precioUnitario())).append(" c/u");
        }
        fila.append("</td>");
        if (i.subtotal() != null) {
            fila.append("<td style=\"text-align:right;white-space:nowrap;font-family:Arial,sans-serif;")
                    .append("font-weight:700;color:#233127;\">$")
                    .append(escaparHtml(i.subtotal())).append("</td>");
        }
        return fila.append("</tr></table></li>").toString();
    }

    /** Convierte {@code /media/...} en URL absoluta; deja intactas las de S3. */
    String urlPublicaDe(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String recortada = url.trim();
        if (recortada.startsWith("http://") || recortada.startsWith("https://")) {
            return recortada;
        }
        String base = props.urlPublica() == null ? "" : props.urlPublica().replaceAll("/+$", "");
        return recortada.startsWith("/") ? base + recortada : base + "/" + recortada;
    }

    private void enviarHtml(String para, String asunto, String html) throws Exception {
        var mensaje = envio.createMimeMessage();
        var ayuda = new MimeMessageHelper(mensaje, false, "UTF-8");
        ayuda.setTo(para);
        ayuda.setFrom(props.usuario(), props.remitente());
        ayuda.setSubject(asunto);
        ayuda.setText(html, true);
        enviarConReintentos(mensaje, asunto);
    }

    /**
     * Reintentos de envío (ADR-14): el correo es secundario y asíncrono, pero
     * un fallo transitorio de red no debería perder el aviso al primer
     * intento. Agotados los intentos, lanza y el llamador solo lo registra.
     */
    private void enviarConReintentos(jakarta.mail.internet.MimeMessage mensaje, String contexto)
            throws Exception {
        int maximos = Math.max(1, props.reintentos());
        for (int intento = 1; ; intento++) {
            try {
                envio.send(mensaje);
                return;
            } catch (Exception ex) {
                if (intento >= maximos) {
                    throw ex;
                }
                log.warn("Reintento {}/{} del correo ({})", intento, maximos, contexto);
                try {
                    Thread.sleep(Math.max(0, props.esperaReintentoMs()));
                } catch (InterruptedException interrumpido) {
                    Thread.currentThread().interrupt();
                    throw interrumpido;
                }
            }
        }
    }
}
