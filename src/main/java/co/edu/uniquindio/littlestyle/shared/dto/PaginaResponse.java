package co.edu.uniquindio.littlestyle.shared.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Página de resultados para la API. Se usa en lugar de exponer {@link Page} de
 * Spring Data, que no tiene una representación JSON estable entre versiones.
 */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {

    public static <T> PaginaResponse<T> from(Page<T> pagina) {
        return new PaginaResponse<>(
                pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}
