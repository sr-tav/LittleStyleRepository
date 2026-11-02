package co.edu.uniquindio.littlestyle.modules.reportes.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.BusquedaCatalogo;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba la consulta JPQL real del reporte de búsquedas sobre H2, sin mocks: agrupación por término,
 * conteo de búsquedas sin resultados, filtro por categoría y rango de fechas.
 */
@DataJpaTest
@ActiveProfiles("test")
class ReporteBusquedaRepositoryTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 11, 1, 0, 0);

    @Autowired
    private ReporteBusquedaRepository repositorio;

    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void sembrar() {
        em.persist(busqueda("camisa", "Camisa", CategoriaPrenda.CAMISAS, 0, INICIO.plusDays(1)));
        em.persist(busqueda("camisa", "Camisa", CategoriaPrenda.CAMISAS, 0, INICIO.plusDays(2)));
        em.persist(busqueda("camisa", "Camisa", CategoriaPrenda.CAMISAS, 4, INICIO.plusDays(3)));
        em.persist(busqueda("vestido", "Vestido", CategoriaPrenda.VESTIDOS, 1, INICIO.plusDays(4)));
        em.persist(busqueda("vestido", "Vestido", CategoriaPrenda.VESTIDOS, 0, INICIO.plusDays(5)));
        em.persist(busqueda("fuera", "Fuera", CategoriaPrenda.CAMISAS, 0, INICIO.minusDays(1)));
        em.flush();
    }

    @Test
    void agrupaPorTerminoYCuentaLasBusquedasSinResultados() {
        assertThat(repositorio.contarTerminos(INICIO, FIN, null, null)).isEqualTo(2);

        List<Object[]> filas = repositorio.resumenTerminos(
                INICIO, FIN, null, null, PageRequest.of(0, 10));

        assertThat(filas).hasSize(2);
        Object[] camisa = filas.getFirst();
        assertThat(camisa[0]).isEqualTo("Camisa");
        assertThat(((Number) camisa[1]).longValue()).isEqualTo(3);
        assertThat(((Number) camisa[2]).longValue()).isEqualTo(2);
        assertThat(camisa[3]).isInstanceOf(LocalDateTime.class);

        Object[] vestido = filas.get(1);
        assertThat(vestido[0]).isEqualTo("Vestido");
        assertThat(((Number) vestido[1]).longValue()).isEqualTo(2);
        assertThat(((Number) vestido[2]).longValue()).isEqualTo(1);
    }

    @Test
    void filtraPorCategoriaSinPerderLaAgrupacion() {
        List<Object[]> filas = repositorio.resumenTerminos(
                INICIO, FIN, CategoriaPrenda.VESTIDOS, null, PageRequest.of(0, 10));

        assertThat(filas).hasSize(1);
        assertThat(filas.getFirst()[0]).isEqualTo("Vestido");
        assertThat(repositorio.contarTerminos(INICIO, FIN, CategoriaPrenda.VESTIDOS, null))
                .isEqualTo(1);
    }

    @Test
    void respetaElRangoDeFechasConFinExclusivo() {
        List<Object[]> filas = repositorio.resumenTerminos(
                INICIO, FIN.plusDays(10), null, null, PageRequest.of(0, 10));

        assertThat(filas).hasSize(2);
        assertThat(repositorio.contarTerminos(FIN, FIN.plusDays(10), null, null))
                .isZero();
    }

    @Test
    void elAlcanceDeVendedorSoloCuentaBusquedasSinResultadosDeSusCategorias() {
        // Sin prendas sembradas, el vendedor no tiene categorías: ninguna búsqueda entra en su alcance.
        assertThat(repositorio.contarTerminos(INICIO, FIN, null, 99L)).isZero();
        assertThat(repositorio.resumenTerminos(INICIO, FIN, null, 99L, PageRequest.of(0, 10))).isEmpty();
    }

    private static BusquedaCatalogo busqueda(String normalizado, String original,
                                             CategoriaPrenda categoria, int resultados,
                                             LocalDateTime fecha) {
        return BusquedaCatalogo.builder()
                .terminoNormalizado(normalizado)
                .terminoOriginal(original)
                .categoria(categoria)
                .resultadosCount(resultados)
                .creadoEn(fecha)
                .build();
    }
}
