package co.edu.uniquindio.littlestyle.modules.auth;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.auth.service.AuthService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    @Autowired
    private UsuarioRepository usuarioRepository;

    // Registra un usuario de prueba con ese rol y devuelve su token Bearer
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

    // El camino feliz: se registra, inicia sesión y con el token consulta su perfil
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

    // El registro debe dejar rastro de qué versión de términos aceptó el usuario
    @Test
    void registroGuardaLaAceptacionDeTerminos() throws Exception {
        registrar("terminos@correo.com", "CLIENTE", null);

        Usuario usuario = usuarioRepository.findByEmail("terminos@correo.com").orElseThrow();
        assertThat(usuario.getVersionTerminos()).isEqualTo(AuthService.VERSION_TERMINOS_VIGENTE);
        assertThat(usuario.getFechaAceptacionTerminos()).isNotNull();
    }

    // Suspender la cuenta debe invalidar el token ya emitido de inmediato,
    // porque el filtro valida el estado en cada request
    @Test
    void tokenDeCuentaSuspendidaDejaDeSerValidoDeInmediato() throws Exception {
        String token = registrar("suspendido@correo.com", "CLIENTE", null);
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Usuario usuario = usuarioRepository.findByEmail("suspendido@correo.com").orElseThrow();
        usuario.setEstado(EstadoUsuario.SUSPENDIDO);
        usuarioRepository.save(usuario);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("La cuenta no está activa"));
    }

    // Al actualizar la cuenta (y cambiar clave) se renueva la sesión y el
    // login con los datos nuevos funciona
    @Test
    void usuarioActualizaSuCuentaYConservaAccesoConSesionRenovada() throws Exception {
        String token = registrar("cuenta-us06@correo.com", "CLIENTE", null);
        String respuesta = mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Laura Isabel","apellido":"Pérez","email":"laura.us06@correo.com",
                                 "telefono":"3007654321","passwordActual":"Clave1234",
                                 "nuevaPassword":"NuevaClave567","confirmarNuevaPassword":"NuevaClave567"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.email").value("laura.us06@correo.com"))
                .andReturn().getResponse().getContentAsString();
        String tokenActualizado = JsonPath.read(respuesta, "$.token");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenActualizado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Laura Isabel"))
                .andExpect(jsonPath("$.telefono").value("3007654321"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"laura.us06@correo.com","password":"NuevaClave567"}
                                """))
                .andExpect(status().isOk());
    }

    // Una clave incorrecta no da pista de si el correo existe o no: siempre 401
    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        registrar("pedro@correo.com", "CLIENTE", null);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pedro@correo.com\",\"password\":\"Incorrecta1\"}"))
                .andExpect(status().isUnauthorized());
    }

    // Cinco fallos seguidos bloquean el login por correo + IP, incluso con la clave correcta
    @Test
    void loginSeBloqueaTrasCincoIntentosFallidos() throws Exception {
        registrar("bloqueo@correo.com", "CLIENTE", null);
        String incorrecta = "{\"email\":\"bloqueo@correo.com\",\"password\":\"Incorrecta1\"}";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(incorrecta))
                    .andExpect(status().isUnauthorized());
        }

        // Ni siquiera la contraseña correcta se acepta mientras dura el bloqueo
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bloqueo@correo.com\",\"password\":\"Clave1234\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    // Los endpoints protegidos exigen token, no se acepta invocarlos "en seco"
    @Test
    void endpointProtegidoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    // Un token trucado también responde 401, pero con un mensaje claro
    @Test
    void tokenInvalidoDevuelve401ConMensaje() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer token.falso.123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token de autenticación inválido"));
    }

    // Rutas por rol: un cliente no ve lo de vendedor, un vendedor no ve lo de cliente,
    // y los de administración se quedan solo para el ADMIN
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

    // Registrar el mismo correo dos veces no amarra: responde conflicto, no 500
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
