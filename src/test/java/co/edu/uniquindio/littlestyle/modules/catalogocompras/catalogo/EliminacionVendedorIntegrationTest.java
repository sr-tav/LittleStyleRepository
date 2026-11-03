package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.prendaMultipart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Al eliminar su cuenta, el vendedor pierde su catálogo (prendas, tallas, imágenes). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EliminacionVendedorIntegrationTest {

    private static final String PRENDA = """
            {"nombre":"Pantalón Jogger Azul","categoria":"PANTALONES","descripcion":"Cómodo","instruccionesCuidado":"Lavar a máquina","precio":32900,
             "colores":["AZUL"],"estampado":"LISO",
             "composicion":[{"material":"ALGODON","porcentaje":100}],
             "tallas":[{"talla":"4","estaturaMinCm":100,"estaturaMaxCm":110,"pesoMinKg":15,"pesoMaxKg":20,"stock":8}]}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private PrendaRepository prendaRepository;

    private String registrarVendedor() throws Exception {
        String body = """
                {"nombre":"Ana","apellido":"Ruiz","email":"vend-del-%d@correo.com","telefono":"3001234567","password":"Clave1234",
                 "confirmarPassword":"Clave1234","rol":"VENDEDOR","nombreTienda":"Tienda Sol","aceptaTerminos":true}
                """.formatted(System.nanoTime());
        String json = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }

    @Test
    void alEliminarSuCuentaDesaparecenSusPrendas() throws Exception {
        String vendedor = registrarVendedor();
        String json = mockMvc.perform(prendaMultipart(PRENDA).header("Authorization", vendedor))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long prendaId = ((Number) JsonPath.read(json, "$.id")).longValue();
        assertThat(prendaRepository.existsById(prendaId)).isTrue();

        mockMvc.perform(delete("/api/auth/me").header("Authorization", vendedor))
                .andExpect(status().isNoContent());

        assertThat(prendaRepository.existsById(prendaId)).isFalse();
    }
}