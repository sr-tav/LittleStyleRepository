package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.config.AlmacenamientoProperties;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.storage.AlmacenamientoImagenes;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.storage.TipoImagen;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Carga masiva y eliminación de fotos de una prenda. La base de datos y el almacenamiento externo no
 * comparten transacción, así que se compensa: si la transacción no se confirma, los objetos ya subidos se
 * borran; al eliminar una foto, el objeto solo se borra después de confirmar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImagenPrendaService {

    private final PrendaRepository prendaRepository;
    private final AlmacenamientoImagenes almacenamiento;
    private final AlmacenamientoProperties props;

    /** Sube todas las imágenes o ninguna: primero valida cada archivo y luego las envía al almacenamiento. */
    @Transactional
    public List<PrendaResponse.Imagen> subir(Long prendaId, List<MultipartFile> archivos) {
        AlmacenamientoProperties.Imagenes limites = props.imagenes();
        if (archivos == null || archivos.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Selecciona al menos una imagen", "archivos");
        }
        if (archivos.size() > limites.maxPorCarga()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Puedes subir máximo " + limites.maxPorCarga() + " imágenes a la vez", "archivos");
        }
        // Bloqueo pesimista: dos cargas concurrentes no pueden superar el máximo por prenda
        Prenda prenda = prendaRepository.bloquearPropia(prendaId, PrendaService.vendedorActual())
                .orElseThrow(PrendaService::noEncontrada);
        int actuales = prenda.getImagenes().size();
        if (actuales + archivos.size() > limites.maxPorPrenda()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Una prenda admite máximo " + limites.maxPorPrenda()
                    + " imágenes (ya tiene " + actuales + ")", "archivos");
        }

        List<ArchivoValidado> validados = archivos.stream().map(a -> validar(a, limites)).toList();

        List<String> subidas = new ArrayList<>();
        alFinalizarTransaccion(confirmada -> {
            if (!confirmada) {
                subidas.forEach(this::eliminarDelAlmacenamiento);
            }
        });

        int orden = prenda.getImagenes().stream().mapToInt(ImagenPrenda::getOrden).max().orElse(-1) + 1;
        LocalDateTime ahora = LocalDateTime.now();
        for (ArchivoValidado archivo : validados) {
            String clave = "prendas/" + prenda.getId() + "/" + UUID.randomUUID() + "." + archivo.tipo().extension();
            String url = almacenamiento.subir(clave, archivo.datos(), archivo.tipo().contentType());
            subidas.add(clave);
            prenda.getImagenes().add(ImagenPrenda.builder()
                    .prenda(prenda).url(url).clave(clave).orden(orden++).fechaCarga(ahora).build());
        }
        prenda.setFechaActualizacion(ahora);
        prendaRepository.flush();
        return prenda.getImagenes().stream().map(PrendaResponse.Imagen::from).toList();
    }

    @Transactional
    public void eliminar(Long prendaId, Long imagenId) {
        Prenda prenda = prendaRepository.bloquearPropia(prendaId, PrendaService.vendedorActual())
                .orElseThrow(PrendaService::noEncontrada);
        ImagenPrenda imagen = buscarImagen(prenda, imagenId);
        prenda.getImagenes().remove(imagen);
        prenda.setFechaActualizacion(LocalDateTime.now());
        alFinalizarTransaccion(confirmada -> {
            if (confirmada) {
                eliminarDelAlmacenamiento(imagen.getClave());
            }
        });
    }

    /** La imagen principal es la primera; se mueve al inicio y se renumera el orden. */
    @Transactional
    public List<PrendaResponse.Imagen> marcarPrincipal(Long prendaId, Long imagenId) {
        Prenda prenda = prendaRepository.bloquearPropia(prendaId, PrendaService.vendedorActual())
                .orElseThrow(PrendaService::noEncontrada);
        ImagenPrenda principal = buscarImagen(prenda, imagenId);
        List<ImagenPrenda> ordenadas = new ArrayList<>(prenda.getImagenes());
        ordenadas.remove(principal);
        ordenadas.addFirst(principal);
        for (int i = 0; i < ordenadas.size(); i++) {
            ordenadas.get(i).setOrden(i);
        }
        prenda.getImagenes().sort((a, b) -> Integer.compare(a.getOrden(), b.getOrden()));
        prenda.setFechaActualizacion(LocalDateTime.now());
        return prenda.getImagenes().stream().map(PrendaResponse.Imagen::from).toList();
    }

    private static ImagenPrenda buscarImagen(Prenda prenda, Long imagenId) {
        return prenda.getImagenes().stream()
                .filter(i -> i.getId().equals(imagenId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));
    }

    private static ArchivoValidado validar(MultipartFile archivo, AlmacenamientoProperties.Imagenes limites) {
        String nombre = nombreVisible(archivo);
        if (archivo.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El archivo " + nombre + " está vacío", "archivos");
        }
        if (archivo.getSize() > limites.maxTamano().toBytes()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El archivo " + nombre + " supera el tamaño máximo de "
                    + limites.maxTamano().toMegabytes() + " MB", "archivos");
        }
        byte[] datos;
        try {
            datos = archivo.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "No fue posible leer el archivo " + nombre, "archivos");
        }
        TipoImagen tipo = TipoImagen.detectar(datos).orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                "El archivo " + nombre + " no es una imagen JPG, PNG o WEBP", "archivos"));
        return new ArchivoValidado(datos, tipo);
    }

    private static String nombreVisible(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.isBlank()) {
            return "seleccionado";
        }
        nombre = nombre.replaceAll("[\\p{Cntrl}]", "");
        return nombre.length() > 60 ? nombre.substring(0, 57) + "..." : nombre;
    }

    private void eliminarDelAlmacenamiento(String clave) {
        try {
            almacenamiento.eliminar(clave);
        } catch (RuntimeException ex) {
            // Un objeto huérfano no afecta al catálogo; se registra para limpiarlo después
            log.warn("No se pudo eliminar del almacenamiento la imagen {}", clave, ex);
        }
    }

    private static void alFinalizarTransaccion(Consumer<Boolean> accion) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                accion.accept(status == STATUS_COMMITTED);
            }
        });
    }

    private record ArchivoValidado(byte[] datos, TipoImagen tipo) {
    }
}
