package co.edu.uniquindio.littlestyle.modules.catalogocompras;

import com.jayway.jsonpath.JsonPath;
import co.edu.uniquindio.littlestyle.config.security.JwtService;
import jakarta.persistence.EntityManager;
import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DireccionClienteRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoExpiracionService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.PedidoPagoService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service.ResultadoConfirmacionPago;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CarritoCheckoutIntegrationTest {

    private static final String PRENDA = """
            {"nombre":"Pantalón Jogger Azul","categoria":"PANTALONES","descripcion":"Cómodo","precio":32900,
             "stockMinimo":5,"colores":["AZUL"],"estampado":"LISO",
             "composicion":[{"material":"ALGODON","porcentaje":80},{"material":"POLIESTER","porcentaje":20}],
             "tallas":[{"talla":"4","estaturaMinCm":100,"estaturaMaxCm":110,"pesoMinKg":15,"pesoMaxKg":20,"stock":8}]}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PrendaRepository prendaRepository;
    @Autowired
    private DireccionClienteRepository direccionClienteRepository;
    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private PedidoExpiracionService pedidoExpiracionService;
    @Autowired
    private PedidoPagoService pedidoPagoService;
    @Autowired
    private EntityManager entityManager;

    @Test
    void protegeCarritoYNoPermiteModificarItemsDeOtroCliente() throws Exception {
        mockMvc.perform(get("/api/cliente/carrito"))
                .andExpect(status().isUnauthorized());

        String vendedor = registrar("carrito-vendedor-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        mockMvc.perform(get("/api/cliente/carrito").header("Authorization", vendedor))
                .andExpect(status().isForbidden());

        Usuario administrador = usuarioRepository.save(Usuario.builder()
                .nombre("Admin")
                .apellido("Pruebas")
                .email("carrito-admin-" + System.nanoTime() + "@correo.com")
                .password("hash-no-utilizado")
                .rol(Rol.ADMINISTRADOR)
                .estado(EstadoUsuario.ACTIVO)
                .build());
        String adminToken = "Bearer " + jwtService.generarToken(administrador).token();
        mockMvc.perform(get("/api/cliente/carrito").header("Authorization", adminToken))
                .andExpect(status().isForbidden());

        String clienteUno = registrar("carrito-cliente-uno-" + System.nanoTime() + "@correo.com", "CLIENTE");
        String clienteDos = registrar("carrito-cliente-dos-" + System.nanoTime() + "@correo.com", "CLIENTE");
        int prendaId = crearPrenda(vendedor);
        String perfil = agregarPerfil(clienteUno);
        int perfilId = JsonPath.read(perfil, "$.id");
        String carrito = mockMvc.perform(post("/api/cliente/carrito/items")
                        .header("Authorization", clienteUno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prendaId\":" + prendaId + ",\"talla\":\"4\",\"cantidad\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        int itemId = JsonPath.read(carrito, "$.items[0].id");

        mockMvc.perform(delete("/api/cliente/carrito/items/{itemId}", itemId)
                        .header("Authorization", clienteDos))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/cliente/carrito").header("Authorization", clienteDos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
        mockMvc.perform(get("/api/cliente/carrito").header("Authorization", clienteUno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(itemId));

        mockMvc.perform(post("/api/cliente/checkout/resumen")
                        .header("Authorization", clienteDos)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                        {"perfilInfantilId":%d,
                                         "direccion":{"destinatario":"Ana Ruiz","direccion":"Calle 1 # 2-3",
                                         "complemento":"Apto 302","codigoPostal":"630001",
                                         "departamento":"Quindío","municipio":"Armenia","telefono":"3001234567"}}
                                        """.formatted(perfilId))
                )
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/cliente/carrito/items/{itemId}", itemId)
                        .header("Authorization", clienteUno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
        mockMvc.perform(post("/api/cliente/carrito/items")
                        .header("Authorization", clienteUno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prendaId\":" + prendaId + ",\"talla\":\"4\",\"cantidad\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/cliente/carrito").header("Authorization", clienteUno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void calculaResumenConStockFisicoYSubtotalMasEnvio() throws Exception {
        String vendedor = registrar("checkout-vendedor-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String cliente = registrar("checkout-cliente-" + System.nanoTime() + "@correo.com", "CLIENTE");
        int prendaId = crearPrenda(vendedor);
        String perfil = agregarPerfil(cliente);
        int perfilId = JsonPath.read(perfil, "$.id");

        mockMvc.perform(post("/api/cliente/carrito/items")
                        .header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prendaId\":" + prendaId + ",\"talla\":\"4\",\"cantidad\":2}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cliente/checkout/resumen")
                        .header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                        {"perfilInfantilId":%d,
                                         "direccion":{"destinatario":"Ana Ruiz","direccion":"Calle 1 # 2-3",
                                         "codigoPostal":"630001","departamento":"Quindío",
                                         "municipio":"Armenia","telefono":"3001234567"}}
                                        """.formatted(perfilId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.subtotal").value(65800))
                        .andExpect(jsonPath("$.costoEnvio").value(7000))
                        .andExpect(jsonPath("$.total").value(72800));
    }

    @Test
    void otrosCarritosNoReservanStockYElCarritoReflejaAgotamientoFisico() throws Exception {
        String vendedor = registrar("stock-vendedor-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String clienteA = registrar("stock-cliente-a-" + System.nanoTime() + "@correo.com", "CLIENTE");
        String clienteB = registrar("stock-cliente-b-" + System.nanoTime() + "@correo.com", "CLIENTE");
        int prendaId = crearPrenda(vendedor);
        mockMvc.perform(post("/api/cliente/carrito/items")
                        .header("Authorization", clienteA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prendaId\":" + prendaId + ",\"talla\":\"4\",\"cantidad\":8}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/cliente/carrito/items")
                        .header("Authorization", clienteB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prendaId\":" + prendaId + ",\"talla\":\"4\",\"cantidad\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].disponible").value(true));
        assertThat(stockTalla(prendaId)).isEqualTo(8);

        Prenda prenda = prendaRepository.findByIdAndEstado((long) prendaId, EstadoPrenda.ACTIVA)
                .orElseThrow();
        prenda.buscarTalla("4").orElseThrow().setStock(0);
        prendaRepository.saveAndFlush(prenda);

        mockMvc.perform(get("/api/cliente/carrito").header("Authorization", clienteA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].disponible").value(false));
    }

    @Test
    void guardaDireccionPorClienteYNoLaExponeAOtraCuenta() throws Exception {
        String emailClienteUno = "direccion-cliente-uno-" + System.nanoTime() + "@correo.com";
        String clienteUno = registrar(emailClienteUno, "CLIENTE");
        String clienteDos = registrar("direccion-cliente-dos-" + System.nanoTime() + "@correo.com", "CLIENTE");
        String nuevaDireccion = """
                {"destinatario":"Ana Ruiz","direccion":"Calle 1 # 2-3","complemento":"Apto 302",
                 "codigoPostal":"630001","departamento":"Quindío",
                 "municipio":"Armenia","telefono":"3001234567"}
                """;

        mockMvc.perform(get("/api/cliente/checkout/direccion").header("Authorization", clienteUno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardada").value(false))
                .andExpect(jsonPath("$.direccion").value(nullValue()));
        mockMvc.perform(put("/api/cliente/checkout/direccion")
                        .header("Authorization", clienteUno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nuevaDireccion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardada").value(true))
                .andExpect(jsonPath("$.direccion.complemento").value("Apto 302"))
                .andExpect(jsonPath("$.direccion.codigoPostal").value("630001"))
                .andExpect(jsonPath("$.direccion.departamento").value("Quindío"))
                .andExpect(jsonPath("$.direccion.municipio").value("Armenia"));
        mockMvc.perform(put("/api/cliente/checkout/direccion")
                        .header("Authorization", clienteUno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nuevaDireccion.replace("630001", "63001")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/cliente/checkout/direccion").header("Authorization", clienteUno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.direccion.destinatario").value("Ana Ruiz"));
        mockMvc.perform(put("/api/cliente/checkout/direccion")
                        .header("Authorization", clienteUno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinatario":"Ana Ruiz","direccion":"Calle 1 # 2-3","complemento":"Piso 2",
                                 "codigoPostal":"630001","departamento":"Quindío",
                                 "municipio":"Armenia","telefono":"3001234567"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.direccion.complemento").value("Piso 2"));
        mockMvc.perform(get("/api/cliente/checkout/direccion").header("Authorization", clienteDos))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardada").value(false))
                .andExpect(jsonPath("$.direccion").value(nullValue()));

        Long clienteUnoId = usuarioRepository.findByEmail(emailClienteUno)
                .map(Usuario::getId).orElseThrow();
        assertThat(direccionClienteRepository.findByClienteId(clienteUnoId)
                .map(direccion -> direccion.getComplemento())).contains("Piso 2");
        Object direccionCifrada = entityManager.createNativeQuery(
                        "select direccion, codigo_postal, departamento from direcciones_cliente where cliente_id = :clienteId")
                .setParameter("clienteId", clienteUnoId)
                .getResultList().getFirst();
        Object[] camposCifrados = (Object[]) direccionCifrada;
        assertThat(camposCifrados[0].toString()).startsWith("v1:").doesNotContain("Calle 1");
        assertThat(camposCifrados[1].toString()).startsWith("v1:").doesNotContain("630001");
        assertThat(camposCifrados[2].toString()).startsWith("v1:").doesNotContain("Quindío");
        mockMvc.perform(delete("/api/auth/me").header("Authorization", clienteUno))
                .andExpect(status().isNoContent());
        assertThat(direccionClienteRepository.findByClienteId(clienteUnoId)).isEmpty();
    }

    @Test
    void creaPedidoPendienteSinDescontarStockYExpiraSinRestituirlo() throws Exception {
        String vendedor = registrar("pedido-vendedor-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String clienteUno = registrar("pedido-cliente-a-" + System.nanoTime() + "@correo.com", "CLIENTE");
        String clienteDos = registrar("pedido-cliente-b-" + System.nanoTime() + "@correo.com", "CLIENTE");
        int prendaId = crearPrenda(vendedor);
        int perfilUno = JsonPath.read(agregarPerfil(clienteUno), "$.id");

        mockMvc.perform(post("/api/cliente/carrito/items")
                        .header("Authorization", clienteUno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prendaId\":" + prendaId + ",\"talla\":\"4\",\"cantidad\":2}"))
                .andExpect(status().isOk());

        String pedidoJson = mockMvc.perform(post("/api/cliente/checkout/pedidos")
                        .header("Authorization", clienteUno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"perfilInfantilId":%d,
                                 "direccion":{"destinatario":"Ana Ruiz","direccion":"Calle 1 # 2-3",
                                 "complemento":"Apto 302","codigoPostal":"630001",
                                 "departamento":"Quindío","municipio":"Armenia","telefono":"3001234567"}}
                                """.formatted(perfilUno)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pedidoId").isNumber())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.total").value(72800))
                .andExpect(jsonPath("$.direccion.telefono").value("3001234567"))
                .andExpect(jsonPath("$.direccion.complemento").value("Apto 302"))
                .andExpect(jsonPath("$.direccion.codigoPostal").value("630001"))
                .andExpect(jsonPath("$.direccion.departamento").value("Quindío"))
                .andExpect(jsonPath("$.direccion.municipio").value("Armenia"))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/cliente/carrito").header("Authorization", clienteUno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
        assertThat(stockTalla(prendaId)).isEqualTo(8);
        mockMvc.perform(post("/api/cliente/carrito/items")
                        .header("Authorization", clienteDos)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prendaId\":" + prendaId + ",\"talla\":\"4\",\"cantidad\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].disponible").value(true));

        Number pedidoIdNumero = JsonPath.read(pedidoJson, "$.pedidoId");
        long pedidoId = pedidoIdNumero.longValue();
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        pedido.setFechaCreacion(pedido.getFechaCreacion().minusMinutes(31));
        pedidoRepository.saveAndFlush(pedido);

        assertThat(pedidoExpiracionService.cancelarPedidosVencidos()).isEqualTo(1);
        assertThat(pedidoRepository.findById(pedidoId).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.CANCELADO);
        assertThat(stockTalla(prendaId)).isEqualTo(8);
        assertThat(pedidoExpiracionService.cancelarPedidosVencidos()).isZero();
        assertThat(stockTalla(prendaId)).isEqualTo(8);
    }

    @Test
    void primerPagoTomaElStockYElSegundoPedidoSeCancelaPorFaltaDeExistencias() throws Exception {
        String vendedor = registrar("checkout-stock-vendedor-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String clienteUno = registrar("checkout-stock-cliente-a-" + System.nanoTime() + "@correo.com", "CLIENTE");
        String clienteDos = registrar("checkout-stock-cliente-b-" + System.nanoTime() + "@correo.com", "CLIENTE");
        int prendaId = crearPrenda(vendedor);
        int prendaDisponibleId = crearPrenda(vendedor);
        int perfilUno = JsonPath.read(agregarPerfil(clienteUno), "$.id");
        int perfilDos = JsonPath.read(agregarPerfil(clienteDos), "$.id");

        mockMvc.perform(post("/api/cliente/carrito/items")
                        .header("Authorization", clienteUno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prendaId\":" + prendaId + ",\"talla\":\"4\",\"cantidad\":8}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/cliente/carrito/items")
                        .header("Authorization", clienteDos)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prendaId\":" + prendaId + ",\"talla\":\"4\",\"cantidad\":8}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/cliente/carrito/items")
                        .header("Authorization", clienteDos)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prendaId\":" + prendaDisponibleId + ",\"talla\":\"4\",\"cantidad\":2}"))
                .andExpect(status().isOk());

        String checkoutUno = """
                {"perfilInfantilId":%d,
                 "direccion":{"destinatario":"Ana Ruiz","direccion":"Calle 1 # 2-3",
                 "complemento":"Apto 302","codigoPostal":"630001",
                 "departamento":"Quindío","municipio":"Armenia","telefono":"3001234567"}}
                """.formatted(perfilUno);
        String checkoutDos = checkoutUno.replace(
                "\"perfilInfantilId\":" + perfilUno, "\"perfilInfantilId\":" + perfilDos);

        String pedidoUnoJson = mockMvc.perform(post("/api/cliente/checkout/pedidos")
                        .header("Authorization", clienteUno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutUno))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String pedidoDosJson = mockMvc.perform(post("/api/cliente/checkout/pedidos")
                        .header("Authorization", clienteDos)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutDos))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(stockTalla(prendaId)).isEqualTo(8);

        long pedidoUnoId = ((Number) JsonPath.read(pedidoUnoJson, "$.pedidoId")).longValue();
        long pedidoDosId = ((Number) JsonPath.read(pedidoDosJson, "$.pedidoId")).longValue();
        ResultadoConfirmacionPago resultadoPrimero = pedidoPagoService.confirmarPago(pedidoUnoId);
        assertThat(resultadoPrimero.estado()).isEqualTo(ResultadoConfirmacionPago.Estado.CONFIRMADO);
        assertThat(resultadoPrimero.prendasAgotadas()).isEmpty();
        assertThat(stockTalla(prendaId)).isZero();
        ResultadoConfirmacionPago resultadoSegundo = pedidoPagoService.confirmarPago(pedidoDosId);
        assertThat(resultadoSegundo.estado()).isEqualTo(ResultadoConfirmacionPago.Estado.STOCK_INSUFICIENTE);
        assertThat(resultadoSegundo.prendasAgotadas()).singleElement().satisfies(prenda -> {
            assertThat(prenda.nombre()).isEqualTo("Pantalón Jogger Azul");
            assertThat(prenda.talla()).isEqualTo("4");
            assertThat(prenda.cantidadSolicitada()).isEqualTo(8);
            assertThat(prenda.stockDisponible()).isZero();
        });
        assertThat(pedidoRepository.findById(pedidoUnoId).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.PAGADO);
        assertThat(pedidoRepository.findById(pedidoDosId).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.CANCELADO);
        assertThat(stockTalla(prendaId)).isZero();
        assertThat(stockTalla(prendaDisponibleId)).isEqualTo(8);
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

    private int crearPrenda(String vendedor) throws Exception {
        String json = mockMvc.perform(post("/api/vendedor/prendas").header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON).content(PRENDA))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String agregarPerfil(String cliente) throws Exception {
        return mockMvc.perform(post("/api/cliente/perfiles").header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Sofía","fechaNacimiento":"2021-01-01","contextura":"MEDIA",
                                 "holgura":"REGULAR","alergias":[],"sinAlergias":true,
                                 "coloresPreferidos":[],"estampadosPreferidos":[]}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private int stockTalla(int prendaId) {
        return prendaRepository.findByIdAndEstado((long) prendaId, EstadoPrenda.ACTIVA)
                .orElseThrow().buscarTalla("4").orElseThrow().getStock();
    }
}
