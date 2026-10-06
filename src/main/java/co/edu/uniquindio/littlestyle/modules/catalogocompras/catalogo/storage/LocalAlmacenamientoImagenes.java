package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.storage;

import co.edu.uniquindio.littlestyle.config.AlmacenamientoProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Almacenamiento en disco para desarrollo y pruebas, sin credenciales de AWS. Los archivos se sirven en
 * {@code /media/**} (ver {@code AlmacenamientoConfig}). No debe usarse en producción.
 */
@Slf4j
public class LocalAlmacenamientoImagenes implements AlmacenamientoImagenes {

    private final Path raiz;
    private final String urlBase;

    public LocalAlmacenamientoImagenes(AlmacenamientoProperties.Local config) {
        this.raiz = Path.of(config.directorio()).toAbsolutePath().normalize();
        this.urlBase = config.urlBase().replaceAll("/+$", "");
    }

    public Path raiz() {
        return raiz;
    }

    @Override
    public String subir(String clave, byte[] contenido, String contentType) {
        Path destino = resolver(clave);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
        } catch (IOException ex) {
            throw new AlmacenamientoException("No fue posible guardar las imágenes", ex);
        }
        return urlBase + "/" + clave;
    }

    @Override
    public void eliminar(String clave) {
        try {
            Files.deleteIfExists(resolver(clave));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** Impide que una clave con {@code ..} escriba fuera del directorio configurado. */
    private Path resolver(String clave) {
        Path destino = raiz.resolve(clave).normalize();
        if (!destino.startsWith(raiz)) {
            throw new IllegalArgumentException("Clave de almacenamiento inválida: " + clave);
        }
        return destino;
    }
}
