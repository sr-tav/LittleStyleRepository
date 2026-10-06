package co.edu.uniquindio.littlestyle.modules.catalogocompras;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.PrendaRecomendable;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port.CatalogoRecomendacionPort;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo de US-08 contra la cadena real de seguridad, H2 y el almacenamiento local:
 * alta de prenda → fotos → stock → alertas → catálogo del motor de recomendaciones.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogoInventarioIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    private static final String PRENDA = """
            {"nombre":"Pantalón Jogger Azul","categoria":"PANTALONES","descripcion":"Cómodo","precio":32900,
             "stockMinimo":5,"colores":["AZUL"],"estampado":"LISO","contieneNiquel":true,
             "composicion":[{"material":"ALGODON","porcentaje":80},{"material":"POLIESTER","porcentaje":20}],
             "tallas":[{"talla":"4","estaturaMinCm":100,"estaturaMaxCm":110,"pesoMinKg":15,"pesoMaxKg":20,"stock":8},
                       {"talla":"6","estaturaMinCm":111,"estaturaMaxCm":122,"pesoMinKg":21,"pesoMaxKg":26,"stock":4}]}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CatalogoRecomendacionPort catalogoRecomendacion;

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
                .andExpect(jsonPath("$.stockTotal").value(12))
                .andExpect(jsonPath("$.nivelStock").value("DISPONIBLE"))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String stock(int talla4, int talla6) {
        return """
                {"tallas":[{"talla":"4","stock":%d},{"talla":"6","stock":%d}]}
                """.formatted(talla4, talla6);
    }

    @Test
    void altaFotosStockYAlertasDeReabastecimiento() throws Exception {
        String vendedor = registrar("vendedor-us08@correo.com", "VENDEDOR");
        int id = crearPrenda(vendedor);

        // Carga masiva de fotos con almacenamiento local, servidas públicamente en /media
        String imagenes = mockMvc.perform(multipart("/api/vendedor/prendas/{id}/imagenes", id)
                        .file(new MockMultipartFile("archivos", "frente.png", "image/png", PNG))
                        .file(new MockMultipartFile("archivos", "espalda.png", "image/png", PNG))
                        .header("Authorization", vendedor))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        String url = JsonPath.read(imagenes, "$[0].url");
        assertThat(url).startsWith("/media/prendas/" + id + "/").endsWith(".png");
        mockMvc.perform(get(url)).andExpect(status().isOk());

        // Stock total 3 ≤ mínimo 5 → alerta de stock bajo en la misma transacción
        mockMvc.perform(put("/api/vendedor/inventario/{id}", id).header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON).content(stock(2, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockTotal").value(3))
                .andExpect(jsonPath("$.nivel").value("STOCK_BAJO"));
        mockMvc.perform(get("/api/vendedor/inventario/alertas").header("Authorization", vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nivel").value("STOCK_BAJO"))
                .andExpect(jsonPath("$[0].diferencia").value(-2))
                .andExpect(jsonPath("$[0].imagenUrl").value(url));

        // Agotarse escala la alerta; reabastecer por encima del mínimo la resuelve
        mockMvc.perform(put("/api/vendedor/inventario/{id}", id).header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON).content(stock(0, 0)))
                .andExpect(jsonPath("$.nivel").value("AGOTADO"));
        mockMvc.perform(get("/api/vendedor/inventario/alertas").header("Authorization", vendedor))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nivel").value("AGOTADO"));

        mockMvc.perform(put("/api/vendedor/inventario/{id}", id).header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON).content(stock(10, 10)))
                .andExpect(jsonPath("$.nivel").value("DISPONIBLE"));
        mockMvc.perform(get("/api/vendedor/inventario/alertas").header("Authorization", vendedor))
                .andExpect(jsonPath("$.length()").value(0));

        Integer historial = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alertas_reabastecimiento WHERE prenda_id = ? AND fecha_resolucion IS NOT NULL",
                Integer.class, id);
        assertThat(historial).isEqualTo(2);
    }

    @Test
    void elMotorDeRecomendacionesLeeElCatalogoRealYSoloLasPrendasActivas() throws Exception {
        String vendedor = registrar("vendedor-reco@correo.com", "VENDEDOR");
        int id = crearPrenda(vendedor);

        PrendaRecomendable prenda = catalogoRecomendacion.buscarPrendaPublicada((long) id).orElseThrow();
        assertThat(prenda.marca()).isEqualTo("Tienda Sol");
        assertThat(prenda.contieneNiquel()).isTrue();
        assertThat(prenda.stockPorTalla()).containsEntry("4", 8).containsEntry("6", 4);

        mockMvc.perform(patch("/api/vendedor/prendas/{id}/estado", id).header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVA\"}"))
                .andExpect(status().isOk());
        assertThat(catalogoRecomendacion.buscarPrendaPublicada((long) id)).isEmpty();
    }

    @Test
    void cadaVendedorSoloAccedeASusPrendasYElClienteNoAccede() throws Exception {
        String vendedorA = registrar("vendedor-a@correo.com", "VENDEDOR");
        String vendedorB = registrar("vendedor-b@correo.com", "VENDEDOR");
        String cliente = registrar("cliente-us08@correo.com", "CLIENTE");
        int id = crearPrenda(vendedorA);

        mockMvc.perform(get("/api/vendedor/prendas/{id}", id).header("Authorization", vendedorB))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/vendedor/inventario/{id}", id).header("Authorization", vendedorB)
                        .contentType(MediaType.APPLICATION_JSON).content(stock(0, 0)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/vendedor/prendas").header("Authorization", vendedorB))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/vendedor/prendas").header("Authorization", cliente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/vendedor/inventario"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validaLaPrendaYLasImagenes() throws Exception {
        String vendedor = registrar("vendedor-validacion@correo.com", "VENDEDOR");

        mockMvc.perform(post("/api/vendedor/prendas").header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRENDA.replace("\"porcentaje\":20", "\"porcentaje\":10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.composicion").exists());
        mockMvc.perform(post("/api/vendedor/prendas").header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\",\"tallas\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.precio").exists())
                .andExpect(jsonPath("$.errores.tallas").exists());

        int id = crearPrenda(vendedor);
        mockMvc.perform(multipart("/api/vendedor/prendas/{id}/imagenes", id)
                        .file(new MockMultipartFile("archivos", "foto.png", "image/png", "<script>".getBytes()))
                        .header("Authorization", vendedor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El archivo foto.png no es una imagen JPG, PNG o WEBP"));
    }
}
