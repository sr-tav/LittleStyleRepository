package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.config.AlmacenamientoProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.storage.AlmacenamientoException;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.storage.AlmacenamientoImagenes;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.VENDEDOR_ID;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.autenticarVendedor;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.prenda;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImagenPrendaServiceTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2};

    @Mock
    private PrendaRepository prendaRepository;
    @Mock
    private AlmacenamientoImagenes almacenamiento;

    private ImagenPrendaService service;
    private Prenda prenda;

    @BeforeEach
    void setUp() {
        autenticarVendedor();
        TransactionSynchronizationManager.initSynchronization();
        var props = new AlmacenamientoProperties("local",
                new AlmacenamientoProperties.Imagenes(DataSize.ofKilobytes(1), 3, 2),
                new AlmacenamientoProperties.S3(null, "us-east-1", null, "catalogo"),
                new AlmacenamientoProperties.Local("target/x", "/media"));
        service = new ImagenPrendaService(prendaRepository, almacenamiento, props);
        prenda = prenda(10L, 5, 1, 1);
    }

    @AfterEach
    void limpiar() {
        TransactionSynchronizationManager.clearSynchronization();
        SecurityContextHolder.clearContext();
    }

    private static MultipartFile archivo(String nombre, byte[] datos) {
        return new MockMultipartFile("archivos", nombre, "image/png", datos);
    }

    private void finalizarTransaccion(int estado) {
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(estado));
    }

    @Test
    void subeVariasImagenesDetectandoElTipoPorSuContenido() {
        when(prendaRepository.bloquearPropia(10L, VENDEDOR_ID)).thenReturn(Optional.of(prenda));
        when(almacenamiento.subir(anyString(), any(), anyString())).thenAnswer(inv -> "https://cdn/" + inv.getArgument(0));

        List<PrendaResponse.Imagen> imagenes = service.subir(10L,
                List.of(archivo("frente.png", PNG), archivo("espalda.png", JPEG)));

        ArgumentCaptor<String> claves = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> tipos = ArgumentCaptor.forClass(String.class);
        verify(almacenamiento, times(2)).subir(claves.capture(), any(), tipos.capture());
        assertThat(claves.getAllValues()).allMatch(c -> c.startsWith("prendas/10/"));
        assertThat(claves.getAllValues().get(0)).endsWith(".png");
        // El nombre dice .png pero el contenido es JPEG: manda el contenido
        assertThat(claves.getAllValues().get(1)).endsWith(".jpg");
        assertThat(tipos.getAllValues()).containsExactly("image/png", "image/jpeg");
        assertThat(imagenes).extracting(PrendaResponse.Imagen::orden).containsExactly(0, 1);
        assertThat(prenda.getImagenes()).hasSize(2);
    }

    @Test
    void siUnArchivoNoEsImagenNoSeSubeNinguno() {
        when(prendaRepository.bloquearPropia(10L, VENDEDOR_ID)).thenReturn(Optional.of(prenda));

        assertThatThrownBy(() -> service.subir(10L,
                List.of(archivo("ok.png", PNG), archivo("virus.png", "MZ-no-imagen".getBytes()))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El archivo virus.png no es una imagen JPG, PNG o WEBP");
        verifyNoInteractions(almacenamiento);
    }

    @Test
    void rechazaArchivosQueSuperanElTamanoMaximo() {
        when(prendaRepository.bloquearPropia(10L, VENDEDOR_ID)).thenReturn(Optional.of(prenda));
        byte[] grande = new byte[2048];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);

        assertThatThrownBy(() -> service.subir(10L, List.of(archivo("grande.png", grande))))
                .hasMessageContaining("supera el tamaño máximo");
        verifyNoInteractions(almacenamiento);
    }

    @Test
    void respetaLosLimitesPorCargaYPorPrenda() {
        assertThatThrownBy(() -> service.subir(10L,
                List.of(archivo("a.png", PNG), archivo("b.png", PNG), archivo("c.png", PNG))))
                .hasMessage("Puedes subir máximo 2 imágenes a la vez");

        prenda.getImagenes().add(ImagenPrenda.builder().id(1L).orden(0).url("u").clave("k").build());
        prenda.getImagenes().add(ImagenPrenda.builder().id(2L).orden(1).url("u").clave("k").build());
        when(prendaRepository.bloquearPropia(10L, VENDEDOR_ID)).thenReturn(Optional.of(prenda));
        assertThatThrownBy(() -> service.subir(10L, List.of(archivo("a.png", PNG), archivo("b.png", PNG))))
                .hasMessage("Una prenda admite máximo 3 imágenes (ya tiene 2)");
        verifyNoInteractions(almacenamiento);
    }

    @Test
    void siLaTransaccionNoSeConfirmaBorraLoQueYaSeSubio() {
        when(prendaRepository.bloquearPropia(10L, VENDEDOR_ID)).thenReturn(Optional.of(prenda));
        when(almacenamiento.subir(anyString(), any(), anyString()))
                .thenReturn("https://cdn/1")
                .thenThrow(new AlmacenamientoException("S3 caído", new RuntimeException()));

        assertThatThrownBy(() -> service.subir(10L, List.of(archivo("a.png", PNG), archivo("b.png", PNG))))
                .isInstanceOf(AlmacenamientoException.class);
        finalizarTransaccion(TransactionSynchronization.STATUS_ROLLED_BACK);

        ArgumentCaptor<String> subida = ArgumentCaptor.forClass(String.class);
        verify(almacenamiento, times(2)).subir(subida.capture(), any(), anyString());
        verify(almacenamiento).eliminar(subida.getAllValues().getFirst());
    }

    @Test
    void alEliminarBorraElObjetoSoloDespuesDeConfirmar() {
        prenda.getImagenes().add(ImagenPrenda.builder().id(5L).orden(0).url("u").clave("prendas/10/a.png")
                .fechaCarga(LocalDateTime.now()).build());
        when(prendaRepository.bloquearPropia(10L, VENDEDOR_ID)).thenReturn(Optional.of(prenda));

        service.eliminar(10L, 5L);

        assertThat(prenda.getImagenes()).isEmpty();
        verify(almacenamiento, never()).eliminar(anyString());
        finalizarTransaccion(TransactionSynchronization.STATUS_COMMITTED);
        verify(almacenamiento).eliminar(eq("prendas/10/a.png"));
    }

    @Test
    void marcarPrincipalMueveLaImagenAlInicio() {
        prenda.getImagenes().add(ImagenPrenda.builder().id(1L).orden(0).url("a").clave("a").build());
        prenda.getImagenes().add(ImagenPrenda.builder().id(2L).orden(1).url("b").clave("b").build());
        prenda.getImagenes().add(ImagenPrenda.builder().id(3L).orden(2).url("c").clave("c").build());
        when(prendaRepository.bloquearPropia(10L, VENDEDOR_ID)).thenReturn(Optional.of(prenda));

        List<PrendaResponse.Imagen> imagenes = service.marcarPrincipal(10L, 3L);

        assertThat(imagenes).extracting(PrendaResponse.Imagen::id).containsExactly(3L, 1L, 2L);
        assertThat(imagenes).extracting(PrendaResponse.Imagen::orden).containsExactly(0, 1, 2);
    }
}
