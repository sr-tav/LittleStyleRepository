package co.edu.uniquindio.littlestyle.modules.dashboard;

import co.edu.uniquindio.littlestyle.config.security.JwtService;
import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.modules.dashboard.model.Busqueda;
import co.edu.uniquindio.littlestyle.modules.dashboard.model.ProductoVista;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.BusquedaRepository;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.ProductoVistaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-15 de punta a punta contra seguridad (JWT real), JPQL y H2: cada vendedor ve solo su negocio (RNF-15),
 * el administrador ve todo, la categoría filtra de verdad y sin datos el dashboard muestra ceros, no demo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardIntegrationTest {

    private static final String OVERVIEW = "/api/v1/dashboard/overview";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PrendaRepository prendaRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ProductoVistaRepository productoVistaRepository;
    @Autowired private BusquedaRepository busquedaRepository;

    private Usuario vendedorA;
    private Usuario vendedorB;
    private Usuario vendedorSinVentas;
    private Usuario admin;
    private Usuario cliente;

    /**
     * Escenario: A vende un vestido (stock bajo) y un buzo; B vende un vestido. El pedido 1 mezcla prendas de A
     * y B, el 2 está cancelado y el 3 ya se envió.
     */
    @BeforeEach
    void escenario() {
        vendedorA = usuario("a@tienda.com", Rol.VENDEDOR);
        vendedorB = usuario("b@tienda.com", Rol.VENDEDOR);
        vendedorSinVentas = usuario("c@tienda.com", Rol.VENDEDOR);
        admin = usuario("admin@littlestyle.com", Rol.ADMINISTRADOR);
        cliente = usuario("cliente@mail.com", Rol.CLIENTE);

        Prenda vestidoA = prenda(vendedorA, "Vestido Rosa", CategoriaPrenda.VESTIDOS, 3, 5);
        Prenda buzoA = prenda(vendedorA, "Buzo Azul", CategoriaPrenda.BUZOS, 10, 2);
        Prenda vestidoB = prenda(vendedorB, "Vestido Verde", CategoriaPrenda.VESTIDOS, 7, 5);

        pedido(EstadoPedido.REGISTRADO, linea(vestidoA, 2, "60000"), linea(vestidoB, 1, "20000"));
        pedido(EstadoPedido.CANCELADO, linea(vestidoA, 5, "100000"));
        pedido(EstadoPedido.ENVIADO, linea(buzoA, 1, "40000"));

        LocalDateTime ahora = LocalDateTime.now();
        for (int i = 0; i < 3; i++) {
            productoVistaRepository.save(ProductoVista.builder().prendaId(vestidoA.getId()).fecha(ahora).build());
        }
        for (int i = 0; i < 2; i++) {
            productoVistaRepository.save(ProductoVista.builder().prendaId(vestidoB.getId()).fecha(ahora).build());
        }
        busqueda("vestido rosa", ahora);
        busqueda("vestido rosa", ahora);
        busqueda("buzo", ahora);
    }

    @Test
    void vendedorSoloVeSuNegocioYNoLosPedidosCancelados() throws Exception {
        String json = consultar(vendedorA, "");

        assertThat(numero(json, "$.totalVentas")).isEqualTo(100000d);   // 60000 + 40000; el cancelado no cuenta
        assertThat((int) JsonPath.read(json, "$.totalPedidos")).isEqualTo(2);
        assertThat(numero(json, "$.productosVendidos")).isEqualTo(3d);
        assertThat(numero(json, "$.ticketPromedio")).isEqualTo(50000d);
        assertThat(numero(json, "$.stockTotal")).isEqualTo(13d);
        assertThat(numero(json, "$.stockBajo")).isEqualTo(1d);            // solo el vestido (3 <= 5)
        assertThat(JsonPath.<Object>read(json, "$.margenBruto")).isNull(); // sin costos no hay margen
        assertThat(JsonPath.<List<String>>read(json, "$.ventasPorCategoria[*].label"))
                .containsExactly("VESTIDOS", "BUZOS");
        assertThat(JsonPath.<List<String>>read(json, "$.productosPopulares[*].label"))
                .containsExactly("Vestido Rosa", "Buzo Azul")
                .doesNotContain("Vestido Verde");
    }

    @Test
    void otroVendedorNoVeLosDatosDelPrimero() throws Exception {
        String json = consultar(vendedorB, "");

        assertThat(numero(json, "$.totalVentas")).isEqualTo(20000d);
        assertThat((int) JsonPath.read(json, "$.totalPedidos")).isEqualTo(1);
        assertThat(numero(json, "$.stockTotal")).isEqualTo(7d);
        assertThat(numero(json, "$.stockBajo")).isEqualTo(0d);
        assertThat(JsonPath.<List<String>>read(json, "$.productosPopulares[*].label"))
                .containsExactly("Vestido Verde");
    }

    @Test
    void administradorVeLaPlataformaCompleta() throws Exception {
        String json = consultar(admin, "");

        assertThat(numero(json, "$.totalVentas")).isEqualTo(120000d);
        assertThat((int) JsonPath.read(json, "$.totalPedidos")).isEqualTo(2);
        assertThat(numero(json, "$.stockTotal")).isEqualTo(20d);
    }

    @Test
    void filtroDeCategoriaRestringeVentasInventarioYSeries() throws Exception {
        String json = consultar(vendedorA, "?categoria=BUZOS");

        assertThat(numero(json, "$.totalVentas")).isEqualTo(40000d);
        assertThat((int) JsonPath.read(json, "$.totalPedidos")).isEqualTo(1);
        assertThat(numero(json, "$.stockTotal")).isEqualTo(10d);
        assertThat(JsonPath.<List<String>>read(json, "$.ventasPorCategoria[*].label")).containsExactly("BUZOS");
    }

    @Test
    void categoriaInvalidaDevuelve400() throws Exception {
        mockMvc.perform(get(OVERVIEW + "?categoria=NoExiste").header("Authorization", bearer(vendedorA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productosMasConsultadosSalenDeLasVistasNoDeLasVentas() throws Exception {
        String json = consultar(vendedorA, "");

        // El buzo se vendió pero nadie vio su ficha: no debe aparecer como "consultado".
        assertThat(JsonPath.<List<String>>read(json, "$.productosMasConsultados[*].label"))
                .containsExactly("Vestido Rosa");
        assertThat((int) JsonPath.read(json, "$.productosMasConsultados[0].cantidad")).isEqualTo(3);
    }

    @Test
    void palabrasMasBuscadasSeAgrupanYOrdenanPorFrecuencia() throws Exception {
        String json = consultar(vendedorA, "");

        assertThat(JsonPath.<List<String>>read(json, "$.busquedasMasFrecuentes[*].label"))
                .containsExactly("vestido rosa", "buzo");
        assertThat((int) JsonPath.read(json, "$.busquedasMasFrecuentes[0].cantidad")).isEqualTo(2);
    }

    @Test
    void sinActividadDevuelveCerosYNoDatosDemo() throws Exception {
        String json = consultar(vendedorSinVentas, "");

        assertThat(numero(json, "$.totalVentas")).isZero();
        assertThat((int) JsonPath.read(json, "$.totalPedidos")).isZero();
        assertThat(numero(json, "$.stockTotal")).isZero();
        assertThat(JsonPath.<List<Object>>read(json, "$.ventasPorCategoria")).isEmpty();
        assertThat(JsonPath.<List<Object>>read(json, "$.productosPopulares")).isEmpty();
        assertThat(JsonPath.<List<Object>>read(json, "$.productosMasConsultados")).isEmpty();
    }

    @Test
    void topsPorSeparadoRespetanElAlcanceDelVendedor() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/top-sold-products").header("Authorization", bearer(vendedorB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].label").value("Vestido Verde"));
        mockMvc.perform(get("/api/v1/dashboard/top-viewed-products").header("Authorization", bearer(vendedorB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].label").value("Vestido Verde"))
                .andExpect(jsonPath("$[0].cantidad").value(2));
    }

    @Test
    void clienteNoPuedeVerElDashboard() throws Exception {
        mockMvc.perform(get(OVERVIEW).header("Authorization", bearer(cliente)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenNoHayAcceso() throws Exception {
        mockMvc.perform(get(OVERVIEW)).andExpect(status().isUnauthorized());
    }

    @Test
    void rangoInvertidoDevuelve400() throws Exception {
        mockMvc.perform(get(OVERVIEW + "?startDate=2026-10-31&endDate=2026-10-01")
                        .header("Authorization", bearer(vendedorA)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers ----

    private String consultar(Usuario usuario, String query) throws Exception {
        return mockMvc.perform(get(OVERVIEW + query).header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.generarToken(usuario).token();
    }

    private static double numero(String json, String path) {
        return ((Number) JsonPath.read(json, path)).doubleValue();
    }

    private Usuario usuario(String email, Rol rol) {
        return usuarioRepository.save(Usuario.builder()
                .nombre("Nombre").apellido("Apellido").email(email)
                .password("hash-no-utilizado").rol(rol).estado(EstadoUsuario.ACTIVO)
                .nombreTienda(rol == Rol.VENDEDOR ? "Tienda " + email : null)
                .build());
    }

    private Prenda prenda(Usuario vendedor, String nombre, CategoriaPrenda categoria, int stock, int stockMinimo) {
        LocalDateTime ahora = LocalDateTime.now();
        Prenda prenda = Prenda.builder()
                .vendedor(vendedor).nombre(nombre).categoria(categoria)
                .precio(new BigDecimal("30000")).estado(EstadoPrenda.ACTIVA)
                .fechaCreacion(ahora).fechaActualizacion(ahora)
                .build();
        TallaPrenda talla = TallaPrenda.builder()
                .prenda(prenda).talla("4")
                .estaturaMinCm(new BigDecimal("100")).estaturaMaxCm(new BigDecimal("110"))
                .pesoMinKg(new BigDecimal("15")).pesoMaxKg(new BigDecimal("20"))
                .stock(stock).stockMinimo(stockMinimo)
                .build();
        prenda.getTallas().add(talla);
        return prendaRepository.saveAndFlush(prenda);
    }

    private LineaPedido linea(Prenda prenda, int cantidad, String subtotal) {
        return LineaPedido.builder()
                .prendaId(prenda.getId()).nombre(prenda.getNombre()).talla("4").cantidad(cantidad)
                .precioUnitario(new BigDecimal(subtotal).divide(BigDecimal.valueOf(cantidad)))
                .subtotal(new BigDecimal(subtotal))
                .build();
    }

    private void pedido(EstadoPedido estado, LineaPedido... lineas) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineaPedido l : lineas) {
            subtotal = subtotal.add(l.getSubtotal());
        }
        Pedido pedido = Pedido.builder()
                .cliente(cliente).perfilInfantilId(1L).estado(estado)
                .destinatario("Ana").direccion("Calle 1").ciudad("Armenia").telefono("3001234567")
                .subtotal(subtotal).costoEnvio(new BigDecimal("7000")).total(subtotal.add(new BigDecimal("7000")))
                .fechaCreacion(LocalDateTime.now()).fechaActualizacion(LocalDateTime.now())
                .build();
        for (LineaPedido l : lineas) {
            pedido.agregar(l);
        }
        pedidoRepository.saveAndFlush(pedido);
    }

    private void busqueda(String termino, LocalDateTime fecha) {
        busquedaRepository.save(Busqueda.builder().termino(termino).totalResultados(4).fecha(fecha).build());
    }
}
