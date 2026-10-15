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

    @Test
    void sinCredencialesNoEnviaNada() {
        CorreoService servicio = new CorreoService(envio,
                new CorreoProperties("smtp.gmail.com", 587, null, null, "LittleStyle"));

        servicio.enviarPedidoEnviado("a@b.com", "Ana", 7L, "COL123", "107000", List.of());

        verify(envio, never()).createMimeMessage();
        verify(envio, never()).send(any(MimeMessage.class));
    }

    @Test
    void conCredencialesEnviaElHtml() throws Exception {
        MimeMessage mensaje = new MimeMessage((jakarta.mail.Session) null);
        when(envio.createMimeMessage()).thenReturn(mensaje);
        CorreoService servicio = new CorreoService(envio,
                new CorreoProperties("smtp.gmail.com", 587, "tienda@gmail.com", "clave", "LittleStyle"));

        servicio.enviarPedidoEnviado("a@b.com", "Ana", 7L, "COL123", "107000",
                List.of(new CorreoService.ItemCorreo("Pantalón", "4", 1)));

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
                new CorreoProperties("smtp.gmail.com", 587, "tienda@gmail.com", "clave", "LittleStyle"));

        servicio.enviarPedidoCreado("a@b.com", "Ana", 7L, "107000",
                List.of(new CorreoService.ItemCorreo("Pantalón", "4", 1)));
        servicio.enviarPagoConfirmado("a@b.com", "Ana", 7L, "107000",
                List.of(new CorreoService.ItemCorreo("Pantalón", "4", 1)));
        servicio.enviarCambioEstado("a@b.com", "Ana", 7L, "EN_PREPARACION", "107000");
        servicio.enviarAlertaStock("v@tienda.com", "Tienda Sol", "Pantalón", "BAJO", 2, 5);

        verify(envio, org.mockito.Mockito.times(4)).send(mensaje);
    }

    @Test
    void sinCredencialesOmiteLosNuevosAvisos() {
        CorreoService servicio = new CorreoService(envio,
                new CorreoProperties("smtp.gmail.com", 587, null, null, "LittleStyle"));

        servicio.enviarPedidoCreado("a@b.com", "Ana", 7L, "107000", List.of());
        servicio.enviarPagoConfirmado("a@b.com", "Ana", 7L, "107000", List.of());
        servicio.enviarCambioEstado("a@b.com", "Ana", 7L, "EN_PREPARACION", "107000");
        servicio.enviarAlertaStock("v@tienda.com", "Tienda Sol", "Pantalón", "BAJO", 2, 5);

        verify(envio, never()).send(any(MimeMessage.class));
    }
}
