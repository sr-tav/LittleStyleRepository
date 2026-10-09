package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaDetalleResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaVitrinaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.prenda;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoClienteServiceTest {

    @Mock
    private PrendaRepository prendaRepository;

    @InjectMocks
    private CatalogoClienteService service;

    @Test
    void exploraSoloPublicadasConPaginacionYOrden() {
        var p = prenda(10L, 5, 3, 0);
        when(prendaRepository.buscarVitrina(eq(EstadoPrenda.ACTIVA), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(p)));

        var pagina = service.explorar("jogger", "PANTALONES", null, "10000", "50000", "true", null, "4", 0, 12,
                "precioAsc");

        assertThat(pagina.contenido()).extracting(PrendaVitrinaResponse::id).containsExactly(10L);
        PrendaVitrinaResponse item = pagina.contenido().getFirst();
        assertThat(item.disponible()).isTrue();
        assertThat(item.precio()).isNotNull();
        ArgumentCaptor<Pageable> paginable = ArgumentCaptor.forClass(Pageable.class);
        verify(prendaRepository).buscarVitrina(eq(EstadoPrenda.ACTIVA), any(), any(), any(), any(), eq("jogger"),
                eq(Boolean.TRUE), any(), eq("4"), paginable.capture());
        assertThat(paginable.getValue().getPageSize()).isEqualTo(12);
        assertThat(paginable.getValue().getSort().getOrderFor("precio")).isNotNull();
    }

    @Test
    void limitaElTamanoDePaginaParaProtegerLaBaseDeDatos() {
        when(prendaRepository.buscarVitrina(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.explorar(null, null, null, null, null, null, null, null, 0, 500, "novedades");

        ArgumentCaptor<Pageable> paginable = ArgumentCaptor.forClass(Pageable.class);
        verify(prendaRepository).buscarVitrina(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                paginable.capture());
        assertThat(paginable.getValue().getPageSize()).isEqualTo(CatalogoClienteService.TAMANO_MAXIMO);
    }

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

    @Test
    void detalleOcultaExistenciasExactasYMinimo() {
        when(prendaRepository.findByIdAndEstado(10L, EstadoPrenda.ACTIVA))
                .thenReturn(Optional.of(prenda(10L, 5, 3, 0)));

        PrendaDetalleResponse detalle = service.detalle(10L);

        assertThat(detalle.id()).isEqualTo(10L);
        assertThat(detalle.disponible()).isTrue();
        assertThat(detalle.tallas()).extracting(PrendaDetalleResponse.TallaDisponible::disponible)
                .containsExactly(true, false);
    }

    @Test
    void detalleDePrendaInexistenteOInactivaDevuelve404() {
        when(prendaRepository.findByIdAndEstado(99L, EstadoPrenda.ACTIVA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detalle(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void materialInvalidoDevuelve400() {
        assertThatThrownBy(() -> service.explorar(null, null, null, null, null, null, "TELA", null, 0, 12, "novedades"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void generoInvalidoDevuelve400() {
        assertThatThrownBy(() -> service.explorar(null, null, "ADULTO", null, null, null, null, null, 0, 12, "novedades"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
