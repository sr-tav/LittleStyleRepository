package co.edu.uniquindio.littlestyle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada del backend de LittleStyle (Spring Boot). */
@SpringBootApplication
public class LittleStyleApplication {

    /** Arranca el contexto de Spring y el servidor web embebido. */
    public static void main(String[] args) {
        SpringApplication.run(LittleStyleApplication.class, args);
    }

}
