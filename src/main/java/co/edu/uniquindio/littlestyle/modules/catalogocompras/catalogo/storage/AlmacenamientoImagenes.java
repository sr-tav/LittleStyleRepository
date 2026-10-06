package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.storage;

/**
 * Puerto hacia el almacenamiento externo de imágenes. La base de datos solo guarda la URL pública y la
 * clave del objeto (ADR-10).
 */
public interface AlmacenamientoImagenes {

    /**
     * Guarda el archivo bajo la clave indicada y devuelve su URL pública.
     *
     * @throws AlmacenamientoException si el servicio externo no está disponible o rechaza el archivo
     */
    String subir(String clave, byte[] contenido, String contentType);

    /** Elimina el objeto. No falla si ya no existe. */
    void eliminar(String clave);
}
