package co.edu.uniquindio.littlestyle.shared.correo;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorreoServiceTest {

    @Mock
    private JavaMailSender envio;

    private static CorreoProperties props(String usuario, String clave) {
        return new CorreoProperties("smtp.gmail.com", 587, usuario, clave, "LittleStyle",
                10000, 3, 0, "http://localhost:8080");
    }

    @Test
    void sinCredencialesNoEnviaNada() {
        CorreoService servicio = new CorreoService(envio,
                props(null, null));

        servicio.enviarPedidoEnviado("a@b.com", "Ana", 7L, "COL123", "107000", List.of());

        verify(envio, never()).createMimeMessage();
        verify(envio, never()).send(any(MimeMessage.class));
    }

    @Test
    void conCredencialesEnviaElHtml() throws Exception {
        MimeMessage mensaje = new MimeMessage((jakarta.mail.Session) null);
        when(envio.createMimeMessage()).thenReturn(mensaje);
        CorreoService servicio = new CorreoService(envio,
                props("tienda@gmail.com", "clave"));

        servicio.enviarPedidoEnviado("a@b.com", "Ana", 7L, "COL123", "107000",
                List.of(new CorreoService.ItemCorreo("Pantalón", "4", 1, "107000", "107000", "/media/a.png")));

        verify(envio).send(mensaje);
        assert mensaje.getSubject().contains("COL123");
    }

    @Test
    void escaparHtmlNeutralizaInyeccion() {
        assert CorreoService.escaparHtml("<script>alert(1)</script>").equals("&lt;script&gt;alert(1)&lt;/script&gt;");
        assert CorreoService.escaparHtml("A & B \"C\"").equals("A &amp; B &quot;C&quot;");
        assert CorreoService.escaparHtml(null).isEmpty();
    }

    @Test
    void conCredencialesEnviaCreacionPagoEstadoYAlerta() throws Exception {
        MimeMessage mensaje = new MimeMessage((jakarta.mail.Session) null);
        when(envio.createMimeMessage()).thenReturn(mensaje);
        CorreoService servicio = new CorreoService(envio,
                props("tienda@gmail.com", "clave"));

        servicio.enviarPedidoCreado("a@b.com", "Ana", 7L, "107000",
                List.of(new CorreoService.ItemCorreo("Pantalón", "4", 1, "107000", "107000", "/media/a.png")));
        servicio.enviarPagoConfirmado("a@b.com", "Ana", 7L, "107000",
                List.of(new CorreoService.ItemCorreo("Pantalón", "4", 1, "107000", "107000", "/media/a.png")));
        servicio.enviarCambioEstado("a@b.com", "Ana", 7L, "EN_PREPARACION", "107000");
        servicio.enviarAlertaStock("v@tienda.com", "Tienda Sol", "Pantalón", "4", "BAJO", 2, 5);

        verify(envio, org.mockito.Mockito.times(4)).send(mensaje);
    }

    @Test
    void reintentaElEnvioAnteFalloTransitorio() {
        MimeMessage mensaje = new MimeMessage((jakarta.mail.Session) null);
        when(envio.createMimeMessage()).thenReturn(mensaje);
        org.mockito.Mockito.doThrow(new RuntimeException("caído"))
                .doThrow(new RuntimeException("caído"))
                .doNothing()
                .when(envio).send(any(MimeMessage.class));
        CorreoService servicio = new CorreoService(envio,
                props("tienda@gmail.com", "clave"));

        servicio.enviarPedidoCreado("a@b.com", "Ana", 7L, "107000",
                List.of(new CorreoService.ItemCorreo("Pantalón", "4", 1, "107000", "107000", "/media/a.png")));

        verify(envio, org.mockito.Mockito.times(3)).send(mensaje);
    }

    @Test
    void avisaAlVendedorDelPedidoNuevo() throws Exception {
        MimeMessage mensaje = new MimeMessage((jakarta.mail.Session) null);
        when(envio.createMimeMessage()).thenReturn(mensaje);
        CorreoService servicio = new CorreoService(envio,
                props("tienda@gmail.com", "clave"));

        servicio.enviarPedidoNuevoVendedor("v@tienda.com", "Tienda Sol", 7L, "Ana",
                List.of(new CorreoService.ItemCorreo("Pantalón", "4", 1, "107000", "107000", "/media/a.png")));

        verify(envio).send(mensaje);
        assert mensaje.getSubject().contains("#7");
    }

    @Test
    void detalleDelPedidoConMiniImagenEInfo() throws Exception {
        MimeMessage mensaje = new MimeMessage((jakarta.mail.Session) null);
        when(envio.createMimeMessage()).thenReturn(mensaje);
        CorreoService servicio = new CorreoService(envio,
                props("tienda@gmail.com", "clave"));

        servicio.enviarPedidoEnviado("a@b.com", "Ana", 7L, "COL123", "107000",
                List.of(new CorreoService.ItemCorreo("Pantalón", "4", 1, "107000", "107000", "/media/a.png")));

        verify(envio).send(mensaje);
        String html = String.valueOf(mensaje.getContent());
        assert html.contains("<img") && html.contains("http://localhost:8080/media/a.png");
        assert html.contains("Talla 4") && html.contains("Cantidad 1");
        assert html.contains("$107000");
    }

    @Test
    void sinFotoUsaInicialYNoRompeLaFila() throws Exception {
        MimeMessage mensaje = new MimeMessage((jakarta.mail.Session) null);
        when(envio.createMimeMessage()).thenReturn(mensaje);
        CorreoService servicio = new CorreoService(envio, props("tienda@gmail.com", "clave"));

        servicio.enviarPedidoCreado("a@b.com", "Ana", 7L, "107000",
                List.of(new CorreoService.ItemCorreo("Pantalón", "4", 1, "107000", "107000", null)));

        verify(envio).send(mensaje);
        String html = String.valueOf(mensaje.getContent());
        assert !html.contains("<img");
        assert html.contains("Pantalón") && html.contains("Talla 4");
    }

    @Test
    void avisaElPagoRechazadoConLaReferencia() throws Exception {        MimeMessage mensaje = new MimeMessage((jakarta.mail.Session) null);
        when(envio.createMimeMessage()).thenReturn(mensaje);
        CorreoService servicio = new CorreoService(envio,
                props("tienda@gmail.com", "clave"));

        servicio.enviarPagoRechazado("a@b.com", "Ana", "LS-55-X", "7280000");

        verify(envio).send(mensaje);
        assert mensaje.getSubject().contains("LS-55-X");
    }

    @Test
    void sinCredencialesOmiteLosNuevosAvisos() {
        CorreoService servicio = new CorreoService(envio,
                props(null, null));

        servicio.enviarPedidoCreado("a@b.com", "Ana", 7L, "107000", List.of());
        servicio.enviarPagoConfirmado("a@b.com", "Ana", 7L, "107000", List.of());
        servicio.enviarPagoRechazado("a@b.com", "Ana", "LS-55-X", "7280000");
        servicio.enviarCambioEstado("a@b.com", "Ana", 7L, "EN_PREPARACION", "107000");
        servicio.enviarAlertaStock("v@tienda.com", "Tienda Sol", "Pantalón", "4", "BAJO", 2, 5);

        verify(envio, never()).send(any(MimeMessage.class));
    }
}
