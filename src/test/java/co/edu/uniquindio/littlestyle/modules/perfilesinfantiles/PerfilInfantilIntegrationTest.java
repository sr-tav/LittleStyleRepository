package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles;

import com.jayway.jsonpath.JsonPath;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter.AlergiasConverter;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter.BigDecimalCifradoConverter;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter.FechaNacimientoConverter;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter.PerfilDatosCifradoMigracion;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter.StringCifradoConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

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
class PerfilInfantilIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PerfilDatosCifradoMigracion migracionCifrado;

    @Test
    void crudMedicionesYEliminacionEnCascada() throws Exception {
        String token = registrarCliente("perfil-crud@correo.com");
        String perfil = crearPerfil(token, "Nicolás");
        int id = JsonPath.read(perfil, "$.id");
        Map<String, Object> perfilPersistido = jdbcTemplate.queryForMap("""
                SELECT nombre, fecha_nacimiento, contextura, holgura, alergias_cifradas, sin_alergias
                FROM perfiles_infantiles WHERE id = ?
                """, id);
        org.assertj.core.api.Assertions.assertThat(perfilPersistido.get("NOMBRE").toString())
                .startsWith("v1:").isNotEqualTo("Nicolás");
        org.assertj.core.api.Assertions.assertThat(perfilPersistido.get("FECHA_NACIMIENTO").toString())
                .startsWith("v1:");
        org.assertj.core.api.Assertions.assertThat(perfilPersistido.get("CONTEXTURA").toString())
                .startsWith("v1:").isNotEqualTo("MEDIA");
        org.assertj.core.api.Assertions.assertThat(perfilPersistido.get("HOLGURA").toString())
                .startsWith("v1:").isNotEqualTo("REGULAR");
        org.assertj.core.api.Assertions.assertThat(perfilPersistido.get("ALERGIAS_CIFRADAS").toString())
                .startsWith("v1:");
        org.assertj.core.api.Assertions.assertThat(perfilPersistido.get("SIN_ALERGIAS").toString())
                .startsWith("v1:").isNotEqualTo("true");
        String colorCifrado = jdbcTemplate.queryForObject(
                "SELECT color FROM perfil_colores_preferidos WHERE perfil_id = ?", String.class, id);
        org.assertj.core.api.Assertions.assertThat(colorCifrado).startsWith("v1:").isNotEqualTo("AZUL");

        mockMvc.perform(get("/api/cliente/perfiles").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Nicolás"));
        mockMvc.perform(get("/api/cliente/perfiles/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porcentajeCompletitud").value(75))
                .andExpect(jsonPath("$.mesesRestantes").exists())
                .andExpect(jsonPath("$.mesesCalculada").doesNotExist());

        agregarMedicion(token, id, "2025-01-15", "108", "19");
        agregarMedicion(token, id, "2025-04-15", "110", "21");
        String fechaCifrada = jdbcTemplate.queryForObject(
                "SELECT fecha_medicion FROM mediciones_crecimiento WHERE perfil_id = ? ORDER BY id DESC LIMIT 1",
                String.class, id);
        Map<String, Object> medicionPersistida = jdbcTemplate.queryForMap("""
                SELECT fecha_medicion, estatura_cifrada, peso_cifrado
                FROM mediciones_crecimiento WHERE perfil_id = ? ORDER BY id DESC LIMIT 1
                """, id);
        org.assertj.core.api.Assertions.assertThat(fechaCifrada).startsWith("v1:")
                .isNotEqualTo("2025-04-15");
        org.assertj.core.api.Assertions.assertThat(medicionPersistida.get("ESTATURA_CIFRADA").toString())
                .startsWith("v1:").isNotEqualTo("110");
        org.assertj.core.api.Assertions.assertThat(medicionPersistida.get("PESO_CIFRADO").toString())
                .startsWith("v1:").isNotEqualTo("21");
        mockMvc.perform(get("/api/cliente/perfiles/{id}/mediciones", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fechaMedicion").value("2025-04-15"))
                .andExpect(jsonPath("$[1].fechaMedicion").value("2025-01-15"));
        mockMvc.perform(get("/api/cliente/perfiles/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ultimaMedicion.fechaMedicion").value("2025-04-15"))
                .andExpect(jsonPath("$.porcentajeCompletitud").value(100));

        mockMvc.perform(put("/api/cliente/perfiles/{id}", id)
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson("Nicolás actualizado")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nicolás actualizado"));

        mockMvc.perform(delete("/api/cliente/perfiles/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/cliente/perfiles/{id}/mediciones", id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void migraFilasLegadasYConservaLaLecturaDelPerfilYElHistorial() throws Exception {
        String token = registrarCliente("perfil-migracion@correo.com");
        int id = JsonPath.read(crearPerfil(token, "Valeria"), "$.id");
        int idSinAlergias = JsonPath.read(crearPerfil(token, "Sofía"), "$.id");
        agregarMedicion(token, id, "2025-04-15", "110", "21");
        String legacyNacimiento = legado(new FechaNacimientoConverter()
                .convertToDatabaseColumn(LocalDate.of(2020, 5, 10)));
        String legacyAlergias = legado(new AlergiasConverter()
                .convertToDatabaseColumn(java.util.Set.of(AlergiaTextil.OTRA)));
        String legacyAlergiaLibre = legado(new StringCifradoConverter()
                .convertToDatabaseColumn("Piel sensible"));
        String legacyColorLibre = legado(new StringCifradoConverter()
                .convertToDatabaseColumn("Turquesa"));
        String legacyEstampadoLibre = legado(new StringCifradoConverter()
                .convertToDatabaseColumn("Lunas azules"));
        String legacyAltura = legado(new BigDecimalCifradoConverter()
                .convertToDatabaseColumn(new java.math.BigDecimal("110")));
        String legacyPeso = legado(new BigDecimalCifradoConverter()
                .convertToDatabaseColumn(new java.math.BigDecimal("21")));
        jdbcTemplate.update("""
                UPDATE perfiles_infantiles
                SET nombre = ?, fecha_nacimiento = ?, contextura = ?, holgura = ?,
                    alergias_cifradas = ?, otra_alergia_cifrada = ?, sin_alergias = ?,
                    otro_color_cifrado = ?, otro_estampado_cifrado = ?
                WHERE id = ?
                """, "Valeria", legacyNacimiento, "MEDIA", "REGULAR", legacyAlergias,
                legacyAlergiaLibre, "false", legacyColorLibre, legacyEstampadoLibre, id);
        jdbcTemplate.update("UPDATE perfil_colores_preferidos SET color = 'AZUL' WHERE perfil_id = ?", id);
        jdbcTemplate.update("""
                UPDATE mediciones_crecimiento
                SET fecha_medicion = ?, estatura_cifrada = ?, peso_cifrado = ?
                WHERE perfil_id = ?
                """, "2025-04-15", legacyAltura, legacyPeso, id);
        jdbcTemplate.update("UPDATE perfiles_infantiles SET sin_alergias = '1' WHERE id = ?",
                idSinAlergias);

        migracionCifrado.migrarDatosAnteriores();

        Map<String, Object> guardado = jdbcTemplate.queryForMap("""
                SELECT nombre, fecha_nacimiento, contextura, holgura, alergias_cifradas, sin_alergias
                FROM perfiles_infantiles WHERE id = ?
                """, id);
        org.assertj.core.api.Assertions.assertThat(guardado.get("NOMBRE").toString()).startsWith("v1:");
        org.assertj.core.api.Assertions.assertThat(guardado.get("FECHA_NACIMIENTO").toString()).startsWith("v1:");
        org.assertj.core.api.Assertions.assertThat(guardado.get("CONTEXTURA").toString()).startsWith("v1:");
        org.assertj.core.api.Assertions.assertThat(guardado.get("HOLGURA").toString()).startsWith("v1:");
        org.assertj.core.api.Assertions.assertThat(guardado.get("ALERGIAS_CIFRADAS").toString()).startsWith("v1:");
        org.assertj.core.api.Assertions.assertThat(guardado.get("SIN_ALERGIAS").toString()).startsWith("v1:");
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT color FROM perfil_colores_preferidos WHERE perfil_id = ?", String.class, id))
                .startsWith("v1:");
        mockMvc.perform(get("/api/cliente/perfiles/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Valeria"))
                .andExpect(jsonPath("$.alergias[0]").value("OTRA"))
                .andExpect(jsonPath("$.otraAlergia").value("Piel sensible"))
                .andExpect(jsonPath("$.otroColor").value("Turquesa"))
                .andExpect(jsonPath("$.otroEstampado").value("Lunas azules"));
        mockMvc.perform(get("/api/cliente/perfiles/{id}/mediciones", id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fechaMedicion").value("2025-04-15"))
                .andExpect(jsonPath("$[0].estaturaCm").value(110))
                .andExpect(jsonPath("$[0].pesoKg").value(21));
        mockMvc.perform(get("/api/cliente/perfiles/{id}", idSinAlergias)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sinAlergias").value(true));
    }

    @Test
    void cifraPreferenciasTextilesYDescripcionesLibresEnLaPersistencia() throws Exception {
        String token = registrarCliente("perfil-cifrado@correo.com");
        String body = """
                {"nombre":"Camila","fechaNacimiento":"2020-05-10","contextura":"MEDIA",
                 "holgura":"REGULAR","alergias":["OTRA"],"otraAlergia":"Piel sensible",
                 "sinAlergias":false,"coloresPreferidos":["ROSA"],"estampadosPreferidos":["FLORES"],
                 "otroColor":"Turquesa","otroEstampado":"Lunas azules"}
                """;
        String respuesta = mockMvc.perform(post("/api/cliente/perfiles")
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(respuesta, "$.id");

        Map<String, Object> perfil = jdbcTemplate.queryForMap("""
                SELECT otra_alergia_cifrada, otro_color_cifrado, otro_estampado_cifrado
                FROM perfiles_infantiles WHERE id = ?
                """, id);
        org.assertj.core.api.Assertions.assertThat(perfil.get("OTRA_ALERGIA_CIFRADA").toString())
                .startsWith("v1:").isNotEqualTo("Piel sensible");
        org.assertj.core.api.Assertions.assertThat(perfil.get("OTRO_COLOR_CIFRADO").toString())
                .startsWith("v1:").isNotEqualTo("Turquesa");
        org.assertj.core.api.Assertions.assertThat(perfil.get("OTRO_ESTAMPADO_CIFRADO").toString())
                .startsWith("v1:").isNotEqualTo("Lunas azules");
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT estampado FROM perfil_estampados_preferidos WHERE perfil_id = ?",
                String.class, id)).startsWith("v1:").isNotEqualTo("FLORES");
        mockMvc.perform(get("/api/cliente/perfiles/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otraAlergia").value("Piel sensible"))
                .andExpect(jsonPath("$.otroColor").value("Turquesa"))
                .andExpect(jsonPath("$.otroEstampado").value("Lunas azules"))
                .andExpect(jsonPath("$.coloresPreferidos[0]").value("ROSA"))
                .andExpect(jsonPath("$.estampadosPreferidos[0]").value("FLORES"));
    }

    @Test
    void validaPerfilYMedicionYNoExponePerfilAjeno() throws Exception {
        String token = registrarCliente("perfil-propio@correo.com");
        String otroToken = registrarCliente("perfil-ajeno@correo.com");
        String perfil = crearPerfil(token, "Valeria");
        int id = JsonPath.read(perfil, "$.id");

        mockMvc.perform(get("/api/cliente/perfiles/{id}", id).header("Authorization", bearer(otroToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/cliente/perfiles/{id}", id)
                        .header("Authorization", bearer(otroToken)).contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJson("Perfil modificado")))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/cliente/perfiles/{id}", id)
                        .header("Authorization", bearer(otroToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/cliente/perfiles/{id}/mediciones", id)
                        .header("Authorization", bearer(otroToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaMedicion\":\"2025-01-01\",\"estaturaCm\":110,\"pesoKg\":20}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/cliente/perfiles/{id}", id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Valeria"));
        mockMvc.perform(post("/api/cliente/perfiles").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJsonConFecha("Menor", LocalDate.now().plusDays(1).toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fechaNacimiento").exists());
        mockMvc.perform(post("/api/cliente/perfiles").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJsonConFecha("Adulto", LocalDate.now().minusYears(18).toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fechaNacimiento").exists());
        mockMvc.perform(post("/api/cliente/perfiles/{id}/mediciones", id)
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaMedicion\":\"2019-01-01\",\"estaturaCm\":110,\"pesoKg\":20}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fechaMedicion").exists());
        mockMvc.perform(post("/api/cliente/perfiles").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Menor","fechaNacimiento":"2020-01-01","contextura":"MEDIA",
                                 "holgura":"REGULAR","alergias":["LANA"],"sinAlergias":true,
                                 "coloresPreferidos":[],"estampadosPreferidos":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.alergias").exists());
        mockMvc.perform(post("/api/cliente/perfiles").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJsonConAlergia("\"OTRA\"", "null", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.otraAlergia").exists());
        mockMvc.perform(post("/api/cliente/perfiles").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(perfilJsonConAlergia("\"LANA\"", "null", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.alergias").exists());
        mockMvc.perform(post("/api/cliente/perfiles").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Menor","fechaNacimiento":"2020-01-01","contextura":"MEDIA",
                                 "holgura":"REGULAR","alergias":[],"sinAlergias":true,
                                 "coloresPreferidos":["TURQUESA"],"estampadosPreferidos":[]}
                                """))
                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.errores.coloresPreferidos").value("Seleccione un color válido"));
    }

    @Test
    void seguridadRequiereTokenYRechazaRolesDistintosDeCliente() throws Exception {
        mockMvc.perform(get("/api/cliente/perfiles"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/cliente/perfiles")
                        .with(SecurityMockMvcRequestPostProcessors.user("vendedor").roles("VENDEDOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/cliente/perfiles")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void normalizaYAceptaOpcionesPersonalizadasQueCoincidenConUnEnum() throws Exception {
        String token = registrarCliente("perfil-opciones@correo.com");

        mockMvc.perform(post("/api/cliente/perfiles").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Menor","fechaNacimiento":"2020-01-01","contextura":"MEDIA",
                                 "holgura":"REGULAR","alergias":["OTRA"],"otraAlergia":"  Piel sensible  ",
                                 "sinAlergias":false,"coloresPreferidos":[],"estampadosPreferidos":[],
                                 "otroColor":" café ","otroEstampado":"dibujos animados"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.otraAlergia").value("Piel sensible"))
                .andExpect(jsonPath("$.alergias[0]").value("OTRA"))
                .andExpect(jsonPath("$.coloresPreferidos[0]").value("CAFE"))
                .andExpect(jsonPath("$.estampadosPreferidos[0]").value("DIBUJOS_ANIMADOS"))
                .andExpect(jsonPath("$.otroColor").value(nullValue()))
                .andExpect(jsonPath("$.otroEstampado").value(nullValue()));
    }

    private String registrarCliente(String email) throws Exception {
        String body = """
                {"nombre":"Laura","apellido":"Gómez","email":"%s","telefono":"3001234567",
                 "password":"Clave1234","confirmarPassword":"Clave1234","rol":"CLIENTE",
                 "aceptaTerminos":true}
                """.formatted(email);
        String json = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private String crearPerfil(String token, String nombre) throws Exception {
        return mockMvc.perform(post("/api/cliente/perfiles").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(perfilJson(nombre)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private void agregarMedicion(String token, int perfilId, String fecha, String estatura, String peso)
            throws Exception {
        String body = """
                {"fechaMedicion":"%s","estaturaCm":%s,"pesoKg":%s}
                """.formatted(fecha, estatura, peso);
        mockMvc.perform(post("/api/cliente/perfiles/{id}/mediciones", perfilId)
                        .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private String perfilJson(String nombre) {
        return perfilJsonConFecha(nombre, "2020-05-10");
    }

    private String perfilJsonConFecha(String nombre, String fechaNacimiento) {
        return """
                {"nombre":"%s","fechaNacimiento":"%s","contextura":"MEDIA","holgura":"REGULAR",
                 "alergias":[],"sinAlergias":true,"coloresPreferidos":["AZUL"],"estampadosPreferidos":[]}
                """.formatted(nombre, fechaNacimiento);
    }

    private String perfilJsonConAlergia(String alergia, String otraAlergia, boolean sinAlergias) {
        return """
                {"nombre":"Menor","fechaNacimiento":"2020-01-01","contextura":"MEDIA",
                 "holgura":"REGULAR","alergias":[%s],"otraAlergia":%s,"sinAlergias":%s,
                 "coloresPreferidos":[],"estampadosPreferidos":[]}
                """.formatted(alergia, otraAlergia, sinAlergias);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String legado(String cifradoVersionado) {
        return cifradoVersionado.substring("v1:".length());
    }
}
