package co.edu.uniquindio.littlestyle;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test de humo: solo comprueba que el contexto de Spring Boot levanta
 * completo con el perfil "test", sin mocks ni configuración adicional.
 */
@SpringBootTest
@ActiveProfiles("test")
class LittleStyleApplicationTests {

    // Si el contexto no levanta, falla aquí y no en cada test individual
    @Test
    void contextLoads() {
    }

}
