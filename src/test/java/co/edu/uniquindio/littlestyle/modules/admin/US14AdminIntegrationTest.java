package co.edu.uniquindio.littlestyle.modules.admin;

import co.edu.uniquindio.littlestyle.config.security.JwtService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.model.Carrito;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.model.ItemCarrito;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.repository.CarritoRepository;
import co.edu.uniquindio.littlestyle.modules.admin.model.AccionAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.RegistroAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.repository.RegistroAuditoriaRepository;
import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port.CatalogoRecomendacionPort;
import co.edu.uniquindio.littlestyle.modules.admin.model.EstadoIncidencia;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoIncidencia;
import co.edu.uniquindio.littlestyle.modules.admin.repository.IncidenciaRepository;
import co.edu.uniquindio.littlestyle.modules.admin.service.AuditoriaService;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integracion US-14 contra JWT, servicios, repositorios y esquema H2 reales. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class US14AdminIntegrationTest {

    private static final String PRENDA = """
            {"nombre":"Buzo US14","categoria":"PANTALONES","descripcion":"Prenda de prueba de moderacion",
             "instruccionesCuidado":"Lavar en agua fria","precio":32900,"colores":["AZUL"],
             "composicion":[{"material":"ALGODON","porcentaje":100}],
             "tallas":[{"talla":"4","estaturaMinCm":100,"estaturaMaxCm":110,
                        "pesoMinKg":15,"pesoMaxKg":20,"stock":8}]}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CarritoRepository carritoRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private CatalogoRecomendacionPort catalogoRecomendacion;
    @Autowired
    private RegistroAuditoriaRepository registroAuditoriaRepository;
    @Autowired
    private IncidenciaRepository incidenciaRepository;
    @Autowired
    private AuditoriaService auditoriaService;
    @Autowired
    private EntityManager entityManager;

    @Test
    void suspensionYDesactivacionSeReflejanEnCatalogoRecomendacionesYAuditoria() throws Exception {
        String sufijo = UUID.randomUUID().toString();
        Usuario admin = crearAdmin("admin-" + sufijo + "@correo.com");
        String tokenAdmin = token(admin);
        String tokenCliente = registrar("cliente-" + sufijo + "@correo.com", "CLIENTE", null);
        String tokenVendedorSuspendido = registrar("vendedor-suspendido-" + sufijo + "@correo.com",
                "VENDEDOR", "Tienda suspendida");
        String tokenVendedor = registrar("vendedor-" + sufijo + "@correo.com", "VENDEDOR", "Tienda activa");
        Long vendedorSuspendidoId = usuarioRepository.findByEmail(
                "vendedor-suspendido-" + sufijo + "@correo.com").orElseThrow().getId();
        Usuario cliente = usuarioRepository.findByEmail("cliente-" + sufijo + "@correo.com").orElseThrow();
        int prendaSuspendida = crearPrenda(tokenVendedorSuspendido);
        int prendaReportada = crearPrenda(tokenVendedor);
        int prendaModerada = crearPrenda(tokenVendedor);
        Carrito carrito = Carrito.builder().cliente(cliente).build();
        carrito.agregar(ItemCarrito.builder().prendaId((long) prendaSuspendida)
                .talla("4").cantidad(1).build());
        carrito.agregar(ItemCarrito.builder().prendaId((long) prendaReportada)
                .talla("4").cantidad(1).build());
        carrito.agregar(ItemCarrito.builder().prendaId((long) prendaModerada)
                .talla("4").cantidad(1).build());
        carritoRepository.save(carrito);

        mockMvc.perform(post("/api/cliente/incidencias")
                        .header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"REPORTE_PRODUCTO","asunto":"Revisar información del producto",
                                 "descripcion":"La composición publicada no coincide con la etiqueta de esta prenda.",
                                 "referenciaId":%d}
                                """.formatted(prendaReportada)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenciaId").value(prendaReportada));
        mockMvc.perform(get("/api/admin/moderacion/prendas")
                        .header("Authorization", tokenAdmin)
                        .param("vendedorEmail", "vendedor-" + sufijo + "@correo.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(prendaReportada));

        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id").value(org.hamcrest.Matchers.hasItem(prendaSuspendida)));
        assertThat(catalogoRecomendacion.buscarPrendaPublicada((long) prendaSuspendida)).isPresent();

        mockMvc.perform(put("/api/admin/usuarios/{id}/estado", vendedorSuspendidoId)
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"SUSPENDIDO",
                                 "motivo":"Incumplimiento confirmado de las políticas de publicación"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SUSPENDIDO"));

        mockMvc.perform(get("/api/vendedor/prendas").header("Authorization", tokenVendedorSuspendido))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(prendaSuspendida))));
        assertThat(catalogoRecomendacion.buscarPrendaPublicada((long) prendaSuspendida)).isEmpty();
        mockMvc.perform(get("/api/cliente/carrito").header("Authorization", tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].disponible").value(false));

        mockMvc.perform(post("/api/admin/moderacion/prendas/{id}/desactivar", prendaModerada)
                        .header("Authorization", tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"La ficha no informa la composición textil obligatoria"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoModeracion").value("DESACTIVADA"));

        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(prendaModerada))));
        assertThat(catalogoRecomendacion.buscarPrendaPublicada((long) prendaModerada)).isEmpty();
        mockMvc.perform(patch("/api/vendedor/prendas/{id}/estado", prendaModerada)
                        .header("Authorization", tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ACTIVA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        org.hamcrest.Matchers.containsString("La ficha no informa la composición")));
        mockMvc.perform(get("/api/vendedor/prendas/{id}", prendaModerada)
                        .header("Authorization", tokenVendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivoModeracion")
                        .value("La ficha no informa la composición textil obligatoria"));
        mockMvc.perform(get("/api/cliente/carrito").header("Authorization", tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[2].disponible").value(false));
        String carritoActual = mockMvc.perform(get("/api/cliente/carrito")
                        .header("Authorization", tokenCliente))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        int itemVendedorSuspendido = JsonPath.read(carritoActual, "$.items[0].id");
        mockMvc.perform(delete("/api/cliente/carrito/items/{id}", itemVendedorSuspendido)
                        .header("Authorization", tokenCliente))
                .andExpect(status().isOk());
        int perfilId = JsonPath.read(agregarPerfil(tokenCliente), "$.id");
        mockMvc.perform(post("/api/cliente/checkout/resumen")
                        .header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"perfilInfantilId":%d,
                                 "direccion":{"destinatario":"Ana Ruiz","direccion":"Calle 1 # 2-3",
                                 "codigoPostal":"630001","departamento":"Quindío",
                                 "municipio":"Armenia","telefono":"3001234567"}}
                                """.formatted(perfilId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/admin/resumen").header("Authorization", tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prendasSinRevisar").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.incidenciasAbiertas").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        assertThat(registroAuditoriaRepository.findAllByOrderByIdAsc())
                .extracting(RegistroAuditoria::getAccion)
                .contains(AccionAuditoria.SUSPENDER_USUARIO, AccionAuditoria.DESACTIVAR_PRODUCTO);
    }

    @Test
    void apiAdminExigeAutenticacionRolYValidacionDelMotivo() throws Exception {
        mockMvc.perform(get("/api/admin/resumen"))
                .andExpect(status().isUnauthorized());

        String sufijo = UUID.randomUUID().toString();
        String cliente = registrar("cliente-rol-" + sufijo + "@correo.com", "CLIENTE", null);
        String vendedor = registrar("vendedor-rol-" + sufijo + "@correo.com", "VENDEDOR", "Tienda rol");
        mockMvc.perform(get("/api/admin/resumen").header("Authorization", cliente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/resumen").header("Authorization", vendedor))
                .andExpect(status().isForbidden());

        Usuario admin = crearAdmin("admin-validacion-" + sufijo + "@correo.com");
        mockMvc.perform(put("/api/admin/usuarios/1/estado")
                        .header("Authorization", token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"BLOQUEADO\",\"motivo\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").exists());
    }

    @Test
    @Transactional
    void persisteHashConPrecisionDeBaseDeDatosYAbreApelacionEnLaTransaccion() throws Exception {
        String sufijo = UUID.randomUUID().toString();
        Usuario admin = crearAdmin("admin-auditoria-" + sufijo + "@correo.com");
        String vendedor = registrar("vendedor-apelacion-" + sufijo + "@correo.com",
                "VENDEDOR", "Tienda apelación");
        Long vendedorId = usuarioRepository.findByEmail("vendedor-apelacion-" + sufijo + "@correo.com")
                .orElseThrow().getId();

        mockMvc.perform(put("/api/admin/usuarios/{id}/estado", vendedorId)
                        .header("Authorization", token(admin))
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.17");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"SUSPENDIDO",
                                 "motivo":"Incumplimiento confirmado de las políticas de publicación"}
                                """))
                .andExpect(status().isOk());

        assertThat(incidenciaRepository.existsByReportanteIdAndTipoAndEstadoIn(
                vendedorId, TipoIncidencia.APELACION_SUSPENSION,
                List.of(EstadoIncidencia.ABIERTA, EstadoIncidencia.EN_REVISION))).isTrue();

        entityManager.flush();
        entityManager.clear();

        assertThat(registroAuditoriaRepository.findAllByOrderByIdAsc())
                .filteredOn(registro -> registro.getAccion() == AccionAuditoria.SUSPENDER_USUARIO
                        && String.valueOf(vendedorId).equals(registro.getEntidadId()))
                .anySatisfy(registro -> assertThat(registro.getIpOrigen()).isEqualTo("203.0.113.17"));
        assertThat(auditoriaService.verificarIntegridad().integra()).isTrue();
    }

    private String registrar(String email, String rol, String tienda) throws Exception {
        String tiendaJson = tienda == null ? "null" : "\"" + tienda + "\"";
        String body = """
                {"nombre":"Ana","apellido":"Ruiz","email":"%s","telefono":"3001234567",
                 "password":"Clave1234","confirmarPassword":"Clave1234","rol":"%s",
                 "nombreTienda":%s,"aceptaTerminos":true}
                """.formatted(email, rol, tiendaJson);
        String json = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }

    private int crearPrenda(String tokenVendedor) throws Exception {
        String json = mockMvc.perform(post("/api/vendedor/prendas").header("Authorization", tokenVendedor)
                        .contentType(MediaType.APPLICATION_JSON).content(PRENDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.estadoModeracion").value("SIN_REVISAR"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private Usuario crearAdmin(String email) {
        return usuarioRepository.save(Usuario.builder()
                .nombre("Admin")
                .apellido("US14")
                .email(email)
                .password("hash-no-utilizado")
                .rol(Rol.ADMINISTRADOR)
                .estado(EstadoUsuario.ACTIVO)
                .build());
    }

    private String agregarPerfil(String tokenCliente) throws Exception {
        return mockMvc.perform(post("/api/cliente/perfiles").header("Authorization", tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Sofía","fechaNacimiento":"2021-01-01","contextura":"MEDIA",
                                 "holgura":"REGULAR","alergias":[],"sinAlergias":true,
                                 "coloresPreferidos":[],"estampadosPreferidos":[]}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwtService.generarToken(usuario).token();
    }
}
