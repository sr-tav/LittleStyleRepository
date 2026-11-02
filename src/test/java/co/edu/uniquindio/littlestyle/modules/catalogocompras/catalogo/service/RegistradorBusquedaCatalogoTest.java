package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.BusquedaCatalogo;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.BusquedaCatalogoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistradorBusquedaCatalogoTest {

    @Mock private BusquedaCatalogoRepository repository;
    @InjectMocks private RegistradorBusquedaCatalogo registrador;

    @Test
    void guardaElTerminoNormalizadoSinDatosPersonales() {
        registrador.registrar("  Vestido  Ñandú ", CategoriaPrenda.VESTIDOS, 3L, 7L);

        ArgumentCaptor<BusquedaCatalogo> captor = ArgumentCaptor.forClass(BusquedaCatalogo.class);
        verify(repository).save(captor.capture());
        BusquedaCatalogo guardada = captor.getValue();
        assertThat(guardada.getTerminoNormalizado()).isEqualTo("vestido nandu");
        assertThat(guardada.getTerminoOriginal()).isEqualTo("Vestido  Ñandú");
        assertThat(guardada.getCategoria()).isEqualTo(CategoriaPrenda.VESTIDOS);
        assertThat(guardada.getResultadosCount()).isEqualTo(3);
        assertThat(guardada.getUsuarioId()).isEqualTo(7L);
    }

    @Test
    void unTerminoVacioNoTocaLaBaseDeDatos() {
        registrador.registrar("   ", null, 0, null);

        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void unFalloDeBaseDeDatosNuncaRompeLaBusquedaDelCliente() {
        when(repository.save(any())).thenThrow(new IllegalStateException("sin conexión"));

        assertThatCode(() -> registrador.registrar("vestido", null, 0, null)).doesNotThrowAnyException();
    }
}
