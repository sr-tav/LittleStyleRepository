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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-12 multi-vendedor de punta a punta contra seguridad y H2: despachos por
 * vendedor, 403 ante paquete ajeno, estado derivado y cancelación del admin.
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
    private co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DespachoRepository despachoRepository;
    @Autowired
    private co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.DespachoService despachoService;
    @Autowired
    private co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository prendaRepository;
    @Autowired
    private co.edu.uniquindio.littlestyle.modules.dashboard.service.DashboardAnalyticsService dashboardAnalyticsService;
    @Autowired
    private co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository usuarioRepository;
    @Autowired
    private co.edu.uniquindio.littlestyle.config.security.JwtService jwtService;

    // Helper: crea un admin directamente en BD y genera su token JWT sin pasar por registro
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

    // Helper: registra un usuario por API real y devuelve su token Bearer
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

    // Helper: crea la prenda de prueba como vendedor y devuelve su id
    private int crearPrenda(String token) throws Exception {
        String json = mockMvc.perform(post("/api/vendedor/prendas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(PRENDA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    // Helper: inserta un pedido con una línea (vía repositorio) en el estado indicado;
    // si es REGISTRADO, crea los despachos como pasó el pago
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
        long id = pedidoRepository.saveAndFlush(pedido).getId();
        if (estado == co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.REGISTRADO) {
            despachoService.crearDespachosAlPagar(id);
        }
        return id;
    }

    // Helper: pedido con líneas de dos vendedores distintos para escenarios multi-vendedor
    private long crearPedidoDosVendedores(String clienteEmail, int prendaA, int prendaB) {
        var cliente = usuarioRepository.findByEmail(clienteEmail).orElseThrow();
        var pedido = co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido.builder()
                .cliente(cliente).perfilInfantilId(1L)
                .estado(co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.REGISTRADO)
                .destinatario("Ana").direccion("Calle 1").ciudad("Armenia").telefono("3001234567")
                .subtotal(new BigDecimal("65800")).costoEnvio(new BigDecimal("7000"))
                .total(new BigDecimal("72800"))
                .fechaCreacion(LocalDateTime.now()).fechaActualizacion(LocalDateTime.now()).build();
        pedido.agregar(co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido.builder()
                .prendaId((long) prendaA).nombre("Pantalón A").talla("4").cantidad(1)
                .precioUnitario(new BigDecimal("32900")).subtotal(new BigDecimal("32900")).build());
        pedido.agregar(co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido.builder()
                .prendaId((long) prendaB).nombre("Pantalón B").talla("4").cantidad(1)
                .precioUnitario(new BigDecimal("32900")).subtotal(new BigDecimal("32900")).build());
        long id = pedidoRepository.saveAndFlush(pedido).getId();
        despachoService.crearDespachosAlPagar(id);
        return id;
    }

    private long despachoDelPedido(long pedidoId) {
        return despachoRepository.findAllByPedidoIdOrderByIdAsc(pedidoId).getFirst().getId();
    }

    @Test
    void dashboardGlobalIncluyePedidosPagadosYEnCursoDeCualquierVendedor() throws Exception {
        String vendedor = registrar("vendedor-dashboard-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String clienteEmail = "cliente-dashboard-" + System.nanoTime() + "@correo.com";
        registrar(clienteEmail, "CLIENTE");
        int prendaId = crearPrenda(vendedor);
        crearPedido(clienteEmail, prendaId,
                co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.REGISTRADO);

        var resumen = dashboardAnalyticsService.obtenerResumen(
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), null, null);

        assertThat(resumen.totalVentas()).isGreaterThanOrEqualTo(new BigDecimal("65800"));
        assertThat(resumen.totalPedidos()).isGreaterThanOrEqualTo(1);
        assertThat(resumen.productosVendidos()).isGreaterThanOrEqualTo(2);
        assertThat(resumen.demandaDetalle()).anySatisfy(detalle -> {
            assertThat(detalle.prenda()).isEqualTo("Pantalón Jogger Azul");
            assertThat(detalle.talla()).isEqualTo("4");
            assertThat(detalle.unidades()).isEqualTo(2);
        });
    }

    @Test
    void adminCancelaRegistradoYTerminalNoReabre() throws Exception {
        String admin = tokenAdmin("admin-est-" + System.nanoTime() + "@correo.com");
        String vendedor = registrar("vendedor-est-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String clienteEmail = "cliente-est-" + System.nanoTime() + "@correo.com";
        registrar(clienteEmail, "CLIENTE");
        int prendaId = crearPrenda(vendedor);
        long id = crearPedido(clienteEmail, prendaId,
                co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.REGISTRADO);

        // Nadie edita el estado global a mano: el admin solo cancela.
        mockMvc.perform(put("/api/admin/pedidos/{id}/estado", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"EN_PREPARACION\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/admin/pedidos/{id}/estado", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"CANCELADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
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
        registrar(clienteEmail, "CLIENTE");
        int prendaId = crearPrenda(vendedor);
        long id = crearPedido(clienteEmail, prendaId,
                co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.PENDIENTE_PAGO);

        mockMvc.perform(put("/api/admin/pedidos/{id}/estado", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"PAGADO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void vendedorAvanzaLoSuyoAOtroLeDa403YSinCancelar() throws Exception {
        String vendedor = registrar("vendedor-av-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String otro = registrar("vendedor-av2-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String clienteEmail = "cliente-av-" + System.nanoTime() + "@correo.com";
        registrar(clienteEmail, "CLIENTE");
        int prendaId = crearPrenda(vendedor);
        long pedidoId = crearPedido(clienteEmail, prendaId,
                co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido.REGISTRADO);
        long despachoId = despachoDelPedido(pedidoId);

        // El paquete existe pero es de otro: 403, no 404.
        mockMvc.perform(put("/api/vendedor/pedidos/{id}/estado", despachoId).header("Authorization", otro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"EN_PREPARACION\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/vendedor/pedidos/{id}/estado", despachoId).header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"EN_PREPARACION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PREPARACION"));
        mockMvc.perform(put("/api/vendedor/pedidos/{id}/estado", despachoId).header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"CANCELADO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void compraConDosVendedoresGeneraUnPedidoYDosDespachos() throws Exception {
        String vendedorA = registrar("vendedor-2a-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String vendedorB = registrar("vendedor-2b-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String clienteEmail = "cliente-2v-" + System.nanoTime() + "@correo.com";
        String cliente = registrar(clienteEmail, "CLIENTE");
        int prendaA = crearPrenda(vendedorA);
        int prendaB = crearPrenda(vendedorB);
        long pedidoId = crearPedidoDosVendedores(clienteEmail, prendaA, prendaB);

        var despachos = despachoRepository.findAllByPedidoIdOrderByIdAsc(pedidoId);
        assertThat(despachos).hasSize(2);
        assertThat(despachos.stream().map(d -> d.getVendedor().getId()).toList()).doesNotHaveDuplicates();

        // El cliente ve el estado global y un bloque por vendedor con su estado y guía.
        mockMvc.perform(get("/api/cliente/checkout/pedidos/{id}", pedidoId).header("Authorization", cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REGISTRADO"))
                .andExpect(jsonPath("$.despachos.length()").value(2))
                .andExpect(jsonPath("$.despachos[0].estado").value("REGISTRADO"));

        // Cada vendedor ve solo su paquete.
        mockMvc.perform(get("/api/vendedor/pedidos").header("Authorization", vendedorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pedidoId").value(pedidoId));
        mockMvc.perform(get("/api/vendedor/pedidos").header("Authorization", vendedorB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pedidoId").value(pedidoId));
    }

    @Test
    void marcarUnDespachoEnviadoDejaElPedidoParcialmenteEnviado() throws Exception {
        String vendedorA = registrar("vendedor-pa-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String vendedorB = registrar("vendedor-pb-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String clienteEmail = "cliente-pe-" + System.nanoTime() + "@correo.com";
        String cliente = registrar(clienteEmail, "CLIENTE");
        int prendaA = crearPrenda(vendedorA);
        int prendaB = crearPrenda(vendedorB);
        long pedidoId = crearPedidoDosVendedores(clienteEmail, prendaA, prendaB);
        List<Long> despachoIds = despachoRepository.findAllByPedidoIdOrderByIdAsc(pedidoId)
                .stream().map(d -> d.getId()).toList();

        // Vendedor A no puede modificar el despacho del vendedor B.
        mockMvc.perform(put("/api/vendedor/pedidos/{id}/estado", despachoIds.get(1))
                        .header("Authorization", vendedorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"EN_PREPARACION\"}"))
                .andExpect(status().isForbidden());

        // Avanza el primer despacho hasta ENVIADO con guía.
        var propioA = despachoRepository.findAllByPedidoIdOrderByIdAsc(pedidoId).getFirst().getId();
        String tokenA = vendedorA;
        mockMvc.perform(put("/api/vendedor/pedidos/{id}/estado", propioA).header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"EN_PREPARACION\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/vendedor/pedidos/{id}/estado", propioA).header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevo\":\"ENVIADO\",\"numeroGuia\":\"GUIA-A-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroGuia").value("GUIA-A-1"));

        // El pedido madre queda PARCIALMENTE_ENVIADO con la guía del paquete enviado.
        mockMvc.perform(get("/api/cliente/checkout/pedidos/{id}", pedidoId).header("Authorization", cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PARCIALMENTE_ENVIADO"));

        // No se puede cancelar un pedido ya parcialmente enviado.
        String admin = tokenAdmin("admin-parc-" + System.nanoTime() + "@correo.com");
        mockMvc.perform(put("/api/admin/pedidos/{id}/estado", pedidoId).header("Authorization", admin)
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
