package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vitrina del cliente (base de US-09) contra la cadena real de seguridad y H2:
 * solo prendas publicadas, filtros en base de datos y ficha sin datos internos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogoClienteIntegrationTest {

    private static final String PRENDA = """
            {"nombre":"Pantalón Jogger Azul","categoria":"PANTALONES","descripcion":"Cómodo","precio":32900,
             "stockMinimo":5,"colores":["AZUL"],"estampado":"LISO",
             "composicion":[{"material":"ALGODON","porcentaje":80},{"material":"POLIESTER","porcentaje":20}],
             "tallas":[{"talla":"4","estaturaMinCm":100,"estaturaMaxCm":110,"pesoMinKg":15,"pesoMaxKg":20,"stock":8},
                       {"talla":"6","estaturaMinCm":111,"estaturaMaxCm":122,"pesoMinKg":21,"pesoMaxKg":26,"stock":4}]}
            """;

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void exploraSoloPublicadasConFiltrosEnBaseDeDatos() throws Exception {
        String vendedor = registrar("vendedor-vit-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String cliente = registrar("cliente-vit-" + System.nanoTime() + "@correo.com", "CLIENTE");
        crearPrenda(vendedor);

        // Vitrina sin datos internos del vendedor
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nombre").value("Pantalón Jogger Azul"))
                .andExpect(jsonPath("$.contenido[0].precio").value(32900))
                .andExpect(jsonPath("$.contenido[0].disponible").value(true))
                .andExpect(jsonPath("$.contenido[0].stockMinimo").doesNotExist())
                .andExpect(jsonPath("$.contenido[0].stockTotal").doesNotExist());

        // Filtros por material y talla con existencias
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente)
                        .param("material", "ALGODON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente)
                        .param("material", "CUERO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente)
                        .param("talla", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
        // Sin género declarado la prenda aplica a todos
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente)
                        .param("genero", "NINA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente)
                        .param("categoria", "ROPA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detalleMuestraFichaSinExistenciasExactasYOcultaInactivas() throws Exception {
        String vendedor = registrar("vendedor-det-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String cliente = registrar("cliente-det-" + System.nanoTime() + "@correo.com", "CLIENTE");
        int id = crearPrenda(vendedor);

        mockMvc.perform(get("/api/cliente/catalogo/{id}", id).header("Authorization", cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pantalón Jogger Azul"))
                .andExpect(jsonPath("$.tallas.length()").value(2))
                .andExpect(jsonPath("$.tallas[0].disponible").value(true))
                .andExpect(jsonPath("$.tallas[0].stock").doesNotExist())
                .andExpect(jsonPath("$.stockMinimo").doesNotExist());

        // Al ocultar la prenda desaparece de la vitrina
        mockMvc.perform(patch("/api/vendedor/prendas/{id}/estado", id).header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVA\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/cliente/catalogo/{id}", id).header("Authorization", cliente))
                .andExpect(status().isNotFound());
    }

    @Test
    void vitrinaExigeRolCliente() throws Exception {
        String vendedor = registrar("vendedor-rol-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        crearPrenda(vendedor);

        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", vendedor))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/cliente/catalogo"))
                .andExpect(status().isUnauthorized());
    }
}
