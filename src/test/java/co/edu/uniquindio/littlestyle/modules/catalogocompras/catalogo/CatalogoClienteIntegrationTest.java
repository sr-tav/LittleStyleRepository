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
            {"nombre":"Pantalón Jogger Azul","categoria":"PANTALONES","descripcion":"Cómodo","instruccionesCuidado":"Lavar a máquina en agua fría","precio":32900,
             "colores":["AZUL"],"estampado":"LISO",
             "composicion":[{"material":"ALGODON","porcentaje":80},{"material":"POLIESTER","porcentaje":20}],
             "tallas":[{"talla":"4","estaturaMinCm":100,"estaturaMaxCm":110,"pesoMinKg":15,"pesoMaxKg":20,"stock":8,"edadMinMeses":24,"edadMaxMeses":72,"stockMinimo":5},
                       {"talla":"6","estaturaMinCm":111,"estaturaMaxCm":122,"pesoMinKg":21,"pesoMaxKg":26,"stock":4,"edadMinMeses":72,"edadMaxMeses":144,"stockMinimo":3}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    // Helper: registra un usuario por API real y devuelve el token Bearer para las llamadas del test
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

    // Recorrido completo de la vitrina: solo publicadas, y cada filtro (material, talla,
    // edad, género, categoría) se valida contra base de datos, incluidos los casos inválidos
    @Test
    void exploraSoloPublicadasConFiltrosEnBaseDeDatos() throws Exception {
        String vendedor = registrar("vendedor-vit-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String cliente = registrar("cliente-vit-" + System.nanoTime() + "@correo.com", "CLIENTE");
        String id = String.valueOf(crearPrenda(vendedor));

        // Vitrina sin datos internos del vendedor
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nombre").value("Pantalón Jogger Azul"))
                .andExpect(jsonPath("$.contenido[0].precio").value(32900))
                .andExpect(jsonPath("$.contenido[0].disponible").value(true))
                .andExpect(jsonPath("$.contenido[0].stockMinimo").doesNotExist())
                .andExpect(jsonPath("$.contenido[0].stockTotal").doesNotExist());

        // Filtros acotados a la prenda creada: otras pruebas de integración no transaccionales
        // dejan prendas publicadas en la H2 compartida
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente)
                        .param("ids", id)
                        .param("material", "ALGODON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente)
                        .param("ids", id)
                        .param("material", "ALGODON,POLIESTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente)
                        .param("ids", id)
                        .param("material", "CUERO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente)
                        .param("ids", id)
                        .param("talla", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
        // Filtro por edad en meses sobre el rango declarado (nulo = coincide)
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente)
                        .param("ids", id)
                        .param("edadMeses", "36"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente)
                        .param("ids", id)
                        .param("edadMeses", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente)
                        .param("ids", id)
                        .param("edadMeses", "-1"))
                .andExpect(status().isBadRequest());
        // Sin género declarado la prenda aplica a todos
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente)
                        .param("ids", id)
                        .param("genero", "NINA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
        mockMvc.perform(get("/api/cliente/catalogo").header("Authorization", cliente)
                        .param("ids", id)
                        .param("categoria", "ROPA"))
                .andExpect(status().isBadRequest());
    }

    // La ficha del producto expone stock por talla y rangos de edad, pero jamás el stock mínimo
    // del vendedor; además, al ocultar la prenda esta deja de verse para el cliente
    @Test
    void detalleMuestraExistenciasPorTallaSinExponerElMinimoYOcultaInactivas() throws Exception {
        String vendedor = registrar("vendedor-det-" + System.nanoTime() + "@correo.com", "VENDEDOR");
        String cliente = registrar("cliente-det-" + System.nanoTime() + "@correo.com", "CLIENTE");
        int id = crearPrenda(vendedor);

        mockMvc.perform(get("/api/cliente/catalogo/{id}", id).header("Authorization", cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pantalón Jogger Azul"))
                .andExpect(jsonPath("$.tallas.length()").value(2))
                .andExpect(jsonPath("$.tallas[0].disponible").value(true))
                .andExpect(jsonPath("$.tallas[0].stockDisponible").value(8))
                .andExpect(jsonPath("$.tallas[0].edadMinMeses").value(24))
                .andExpect(jsonPath("$.tallas[0].edadMaxMeses").value(72))
                .andExpect(jsonPath("$.tallas[1].stockDisponible").value(4))
                .andExpect(jsonPath("$.stockMinimo").doesNotExist());

        // Al ocultar la prenda desaparece de la vitrina
        mockMvc.perform(patch("/api/vendedor/prendas/{id}/estado", id).header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVA\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/cliente/catalogo/{id}", id).header("Authorization", cliente))
                .andExpect(status().isNotFound());
    }

    // La vitrina es exclusiva del rol CLIENTE: un vendedor logueado recibe 403 y sin token 401
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
