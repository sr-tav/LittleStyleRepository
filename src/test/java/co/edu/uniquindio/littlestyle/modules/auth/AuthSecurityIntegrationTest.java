package co.edu.uniquindio.littlestyle.modules.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo contra la cadena real de Spring Security (H2 en memoria):
 * registro → login → acceso con JWT, y autorización por rol.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String registrar(String email, String rol, String tienda) throws Exception {
        String body = """
                {"nombre":"Laura","apellido":"Pérez","email":"%s","telefono":"3001234567",
                 "password":"Clave1234","confirmarPassword":"Clave1234","rol":"%s",
                 "nombreTienda":%s,"aceptaTerminos":true}
                """.formatted(email, rol, tienda == null ? "null" : "\"" + tienda + "\"");
        String json = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    @Test
    void registroLoginYAccesoConToken() throws Exception {
        registrar("laura@correo.com", "CLIENTE", null);

        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"LAURA@correo.com\",\"password\":\"Clave1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(json, "$.token");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("laura@correo.com"));
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        registrar("pedro@correo.com", "CLIENTE", null);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pedro@correo.com\",\"password\":\"Incorrecta1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointProtegidoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void tokenInvalidoDevuelve401ConMensaje() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer token.falso.123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token de autenticación inválido"));
    }

    @Test
    void rolSinPermisoDevuelve403() throws Exception {
        String tokenCliente = registrar("cliente403@correo.com", "CLIENTE", null);
        String tokenVendedor = registrar("vendedor403@correo.com", "VENDEDOR", "Tienda Feliz");

        mockMvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/vendedor/productos").header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/cliente/perfiles").header("Authorization", "Bearer " + tokenVendedor))
                .andExpect(status().isForbidden());
    }

    @Test
    void correoDuplicadoDevuelve409() throws Exception {
        registrar("repetido@correo.com", "CLIENTE", null);

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Otro","apellido":"Usuario","email":"repetido@correo.com","telefono":"3009876543",
                         "password":"Clave1234","confirmarPassword":"Clave1234","rol":"CLIENTE","aceptaTerminos":true}
                        """))
                .andExpect(status().isConflict());
    }
}
