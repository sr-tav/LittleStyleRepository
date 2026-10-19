package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaVitrinaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.prenda;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test unitario del servicio de vitrina con el repositorio mockeado (mockeamos las consultas
 * de Spring Data). Cubre filtros en BD, paginación, validaciones 400 y el detalle sin datos internos.
 */
@ExtendWith(MockitoExtension.class)
class CatalogoClienteServiceTest {

    // Repositorio mockeado: devolvemos ids y prendas de fixture según la consulta que recibe
    @Mock
    private PrendaRepository prendaRepository;
    // Puerto y motor de recomendación mockeados, para el caso "solo mi talla"
    @Mock
    private co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port.PerfilBiometricoPort perfiles;
    @Mock
    private co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.service.MotorRecomendacionService motor;

    @InjectMocks
    private CatalogoClienteService service;

    // Búsqueda general de vitrina: solo activas, con los filtros que viajan a la consulta en BD,
    // orden anexado (precio + desempate por id) y paginación respetada
    @Test
    void exploraSoloPublicadasConPaginacionYOrden() {
        var p = prenda(10L, 5, 3, 0);
        when(prendaRepository.buscarIdsVitrina(eq(EstadoPrenda.ACTIVA), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(10L), PageRequest.of(0, 12), 1));
        when(prendaRepository.buscarPorIds(List.of(10L))).thenReturn(List.of(p));

        var pagina = service.explorar("jogger", "PANTALONES", null, "10000", "50000", "true", null, "4", 0, 12,
                "precioAsc");

        assertThat(pagina.contenido()).extracting(PrendaVitrinaResponse::id).containsExactly(10L);
        PrendaVitrinaResponse item = pagina.contenido().getFirst();
        assertThat(item.disponible()).isTrue();
        assertThat(item.tallasDisponibles()).containsExactly("4");
        assertThat(item.precio()).isNotNull();
        ArgumentCaptor<Pageable> paginable = ArgumentCaptor.forClass(Pageable.class);
        verify(prendaRepository).buscarIdsVitrina(eq(EstadoPrenda.ACTIVA), any(), any(), any(), any(), eq("jogger"),
                eq(Boolean.TRUE), any(), eq("4"), eq(null), paginable.capture());
        assertThat(paginable.getValue().getPageSize()).isEqualTo(12);
        assertThat(paginable.getValue().getSort().getOrderFor("precio")).isNotNull();
        // Desempate por id: paginación estable.
        assertThat(paginable.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    // Si el cliente pide 500 elementos de golpe, el servicio lo recorta al máximo permitido
    // antes de tocar la base de datos
    @Test
    void limitaElTamanoDePaginaParaProtegerLaBaseDeDatos() {
        when(prendaRepository.buscarIdsVitrina(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.explorar(null, null, null, null, null, null, null, null, 0, 500, "novedades");

        ArgumentCaptor<Pageable> paginable = ArgumentCaptor.forClass(Pageable.class);
        verify(prendaRepository).buscarIdsVitrina(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), paginable.capture());
        assertThat(paginable.getValue().getPageSize()).isEqualTo(CatalogoClienteService.TAMANO_MAXIMO);
    }

    // Con ids de recomendados se usa la consulta por ids; la búsqueda general ni se toca
    @Test
    void idsDeRecomendadosFiltraEnBaseDeDatos() {
        var p = prenda(10L, 5, 3, 0);
        when(prendaRepository.buscarIdsVitrinaPorIds(eq(EstadoPrenda.ACTIVA), eq(List.of(10L, 20L)), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(10L), PageRequest.of(0, 12), 1));
        when(prendaRepository.buscarPorIds(List.of(10L))).thenReturn(List.of(p));

        var pagina = service.explorar(null, null, null, null, null, null, null, null, List.of(10L, 20L), 0, 12,
                "novedades");

        assertThat(pagina.contenido()).extracting(PrendaVitrinaResponse::id).containsExactly(10L);
        verify(prendaRepository, never()).buscarIdsVitrina(any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any());
    }

    // Lista de ids vacía = corto circuito: se devuelve la página vacía sin ir a la base de datos
    @Test
    void idsVaciosDevuelvenPaginaVaciaSinConsultar() {
        var pagina = service.explorar(null, null, null, null, null, null, null, null, List.of(), 0, 12,
                "novedades");

        assertThat(pagina.contenido()).isEmpty();
        verifyNoInteractions(prendaRepository);
    }

    // La edad válida filtra dentro del rango de la prenda; la negativa rompe con 400
    @Test
    void edadFiltraPorRangoDeclaradoYNegativaEs400() {
        var p = prenda(10L, 5, 3, 0);
        when(prendaRepository.buscarIdsVitrina(eq(EstadoPrenda.ACTIVA), any(), any(), any(), any(), any(), any(),
                any(), any(), eq(48), any()))
                .thenReturn(new PageImpl<>(List.of(10L), PageRequest.of(0, 12), 1));
        when(prendaRepository.buscarPorIds(List.of(10L))).thenReturn(List.of(p));

        var pagina = service.explorar(null, null, null, null, null, null, null, null, null, 48, 0, 12,
                "novedades");

        assertThat(pagina.contenido()).extracting(PrendaVitrinaResponse::id).containsExactly(10L);
        assertThatThrownBy(() -> service.explorar(null, null, null, null, null, null, null, null, null, -1, 0,
                12, "novedades"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // Varias categorías de material llegan juntas al repositorio: busca prendas con alguna de ellas
    @Test
    void materialesMultiplesFiltranPorAlgunComponente() {
        when(prendaRepository.buscarIdsVitrina(eq(EstadoPrenda.ACTIVA), any(), any(), any(), any(), any(), any(),
                eq(List.of(MaterialTextil.ALGODON, MaterialTextil.POLIESTER)), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.explorar(null, null, null, null, null, null, "ALGODON,POLIESTER", null, null, null, 0, 12,
                "novedades");

        verify(prendaRepository).buscarIdsVitrina(eq(EstadoPrenda.ACTIVA), any(), any(), any(), any(), any(), any(),
                eq(List.of(MaterialTextil.ALGODON, MaterialTextil.POLIESTER)), any(), any(), any());
    }

    // Cada filtro malformado (categoría, rango de precios, orden, página negativa) debe reventar en 400
    @Test
    void rechazaFiltrosInvalidosCon400() {
        assertThatThrownBy(() -> service.explorar(null, "ROPA", null, null, null, null, null, null, 0, 12, "novedades"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> service.explorar(null, null, null, "50000", "10000", null, null, null, 0, 12, "novedades"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.explorar(null, null, null, null, null, null, null, null, 0, 12, "color"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.explorar(null, null, null, null, null, null, null, null, -1, 12, "novedades"))
                .isInstanceOf(BusinessException.class);
    }

    // El detalle público muestra el stock por talla (disponible o no) pero nunca el stock mínimo del vendedor
    @Test
    void detalleMuestraExistenciasPorTallaPeroOcultaElMinimo() {
        when(prendaRepository.findByIdAndEstado(10L, EstadoPrenda.ACTIVA))
                .thenReturn(Optional.of(prenda(10L, 5, 3, 0)));

        PrendaDetalleResponse detalle = service.detalle(10L);

        assertThat(detalle.id()).isEqualTo(10L);
        assertThat(detalle.disponible()).isTrue();
        assertThat(detalle.tallas()).extracting(PrendaDetalleResponse.TallaDisponible::disponible)
                .containsExactly(true, false);
        assertThat(detalle.tallas()).extracting(PrendaDetalleResponse.TallaDisponible::stockDisponible)
                .containsExactly(3, 0);
    }

    // Si la prenda no existe o está inactiva, para el cliente es un 404
    @Test
    void detalleDePrendaInexistenteOInactivaDevuelve404() {
        when(prendaRepository.findByIdAndEstado(99L, EstadoPrenda.ACTIVA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detalle(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // Enum de material desconocido (ej. "TELA") se rechaza con 400 antes de consultar
    @Test
    void materialInvalidoDevuelve400() {
        assertThatThrownBy(() -> service.explorar(null, null, null, null, null, null, "TELA", null, 0, 12, "novedades"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // Igual con el género: un valor fuera del enum se rechaza con 400
    @Test
    void generoInvalidoDevuelve400() {
        assertThatThrownBy(() -> service.explorar(null, null, "ADULTO", null, null, null, null, null, 0, 12, "novedades"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // Con "solo mi talla", el perfil biométrico y el motor de recomendación resuelven los ids
    // dentro del servidor y luego se filtra por ellos en base de datos
    @Test
    void soloMiTallaResuelveIdsDentroDelServidor() {
        var p = prenda(10L, 5, 3, 0);
        var bio = org.mockito.Mockito.mock(
                co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.PerfilBiometrico.class);
        when(perfiles.obtenerActivos(3L)).thenReturn(bio);
        when(motor.idsRecomendados(bio)).thenReturn(List.of(10L, 20L));
        when(prendaRepository.buscarIdsVitrinaPorIds(eq(EstadoPrenda.ACTIVA), eq(List.of(10L, 20L)), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(10L), PageRequest.of(0, 12), 1));
        when(prendaRepository.buscarPorIds(List.of(10L))).thenReturn(List.of(p));

        var pagina = service.explorar(null, null, null, null, null, null, null, null, null,
                3L, true, null, 0, 12, "novedades");

        assertThat(pagina.contenido()).extracting(PrendaVitrinaResponse::id).containsExactly(10L);
    }

    // Pedir "solo mi talla" sin perfil asociado no tiene sentido: error 400 directo
    @Test
    void soloMiTallaSinPerfilDevuelve400() {
        assertThatThrownBy(() -> service.explorar(null, null, null, null, null, null, null, null, null,
                null, true, null, 0, 12, "novedades"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // Precio y disponibilidad se aplican en la consulta de BD (no en memoria) según los valores de la URL
    @Test
    void precioYDisponibilidadFiltranEnBaseDeDatos() {
        var p = prenda(10L, 5, 3, 0);
        when(prendaRepository.buscarIdsVitrina(eq(EstadoPrenda.ACTIVA), any(), any(),
                eq(new java.math.BigDecimal("10000")), eq(new java.math.BigDecimal("50000")), any(),
                eq(Boolean.TRUE), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(10L), PageRequest.of(0, 12), 1));
        when(prendaRepository.buscarPorIds(List.of(10L))).thenReturn(List.of(p));

        var pagina = service.explorar(null, null, null, "10000", "50000", "true", null, null, null,
                null, 0, 12, "novedades");

        assertThat(pagina.contenido()).extracting(PrendaVitrinaResponse::id).containsExactly(10L);
    }

    // Un valor booleano raro como "si" en disponibilidad también cae en 400
    @Test
    void disponibilidadInvalidaDevuelve400() {
        assertThatThrownBy(() -> service.explorar(null, null, null, null, null, "si", null, null,
                null, null, 0, 12, "novedades"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
