package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.dto.TransaccionWompi;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model.EstadoTransaccionPago;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.repository.TransaccionPagoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.service.WompiClient;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.wompi.llave-publica=pub_test_integracion",
        "app.wompi.secreto-integridad=test_integrity_secreto",
        "app.wompi.secreto-eventos=test_events_secreto",
        // La prueba corre dentro de una transacción que otro hilo no vería; el modo asíncrono del
        // webhook se prueba en ReceptorEventosWompiTest.
        "app.pagos.eventos-asincronos=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PagoWompiIntegrationTest {

    private static final String SECRETO_EVENTOS = "test_events_secreto";
    private static final String PRENDA = """
            {"nombre":"Pantalón Jogger Azul","categoria":"PANTALONES","descripcion":"Cómodo","instruccionesCuidado":"Lavar a máquina en agua fría","precio":32900,
             "stockMinimo":1,"colores":["AZUL"],"estampado":"LISO",
             "composicion":[{"material":"ALGODON","porcentaje":100}],
             "tallas":[{"talla":"4","estaturaMinCm":100,"estaturaMaxCm":110,"pesoMinKg":15,"pesoMaxKg":20,"stock":8}]}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PrendaRepository prendaRepository;
    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private TransaccionPagoRepository transaccionPagoRepository;
    @MockitoBean
    private WompiClient wompiClient;

    @Test
    void flujoCompletoWebhookAprobadoMarcaPedidoPagadoYDescuentaStock() throws Exception {
        Compra compra = crearPedido(2);

        String inicio = mockMvc.perform(post("/api/cliente/pagos/pedidos/" + compra.pedidoId())
                        .header("Authorization", compra.cliente()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montoCentavos").value(7280000))
                .andExpect(jsonPath("$.moneda").value("COP"))
                .andExpect(jsonPath("$.llavePublica").value("pub_test_integracion"))
                .andReturn().getResponse().getContentAsString();
        String referencia = JsonPath.read(inicio, "$.referencia");
        String firmaEsperada = sha256(referencia + "7280000COPtest_integrity_secreto");
        assertThat((String) JsonPath.read(inicio, "$.firmaIntegridad")).isEqualTo(firmaEsperada);

        mockMvc.perform(post("/api/pagos/wompi/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evento("wompi-1", "APPROVED", referencia, 7280000L, SECRETO_EVENTOS)))
                .andExpect(status().isOk());

        assertThat(pedidoRepository.findById(compra.pedidoId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.PAGADO);
        assertThat(stockTalla(compra.prendaId())).isEqualTo(6);
        assertThat(transaccionPagoRepository.findByReferencia(referencia).orElseThrow().getEstado())
                .isEqualTo(EstadoTransaccionPago.APROBADA);

        // Un reenvío del mismo evento no vuelve a descontar stock
        mockMvc.perform(post("/api/pagos/wompi/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evento("wompi-1", "APPROVED", referencia, 7280000L, SECRETO_EVENTOS)))
                .andExpect(status().isOk());
        assertThat(stockTalla(compra.prendaId())).isEqualTo(6);

        mockMvc.perform(get("/api/cliente/pagos/" + referencia).header("Authorization", compra.cliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value("APROBADA"))
                .andExpect(jsonPath("$.estadoPedido").value("PAGADO"));
    }

    @Test
    void webhookConFirmaFalsaResponde401YNoCambiaElPedido() throws Exception {
        Compra compra = crearPedido(1);
        String referencia = iniciarPago(compra);

        mockMvc.perform(post("/api/pagos/wompi/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evento("wompi-2", "APPROVED", referencia, 3990000L, "secreto-del-atacante")))
                .andExpect(status().isUnauthorized());

        assertThat(pedidoRepository.findById(compra.pedidoId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.PENDIENTE_PAGO);
        assertThat(stockTalla(compra.prendaId())).isEqualTo(8);
    }

    @Test
    void webhookRechazaUnAvisoAutenticoReutilizadoFueraDeSuVigencia() throws Exception {
        Compra compra = crearPedido(1);
        String referencia = iniciarPago(compra);
        long haceOnceMinutos = Instant.now().minusSeconds(11 * 60).getEpochSecond();

        mockMvc.perform(post("/api/pagos/wompi/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evento("wompi-4", "APPROVED", referencia, 3990000L, SECRETO_EVENTOS,
                                haceOnceMinutos)))
                .andExpect(status().isUnauthorized());

        assertThat(pedidoRepository.findById(compra.pedidoId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.PENDIENTE_PAGO);
        assertThat(stockTalla(compra.prendaId())).isEqualTo(8);
    }

    @Test
    void webhookAceptaLaFechaDelAvisoEnFormatoIso() throws Exception {
        Compra compra = crearPedido(1);
        String referencia = iniciarPago(compra);
        String ahora = Instant.now().toString();
        String checksum = sha256("wompi-5APPROVED3990000" + ahora + SECRETO_EVENTOS);

        mockMvc.perform(post("/api/pagos/wompi/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"event":"transaction.updated",
                                 "data":{"transaction":{"id":"wompi-5","status":"APPROVED","reference":"%s",
                                   "amount_in_cents":3990000,"currency":"COP","payment_method_type":"CARD"}},
                                 "signature":{"properties":["transaction.id","transaction.status",
                                   "transaction.amount_in_cents"],"checksum":"%s"},
                                 "timestamp":"%s"}
                                """.formatted(referencia, checksum, ahora)))
                .andExpect(status().isOk());

        assertThat(pedidoRepository.findById(compra.pedidoId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.PAGADO);
    }

    @Test
    void verificacionDirectaConWompiConfirmaElPagoSinWebhook() throws Exception {
        Compra compra = crearPedido(1);
        String referencia = iniciarPago(compra);
        when(wompiClient.consultarTransaccion("wompi-3")).thenReturn(
                new TransaccionWompi("wompi-3", "APPROVED", referencia, 3990000L, "COP", "NEQUI"));

        mockMvc.perform(post("/api/cliente/pagos/verificar")
                        .header("Authorization", compra.cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transaccionId\":\"wompi-3\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value("APROBADA"))
                .andExpect(jsonPath("$.estadoPedido").value("PAGADO"))
                .andExpect(jsonPath("$.metodoPago").value("NEQUI"));
        assertThat(stockTalla(compra.prendaId())).isEqualTo(7);
    }

    @Test
    void pagosRequierenRolClienteYSonPrivadosPorCliente() throws Exception {
        Compra compra = crearPedido(1);
        mockMvc.perform(post("/api/cliente/pagos/pedidos/" + compra.pedidoId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/cliente/pagos/pedidos/" + compra.pedidoId())
                        .header("Authorization", compra.vendedor()))
                .andExpect(status().isForbidden());

        String otroCliente = registrar("pago-otro-" + System.nanoTime() + "@correo.com", "CLIENTE");
        mockMvc.perform(post("/api/cliente/pagos/pedidos/" + compra.pedidoId())
                        .header("Authorization", otroCliente))
                .andExpect(status().isNotFound());
    }

    private String iniciarPago(Compra compra) throws Exception {
        String inicio = mockMvc.perform(post("/api/cliente/pagos/pedidos/" + compra.pedidoId())
                        .header("Authorization", compra.cliente()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(inicio, "$.referencia");
    }

    private Compra crearPedido(int cantidad) throws Exception {
        String vendedor = registrar("pago-vendedor-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String cliente = registrar("pago-cliente-" + System.nanoTime() + "@correo.com", "CLIENTE");
        String prendaJson = mockMvc.perform(post("/api/vendedor/prendas").header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON).content(PRENDA))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int prendaId = JsonPath.read(prendaJson, "$.id");
        String perfilJson = mockMvc.perform(post("/api/cliente/perfiles").header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Sofía","fechaNacimiento":"2021-01-01","contextura":"MEDIA",
                                 "holgura":"REGULAR","alergias":[],"sinAlergias":true,
                                 "coloresPreferidos":[],"estampadosPreferidos":[]}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int perfilId = JsonPath.read(perfilJson, "$.id");

        mockMvc.perform(post("/api/cliente/carrito/items").header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prendaId\":" + prendaId + ",\"talla\":\"4\",\"cantidad\":" + cantidad + "}"))
                .andExpect(status().isOk());
        String pedidoJson = mockMvc.perform(post("/api/cliente/checkout/pedidos").header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"perfilInfantilId":%d,
                                 "direccion":{"destinatario":"Ana Ruiz","direccion":"Calle 1 # 2-3",
                                 "complemento":null,"codigoPostal":"630001",
                                 "departamento":"Quindío","municipio":"Armenia","telefono":"3001234567"}}
                                """.formatted(perfilId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long pedidoId = ((Number) JsonPath.read(pedidoJson, "$.pedidoId")).longValue();
        return new Compra(cliente, vendedor, pedidoId, prendaId);
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

    private int stockTalla(int prendaId) {
        return prendaRepository.findByIdAndEstado((long) prendaId, EstadoPrenda.ACTIVA)
                .orElseThrow().buscarTalla("4").orElseThrow().getStock();
    }

    private static String evento(String id, String estado, String referencia, long monto, String secreto)
            throws Exception {
        return evento(id, estado, referencia, monto, secreto, Instant.now().getEpochSecond());
    }

    private static String evento(String id, String estado, String referencia, long monto, String secreto,
                                 long timestamp) throws Exception {
        String checksum = sha256(id + estado + monto + timestamp + secreto);
        return """
                {"event":"transaction.updated",
                 "data":{"transaction":{"id":"%s","status":"%s","reference":"%s","amount_in_cents":%d,
                   "currency":"COP","payment_method_type":"CARD"}},
                 "environment":"test",
                 "signature":{"properties":["transaction.id","transaction.status","transaction.amount_in_cents"],
                   "checksum":"%s"},
                 "timestamp":%d,
                 "sent_at":"2026-10-13T15:00:00.000Z"}
                """.formatted(id, estado, referencia, monto, checksum, timestamp);
    }

    private static String sha256(String texto) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
    }

    private record Compra(String cliente, String vendedor, long pedidoId, int prendaId) {
    }
}
