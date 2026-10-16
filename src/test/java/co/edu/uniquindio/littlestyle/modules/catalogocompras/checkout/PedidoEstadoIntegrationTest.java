package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-12 de punta a punta contra seguridad y H2: avance por rol, PAGADO manual
 * bloqueado, cancelación del cliente y reposición de stock al cancelar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PedidoEstadoIntegrationTest {

    private static final String PRENDA = """
            {"nombre":"Pantalón Jogger Azul","categoria":"PANTALONES","descripcion":"Cómodo","instruccionesCuidado":"Lavar a máquina","precio":32900,
             "colores":["AZUL"],"estampado":"LISO",
             "composicion":[{"material":"ALGODON","porcentaje":100}],
             "tallas":[{"talla":"4","estaturaMinCm":100,"estaturaMaxCm":110,"pesoMinKg":15,"pesoMaxKg":20,"stock":8,"stockMinimo":5}]}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository pedidoRepository;
    @Autowired
    private co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository prendaRepository;
    @Autowired
    private co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository usuarioRepository;
    @Autowired
    private co.edu.uniquindio.littlestyle.config.security.JwtService jwtService;

    private String tokenAdmin(String email) {
        var administrador = usuarioRepository.save(
                co.edu.uniquindio.littlestyle.modules.auth.model.Usuario.builder()
                        .nombre("Admin").apellido("Pruebas").email(email)
                        .password("hash-no-utilizado")
                        .rol(co.edu.uniquindio.littlestyle.modules.auth.model.Rol.ADMINISTRADOR)
                        .estado(co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario.ACTIVO)
                        .build());
        return "Bearer " + jwtService.generarToken(administrador).token();
    }

    private String registrar(String email, String rol) throws Exception {
        String tienda = "VENDEDOR".equals(rol) ? "\"Tienda Sol\"" : "null";
        String body = """
                {"nombre":"Ana","apellido":"Ruiz","email":"%s","telefono":"3001234567","password":"Clave1234",
                 "confirmarPassword":"Clave1234","rol":"%s","nombreTienda":%s,"aceptaTerminos":true}
                """.formatted(email, rol, tienda);
        String json = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }

    private int crearPrenda(String token) throws Exception {
        String json = mockMvc.perform(post("/api/vendedor/prendas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(PRENDA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private long crearPedido(String clienteEmail, int prendaId,
                             co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido estado) {
        var cliente = usuarioRepository.findByEmail(clienteEmail).orElseThrow();
        var pedido = co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido.builder()
                .cliente(cliente).perfilInfantilId(1L).estado(estado)
                .destinatario("Ana").direccion("Calle 1").ciudad("Armenia").telefono("3001234567")
                .subtotal(new BigDecimal("65800")).costoEnvio(new BigDecimal("7000"))
                .total(new BigDecimal("72800"))
                .fechaCreacion(LocalDateTime.now()).fechaActualizacion(LocalDateTime.now()).build();
        pedido.agregar(co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido.builder()
                .prendaId((long) prendaId).nombre("Pantalón Jogger Azul").talla("4").cantidad(2)
                .precioUnitario(new BigDecimal("32900")).subtotal(new BigDecimal("65800")).build());
        return pedidoRepository.saveAndFlush(pedido).getId();
    }

    @Test
    void adminAvanzaHastaEnviadoConGuiaYTerminalNoReabre() throws Exception {
        String admin = tokenAdmin("admin-est-" + System.nanoTime() + "@correo.com");
        String vendedor = registrar("vendedor-est-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String clienteEmail = "cliente-est-" + System.nanoTime() + "@correo.com";
        registrar(clienteEmail, "CLIENTE");
        int prendaId = crearPrenda(vendedor);
        long id = crearPedido(clienteEmail, prendaId,
                co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.REGISTRADO);

        mockMvc.perform(put("/api/admin/pedidos/{id}/estado", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"EN_PREPARACION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PREPARACION"));
        mockMvc.perform(put("/api/admin/pedidos/{id}/estado", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"ENVIADO\",\"numeroGuia\":\"COL123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroGuia").value("COL123"));
        mockMvc.perform(put("/api/admin/pedidos/{id}/estado", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"CANCELADO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void marcarPagadoAManoDevuelve409() throws Exception {
        String admin = tokenAdmin("admin-pag-" + System.nanoTime() + "@correo.com");
        String vendedor = registrar("vendedor-pag-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String clienteEmail = "cliente-pag-" + System.nanoTime() + "@correo.com";
        String cliente = registrar(clienteEmail, "CLIENTE");
        int prendaId = crearPrenda(vendedor);
        long id = crearPedido(clienteEmail, prendaId,
                co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.PENDIENTE_PAGO);

        mockMvc.perform(put("/api/admin/pedidos/{id}/estado", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"PAGADO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void vendedorAvanzaLoSuyoAOtroLeDa404YSinCancelar() throws Exception {
        String vendedor = registrar("vendedor-av-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String otro = registrar("vendedor-av2-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String clienteEmail = "cliente-av-" + System.nanoTime() + "@correo.com";
        registrar(clienteEmail, "CLIENTE");
        int prendaId = crearPrenda(vendedor);
        long id = crearPedido(clienteEmail, prendaId,
                co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.REGISTRADO);

        mockMvc.perform(put("/api/vendedor/pedidos/{id}/estado", id).header("Authorization", otro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"EN_PREPARACION\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/vendedor/pedidos/{id}/estado", id).header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"EN_PREPARACION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PREPARACION"));
        mockMvc.perform(put("/api/vendedor/pedidos/{id}/estado", id).header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"CANCELADO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void clienteCancelaAntesDelPagoYDespuesNo() throws Exception {
        String vendedor = registrar("vendedor-cc-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String clienteEmail = "cliente-cc-" + System.nanoTime() + "@correo.com";
        String cliente = registrar(clienteEmail, "CLIENTE");
        int prendaId = crearPrenda(vendedor);
        long pendiente = crearPedido(clienteEmail, prendaId,
                co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.PENDIENTE_PAGO);
        long registrado = crearPedido(clienteEmail, prendaId,
                co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.REGISTRADO);

        mockMvc.perform(post("/api/cliente/checkout/pedidos/{id}/cancelar", pendiente)
                        .header("Authorization", cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
        mockMvc.perform(post("/api/cliente/checkout/pedidos/{id}/cancelar", registrado)
                        .header("Authorization", cliente))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelarConPagoPrevioReponeStock() throws Exception {
        String admin = tokenAdmin("admin-st-" + System.nanoTime() + "@correo.com");
        String vendedor = registrar("vendedor-st-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String clienteEmail = "cliente-st-" + System.nanoTime() + "@correo.com";
        registrar(clienteEmail, "CLIENTE");
        int prendaId = crearPrenda(vendedor);
        long id = crearPedido(clienteEmail, prendaId,
                co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.REGISTRADO);

        mockMvc.perform(put("/api/admin/pedidos/{id}/estado", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"CANCELADO\"}"))
                .andExpect(status().isOk());

        var prenda = prendaRepository.findById((long) prendaId).orElseThrow();
        int stock = prenda.getTallas().stream().filter(t -> t.getTalla().equals("4"))
                .mapToInt(t -> t.getStock()).sum();
        assertThat(stock).isEqualTo(10);
    }
}
