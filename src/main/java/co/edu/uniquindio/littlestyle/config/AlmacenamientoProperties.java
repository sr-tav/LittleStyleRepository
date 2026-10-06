package co.edu.uniquindio.littlestyle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Configuración del almacenamiento de imágenes del catálogo (ADR-10).
 * {@code tipo=s3} usa Amazon S3; {@code tipo=local} guarda en disco y sirve los archivos en {@code /media/**}
 * (solo para desarrollo y pruebas).
 */
@ConfigurationProperties(prefix = "app.storage")
public record AlmacenamientoProperties(
        @DefaultValue("local") String tipo,
        @DefaultValue Imagenes imagenes,
        @DefaultValue S3 s3,
        @DefaultValue Local local
) {

    public record Imagenes(
            @DefaultValue("5MB") DataSize maxTamano,
            @DefaultValue("8") int maxPorPrenda,
            @DefaultValue("10") int maxPorCarga
    ) {
    }

    public record S3(
            String bucket,
            @DefaultValue("us-east-1") String region,
            /** Base pública de las URLs (p. ej. un dominio de CloudFront). Vacío: URL estándar del bucket. */
            String urlPublicaBase,
            @DefaultValue("catalogo") String prefijo
    ) {
    }

    public record Local(
            @DefaultValue("./data/uploads") String directorio,
            @DefaultValue("/media") String urlBase
    ) {
    }
}
