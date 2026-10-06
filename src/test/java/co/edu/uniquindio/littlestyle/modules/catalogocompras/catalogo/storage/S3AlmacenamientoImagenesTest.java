package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.storage;

import co.edu.uniquindio.littlestyle.config.AlmacenamientoProperties;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3AlmacenamientoImagenesTest {

    @Mock
    private S3Client s3;

    private S3AlmacenamientoImagenes almacenamiento(String urlPublica) {
        return new S3AlmacenamientoImagenes(s3,
                new AlmacenamientoProperties.S3("littlestyle-media", "us-east-2", urlPublica, "/catalogo/"));
    }

    @Test
    void subeConPrefijoTipoYCacheYDevuelveLaUrlDelBucket() {
        String url = almacenamiento(null).subir("prendas/1/a.png", new byte[]{1, 2}, "image/png");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("littlestyle-media");
        assertThat(request.getValue().key()).isEqualTo("catalogo/prendas/1/a.png");
        assertThat(request.getValue().contentType()).isEqualTo("image/png");
        assertThat(request.getValue().cacheControl()).contains("immutable");
        assertThat(url).isEqualTo("https://littlestyle-media.s3.us-east-2.amazonaws.com/catalogo/prendas/1/a.png");
    }

    @Test
    void usaLaBasePublicaConfiguradaComoUnCdn() {
        String url = almacenamiento("https://cdn.littlestyle.co/").subir("prendas/1/a.png", new byte[]{1}, "image/png");

        assertThat(url).isEqualTo("https://cdn.littlestyle.co/catalogo/prendas/1/a.png");
    }

    @Test
    void unFalloDeS3SeTraduceA502SinDetallesTecnicos() {
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("Unable to execute HTTP request"));

        assertThatThrownBy(() -> almacenamiento(null).subir("prendas/1/a.png", new byte[]{1}, "image/png"))
                .isInstanceOf(AlmacenamientoException.class)
                .hasMessageNotContaining("HTTP")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void eliminaElObjetoConLaMismaClave() {
        almacenamiento(null).eliminar("prendas/1/a.png");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(request.capture());
        assertThat(request.getValue().key()).isEqualTo("catalogo/prendas/1/a.png");
    }

    @Test
    void exigeElBucket() {
        assertThatThrownBy(() -> new S3AlmacenamientoImagenes(s3,
                new AlmacenamientoProperties.S3(" ", "us-east-1", null, "catalogo")))
                .isInstanceOf(IllegalStateException.class);
    }
}
