package co.edu.uniquindio.littlestyle.shared.correo;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Properties;

@Configuration
@EnableAsync
@EnableConfigurationProperties(CorreoProperties.class)
public class CorreoConfig {

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
        envio.setJavaMailProperties(smtp);
        return envio;
    }
}
