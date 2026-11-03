package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Los filtros disponible, talla y edad se evalúan sobre la misma talla. */
@DataJpaTest
@ActiveProfiles("test")
class PrendaRepositoryDisponibilidadTest {

    @Autowired private PrendaRepository repositorio;
    @Autowired private TestEntityManager em;

    private Long agotadaEn24Meses;
    private Long conStockEn24Meses;
    private Long combinacionesColor;

    @BeforeEach
    void sembrar() {
        Usuario vendedor = em.persist(Usuario.builder().nombre("Ana").apellido("Ruiz")
                .email("ana-disp@correo.com").password("x").rol(Rol.VENDEDOR).estado(EstadoUsuario.ACTIVO)
                .nombreTienda("Tienda").fechaRegistro(LocalDateTime.now()).build());
        // A: la talla de 24-72 meses está agotada; la de 72-144 tiene stock
        agotadaEn24Meses = persistirPrenda(vendedor, "Prenda A", 0, 5);
        // B: la talla de 24-72 meses tiene stock
        conStockEn24Meses = persistirPrenda(vendedor, "Prenda B", 3, 0);
        Prenda porColor = Prenda.builder().vendedor(vendedor).nombre("Prenda por color")
                .categoria(CategoriaPrenda.CAMISAS).precio(new BigDecimal("30000")).estado(EstadoPrenda.ACTIVA)
                .fechaCreacion(LocalDateTime.now()).fechaActualizacion(LocalDateTime.now()).build();
        porColor.getTallas().add(talla(porColor, "4", 24, 72, 0, ColorPreferido.AZUL));
        porColor.getTallas().add(talla(porColor, "4", 24, 72, 2, ColorPreferido.ROSA));
        porColor.getTallas().add(talla(porColor, "8", 73, 144, 3, ColorPreferido.AZUL));
        combinacionesColor = em.persistAndFlush(porColor).getId();
        em.flush();
        em.clear();
    }

    private Long persistirPrenda(Usuario vendedor, String nombre, int stockPequena, int stockGrande) {
        Prenda prenda = Prenda.builder().vendedor(vendedor).nombre(nombre).categoria(CategoriaPrenda.CAMISAS)
                .precio(new BigDecimal("30000")).estado(EstadoPrenda.ACTIVA)
                .fechaCreacion(LocalDateTime.now()).fechaActualizacion(LocalDateTime.now()).build();
        prenda.getTallas().add(talla(prenda, "4", 24, 72, stockPequena));
        prenda.getTallas().add(talla(prenda, "8", 73, 144, stockGrande));
        return em.persistAndFlush(prenda).getId();
    }

    private static TallaPrenda talla(Prenda prenda, String nombre, int edadMin, int edadMax, int stock) {
        return talla(prenda, nombre, edadMin, edadMax, stock, null);
    }

    private static TallaPrenda talla(Prenda prenda, String nombre, int edadMin, int edadMax, int stock,
                                     ColorPreferido color) {
        return TallaPrenda.builder().prenda(prenda).talla(nombre)
                .estaturaMinCm(BigDecimal.valueOf(100)).estaturaMaxCm(BigDecimal.valueOf(120))
                .pesoMinKg(BigDecimal.valueOf(15)).pesoMaxKg(BigDecimal.valueOf(25)).color(color)
                .edadMinMeses(edadMin).edadMaxMeses(edadMax).stock(stock).build();
    }

    private List<Long> buscar(Boolean disponible, String talla, Integer edadMeses) {
        return repositorio.buscarIdsVitrina(EstadoPrenda.ACTIVA, null, null, null, null, null,
                disponible, null, talla, edadMeses, PageRequest.of(0, 20)).getContent();
    }

    @Test
    void disponibleYEdadExigenStockEnLaTallaDeEsaEdad() {
        assertThat(buscar(true, null, 24)).contains(conStockEn24Meses).doesNotContain(agotadaEn24Meses);
    }

    @Test
    void tallaYDisponibleExigenStockEnEsaTalla() {
        assertThat(buscar(true, "8", null)).contains(agotadaEn24Meses).doesNotContain(conStockEn24Meses);
    }

    @Test
    void sinDisponibleLaEdadMuestraTambienLasAgotadas() {
        assertThat(buscar(null, null, 24)).contains(conStockEn24Meses, agotadaEn24Meses);
    }

    @Test
    void colorYTallaSeFiltranEnLaMismaVarianteDisponible() {
        assertThat(buscarColor(ColorPreferido.AZUL, "8")).contains(combinacionesColor);
        assertThat(buscarColor(ColorPreferido.ROSA, "8")).doesNotContain(combinacionesColor);
        assertThat(buscarColor(ColorPreferido.AZUL, "4")).doesNotContain(combinacionesColor);
    }

    private List<Long> buscarColor(ColorPreferido color, String talla) {
        return repositorio.buscarIdsVitrinaConColor(EstadoPrenda.ACTIVA, null, null, null, null, null,
                null, null, talla, color, null, PageRequest.of(0, 20)).getContent();
    }
}