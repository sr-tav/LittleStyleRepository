package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.storage;

import co.edu.uniquindio.littlestyle.config.AlmacenamientoProperties;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Implementación sobre Amazon S3 con el SDK de AWS v2. Las credenciales se resuelven con la cadena
 * estándar del SDK (variables de entorno, perfil o rol IAM de la instancia, ADR-09); nunca se configuran
 * en el código. El bucket debe permitir lectura pública de los objetos para que Angular cargue las
 * imágenes directamente (ADR-10).
 */
@Slf4j
public class S3AlmacenamientoImagenes implements AlmacenamientoImagenes {

    private static final String CACHE_INMUTABLE = "public, max-age=31536000, immutable";

    private final S3Client s3;
    private final AlmacenamientoProperties.S3 config;

    public S3AlmacenamientoImagenes(S3Client s3, AlmacenamientoProperties.S3 config) {
        if (config.bucket() == null || config.bucket().isBlank()) {
            throw new IllegalStateException("app.storage.s3.bucket es obligatorio cuando app.storage.tipo=s3");
        }
        this.s3 = s3;
        this.config = config;
    }

    @Override
    public String subir(String clave, byte[] contenido, String contentType) {
        String objeto = claveObjeto(clave);
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(config.bucket())
                            .key(objeto)
                            .contentType(contentType)
                            // Cada carga usa una clave nueva, así que el objeto nunca cambia
                            .cacheControl(CACHE_INMUTABLE)
                            .build(),
                    RequestBody.fromBytes(contenido));
        } catch (SdkException ex) {
            log.error("No fue posible subir la imagen {} a S3", objeto, ex);
            throw new AlmacenamientoException("No fue posible guardar las imágenes. Intenta de nuevo en unos minutos", ex);
        }
        return urlPublica(objeto);
    }

    @Override
    public void eliminar(String clave) {
        String objeto = claveObjeto(clave);
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(config.bucket()).key(objeto).build());
        } catch (SdkException ex) {
            log.error("No fue posible eliminar la imagen {} de S3", objeto, ex);
            throw new AlmacenamientoException("No fue posible eliminar la imagen del almacenamiento", ex);
        }
    }

    private String claveObjeto(String clave) {
        String prefijo = config.prefijo() == null ? "" : config.prefijo().replaceAll("^/+|/+$", "");
        return prefijo.isEmpty() ? clave : prefijo + "/" + clave;
    }

    private String urlPublica(String objeto) {
        String base = config.urlPublicaBase();
        if (base == null || base.isBlank()) {
            base = "https://" + config.bucket() + ".s3." + config.region() + ".amazonaws.com";
        }
        return base.replaceAll("/+$", "") + "/" + objeto;
    }
}
