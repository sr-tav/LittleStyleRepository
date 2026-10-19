package co.edu.uniquindio.littlestyle.shared.correo;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Properties;

/** Habilita el envío asíncrono de correos y registra sus propiedades. */
@Configuration
@EnableAsync
@EnableConfigurationProperties(CorreoProperties.class)
public class CorreoConfig {

    /** Cliente SMTP con autenticación, STARTTLS y los tiempos de espera configurados (RNF-23). */
    @Bean
    public JavaMailSender javaMailSender(CorreoProperties props) {
        JavaMailSenderImpl envio = new JavaMailSenderImpl();
        envio.setHost(props.host());
        envio.setPort(props.puerto());
        envio.setUsername(props.usuario());
        envio.setPassword(props.clave());
        Properties smtp = new Properties();
        smtp.put("mail.smtp.auth", "true");
        smtp.put("mail.smtp.starttls.enable", "true");
        smtp.put("mail.smtp.connectiontimeout", String.valueOf(props.timeoutMs()));
        smtp.put("mail.smtp.timeout", String.valueOf(props.timeoutMs()));
        smtp.put("mail.smtp.writetimeout", String.valueOf(props.timeoutMs()));
        envio.setJavaMailProperties(smtp);
        return envio;
    }
}
