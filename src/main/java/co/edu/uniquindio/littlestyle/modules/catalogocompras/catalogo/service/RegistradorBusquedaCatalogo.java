package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.BusquedaCatalogo;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.BusquedaCatalogoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Persiste las búsquedas del catálogo de forma asíncrona y tolerante a fallos (US-16).
 *
 * <p>La búsqueda del cliente ya se respondió cuando esto corre. Cualquier error se registra en el
 * log y se descarta: nunca se propaga al llamador ni cambia la respuesta de la vitrina.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistradorBusquedaCatalogo {

    private final BusquedaCatalogoRepository repository;

    /**
     * Registra el término buscado. Es {@code @Async}: se ejecuta fuera del hilo de la petición y
     * atrapa {@link Throwable} para que un fallo de base de datos no afecte la búsqueda.
     *
     * @param terminoOriginal texto tal como lo escribió el usuario (se recorta a 200 caracteres)
     * @param categoria       categoría solicitada, o {@code null} si no se filtró
     * @param resultados      total de coincidencias de la búsqueda (no solo de la página)
     * @param usuarioId       id del usuario autenticado, o {@code null} si es anónimo
     */
    @Async
    public void registrar(String terminoOriginal, CategoriaPrenda categoria, long resultados, Long usuarioId) {
        try {
            String normalizado = NormalizadorBusqueda.normalizar(terminoOriginal);
            if (normalizado.isBlank()) {
                return;
            }
            repository.save(BusquedaCatalogo.builder()
                    .terminoNormalizado(normalizado)
                    .terminoOriginal(NormalizadorBusqueda.recortar(terminoOriginal.trim()))
                    .categoria(categoria)
                    .resultadosCount((int) Math.min(Integer.MAX_VALUE, Math.max(0, resultados)))
                    .usuarioId(usuarioId)
                    .build());
        } catch (Throwable ex) {
            // Un fallo de telemetría de búsquedas NUNCA rompe la búsqueda del cliente.
            log.warn("busqueda.registro resultado=ERROR motivo={}", ex.getClass().getSimpleName());
        }
    }
}
