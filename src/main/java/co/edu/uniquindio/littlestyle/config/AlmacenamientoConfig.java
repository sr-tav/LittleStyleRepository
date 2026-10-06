package co.edu.uniquindio.littlestyle.config;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.storage.AlmacenamientoImagenes;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.storage.LocalAlmacenamientoImagenes;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.storage.S3AlmacenamientoImagenes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Selecciona la implementación de {@link AlmacenamientoImagenes} según {@code app.storage.tipo}.
 */
@Configuration
@EnableConfigurationProperties(AlmacenamientoProperties.class)
public class AlmacenamientoConfig {

    /** Ruta donde se publican las imágenes del almacenamiento local. */
    public static final String RUTA_MEDIA = "/media/**";

    @Configuration
    @ConditionalOnProperty(name = "app.storage.tipo", havingValue = "s3")
    static class S3 {

        @Bean
        S3Client s3Client(AlmacenamientoProperties props) {
            return S3Client.builder().region(Region.of(props.s3().region())).build();
        }

        @Bean
        AlmacenamientoImagenes almacenamientoImagenes(S3Client s3Client, AlmacenamientoProperties props) {
            return new S3AlmacenamientoImagenes(s3Client, props.s3());
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "app.storage.tipo", havingValue = "local", matchIfMissing = true)
    static class Local implements WebMvcConfigurer {

        private final LocalAlmacenamientoImagenes almacenamiento;

        Local(AlmacenamientoProperties props) {
            this.almacenamiento = new LocalAlmacenamientoImagenes(props.local());
        }

        @Bean
        AlmacenamientoImagenes almacenamientoImagenes() {
            return almacenamiento;
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler(RUTA_MEDIA)
                    .addResourceLocations(almacenamiento.raiz().toUri().toString());
        }
    }
}
