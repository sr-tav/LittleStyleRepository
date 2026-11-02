package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normaliza los términos de búsqueda para agruparlos en el reporte de búsquedas (US-16).
 *
 * <p>Reglas: recorta extremos, pasa a minúsculas, elimina tildes/diacríticos, colapsa espacios y
 * limita a 200 caracteres (el ancho de la columna). El término original se conserva aparte.</p>
 */
public final class NormalizadorBusqueda {

    public static final int LONGITUD_MAXIMA = 200;

    private NormalizadorBusqueda() {
    }

    public static String normalizar(String termino) {
        if (termino == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(termino, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String limpio = sinAcentos.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return recortar(limpio);
    }

    public static String recortar(String termino) {
        if (termino == null) {
            return "";
        }
        return termino.length() <= LONGITUD_MAXIMA ? termino : termino.substring(0, LONGITUD_MAXIMA);
    }
}
