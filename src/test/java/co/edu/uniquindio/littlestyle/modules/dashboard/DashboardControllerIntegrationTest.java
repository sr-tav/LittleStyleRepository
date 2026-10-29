package co.edu.uniquindio.littlestyle.modules.dashboard;

import com.jayway.jsonpath.JsonPath;
import co.edu.uniquindio.littlestyle.config.security.JwtService;
import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.DespachoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardControllerIntegrationTest {

    private static final String PANTALON = """
            {"nombre":"Pantalón Jogger Azul","categoria":"PANTALONES","descripcion":"Cómodo","instruccionesCuidado":"Lavar a máquina","precio":32900,
             "colores":["AZUL"],"estampado":"LISO",
             "composicion":[{"material":"ALGODON","porcentaje":100}],
             "tallas":[{"talla":"4","estaturaMinCm":100,"estaturaMaxCm":110,"pesoMinKg":15,"pesoMaxKg":20,"stock":8,"stockMinimo":5}]}
            """;
    private static final String VESTIDO = """
            {"nombre":"Vestido Floral","categoria":"VESTIDOS","descripcion":"Vestido cómodo","instruccionesCuidado":"Lavar a máquina","precio":32900,
             "colores":["ROSA"],"estampado":"FLORES",
             "composicion":[{"material":"ALGODON","porcentaje":100}],
             "tallas":[{"talla":"4","estaturaMinCm":100,"estaturaMaxCm":110,"pesoMinKg":15,"pesoMaxKg":20,"stock":8,"stockMinimo":5}]}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private DespachoService despachoService;
    @Autowired private JwtService jwtService;

    @Test
    void administradorVeElConsolidadoYVendedorSoloSusProductosConFiltroDeCategoria() throws Exception {
        String sufijo = Long.toString(System.nanoTime());
        String vendedorPantalones = registrar("vendedor-pantalon-" + sufijo + "@correo.com", "VENDEDOR");
        String vendedorVestidos = registrar("vendedor-vestido-" + sufijo + "@correo.com", "VENDEDOR");
        String clienteEmail = "cliente-dashboard-" + sufijo + "@correo.com";
        registrar(clienteEmail, "CLIENTE");
        String admin = tokenAdmin("admin-dashboard-" + sufijo + "@correo.com");

        long pantalonId = crearPrenda(vendedorPantalones, PANTALON);
        long vestidoId = crearPrenda(vendedorVestidos, VESTIDO);
        crearPedido(clienteEmail, pantalonId);
        crearPedido(clienteEmail, vestidoId);

        LocalDate desde = LocalDate.now().minusDays(1);
        LocalDate hasta = LocalDate.now().plusDays(1);
        String global = consultar(admin, desde, hasta, null);
        String vestidosGlobal = consultar(admin, desde, hasta, "VESTIDOS");
        String propio = consultar(vendedorPantalones, desde, hasta, null);
        String vestidosPropios = consultar(vendedorPantalones, desde, hasta, "VESTIDOS");

        assertThat(((Number) JsonPath.read(global, "$.totalVentas")).doubleValue()).isEqualTo(131600d);
        assertThat(((Number) JsonPath.read(global, "$.totalPedidos")).intValue()).isEqualTo(2);
        assertThat(((Number) JsonPath.read(global, "$.productosVendidos")).intValue()).isEqualTo(4);
        assertThat((java.util.List<?>) JsonPath.read(global, "$.ventasPorCategoria")).hasSize(2);
        assertThat((java.util.List<?>) JsonPath.read(global, "$.demandaDetalle")).hasSize(2);

        assertThat(((Number) JsonPath.read(vestidosGlobal, "$.totalVentas")).doubleValue()).isEqualTo(65800d);
        assertThat((java.util.List<?>) JsonPath.read(vestidosGlobal, "$.ventasPorCategoria")).hasSize(1);
        assertThat((String) JsonPath.read(vestidosGlobal, "$.ventasPorCategoria[0].label")).isEqualTo("VESTIDOS");
        assertThat((java.util.List<?>) JsonPath.read(vestidosGlobal, "$.demandaDetalle")).hasSize(1);
        assertThat((String) JsonPath.read(vestidosGlobal, "$.demandaDetalle[0].prenda")).isEqualTo("Vestido Floral");

        assertThat(((Number) JsonPath.read(propio, "$.totalVentas")).doubleValue()).isEqualTo(65800d);
        assertThat((java.util.List<?>) JsonPath.read(propio, "$.demandaDetalle")).hasSize(1);
        assertThat((String) JsonPath.read(propio, "$.demandaDetalle[0].prenda")).isEqualTo("Pantalón Jogger Azul");
        assertThat(((Number) JsonPath.read(vestidosPropios, "$.totalVentas")).doubleValue()).isZero();
        assertThat((java.util.List<?>) JsonPath.read(vestidosPropios, "$.demandaDetalle")).isEmpty();
    }

    private String consultar(String token, LocalDate desde, LocalDate hasta, String categoria) throws Exception {
        var solicitud = get("/api/v1/dashboard/overview")
                .header("Authorization", token)
                .param("startDate", desde.toString())
                .param("endDate", hasta.toString());
        if (categoria != null) {
            solicitud.param("categoria", categoria);
        }
        return mockMvc.perform(solicitud)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String registrar(String email, String rol) throws Exception {
        String tienda = "VENDEDOR".equals(rol) ? "\"Tienda " + email + "\"" : "null";
        String body = """
                {"nombre":"Ana","apellido":"Ruiz","email":"%s","telefono":"3001234567","password":"Clave1234",
                 "confirmarPassword":"Clave1234","rol":"%s","nombreTienda":%s,"aceptaTerminos":true}
                """.formatted(email, rol, tienda);
        String json = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }

    private String tokenAdmin(String email) {
        Usuario admin = usuarioRepository.save(Usuario.builder()
                .nombre("Admin")
                .apellido("Pruebas")
                .email(email)
                .password("hash-no-utilizado")
                .rol(Rol.ADMINISTRADOR)
                .estado(EstadoUsuario.ACTIVO)
                .build());
        return "Bearer " + jwtService.generarToken(admin).token();
    }

    private long crearPrenda(String token, String solicitud) throws Exception {
        String json = mockMvc.perform(post("/api/vendedor/prendas")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private void crearPedido(String clienteEmail, long prendaId) {
        Usuario cliente = usuarioRepository.findByEmail(clienteEmail).orElseThrow();
        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .perfilInfantilId(1L)
                .estado(EstadoPedido.REGISTRADO)
                .destinatario("Ana")
                .direccion("Calle 1")
                .ciudad("Armenia")
                .telefono("3001234567")
                .subtotal(new BigDecimal("65800"))
                .costoEnvio(new BigDecimal("7000"))
                .total(new BigDecimal("72800"))
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        pedido.agregar(LineaPedido.builder()
                .prendaId(prendaId)
                .nombre("Prenda de prueba")
                .talla("4")
                .cantidad(2)
                .precioUnitario(new BigDecimal("32900"))
                .subtotal(new BigDecimal("65800"))
                .build());
        Long pedidoId = pedidoRepository.saveAndFlush(pedido).getId();
        despachoService.crearDespachosAlPagar(pedidoId);
    }
}
