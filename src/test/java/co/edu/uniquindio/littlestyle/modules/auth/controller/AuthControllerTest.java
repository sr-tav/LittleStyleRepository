package co.edu.uniquindio.littlestyle.modules.auth.controller;

import co.edu.uniquindio.littlestyle.modules.auth.dto.AuthResponse;
import co.edu.uniquindio.littlestyle.modules.auth.dto.LoginRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.RegistroRequest;
import co.edu.uniquindio.littlestyle.modules.auth.dto.UsuarioResponse;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.service.AuthService;
import co.edu.uniquindio.littlestyle.shared.exception.EmailYaRegistradoException;
import co.edu.uniquindio.littlestyle.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del controlador con MockMvc en modo standalone: valida el contrato HTTP
 * (códigos de estado, validación de inputs y formato de errores) con el servicio simulado.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    private static final String REGISTRO_VALIDO = """
            {
              "nombre": "María", "apellido": "González", "email": "maria@correo.com",
              "telefono": "3001234567", "password": "Clave1234", "confirmarPassword": "Clave1234",
              "rol": "CLIENTE", "aceptaTerminos": true
            }
            """;

    private static final AuthResponse RESPUESTA = AuthResponse.bearer("jwt-token", 123L,
            new UsuarioResponse(1L, "María", "González", "maria@correo.com", "3001234567", null, Rol.CLIENTE));

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registroValidoDevuelve201ConToken() throws Exception {
        when(authService.registrar(any(RegistroRequest.class))).thenReturn(RESPUESTA);

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"));
    }

    @Test
    void registroConInputsInvalidosDevuelve400ConErroresPorCampo() throws Exception {
        String invalido = """
                {
                  "nombre": "M4ria", "apellido": "", "email": "no-es-correo",
                  "telefono": "123", "password": "corta", "confirmarPassword": "corta",
                  "rol": "CLIENTE", "aceptaTerminos": false
                }
                """;

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.apellido").exists())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.telefono").exists())
                .andExpect(jsonPath("$.errores.password").exists())
                .andExpect(jsonPath("$.errores.aceptaTerminos").exists());
        verifyNoInteractions(authService);
    }

    @Test
    void registroConRolDesconocidoDevuelve400() throws Exception {
        String rolInvalido = REGISTRO_VALIDO.replace("\"CLIENTE\"", "\"SUPERUSUARIO\"");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(rolInvalido))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    @Test
    void registroConCorreoDuplicadoDevuelve409() throws Exception {
        when(authService.registrar(any(RegistroRequest.class)))
                .thenThrow(new EmailYaRegistradoException("maria@correo.com"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.email").exists());
    }

    @Test
    void loginValidoDevuelve200() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(RESPUESTA);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@correo.com\",\"password\":\"Clave1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void loginConCredencialesIncorrectasDevuelve401() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@correo.com\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    @Test
    void loginDeCuentaSuspendidaDevuelve403() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new DisabledException("disabled"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@correo.com\",\"password\":\"Clave1234\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginSinCamposDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.password").exists());
        verifyNoInteractions(authService);
    }
}
