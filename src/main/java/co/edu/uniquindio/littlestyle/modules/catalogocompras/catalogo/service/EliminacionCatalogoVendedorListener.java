package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.repository.ItemCarritoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.storage.AlmacenamientoImagenes;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.repository.AlertaReabastecimientoRepository;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.ProductoVistaRepository;
import co.edu.uniquindio.littlestyle.shared.event.CuentaEliminadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Borra el catálogo de un vendedor cuando elimina su  cuenta (o el administrador la elimina): prendas, tallas, colores, composición, imágenes, alertas, lineas de carrito y vistas.
 * Los pedidos ya hechos se  conservan porque guardan nombre y precio de la prenda al comprar. Para un cliente no hace nada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EliminacionCatalogoVendedorListener {

    private final PrendaRepository prendaRepository;
    private final ItemCarritoRepository itemCarritoRepository;
    private  final AlertaReabastecimientoRepository alertaRepository;
    private final ProductoVistaRepository productoVistaRepository;
    private final AlmacenamientoImagenes almacenamiento;

    /** Se ejecuta dentro de la transacción de elimincación de cuenta, los archivos se borransolo si hace commit. */
    @EventListener
    public void alEliminarCuenta(CuentaEliminadaEvent evento) {
        List<Prenda> prendas = prendaRepository.findAllByVendedorIdOrderByFechaActualizacionDesc(evento.usuarioId());
        if (prendas.isEmpty()) {
            return;
        }
        List<Long> ids = prendas.stream().map(Prenda::getId).toList();
        List<String> claves = prendas.stream().flatMap(p -> p.getImagenes().stream()).map(ImagenPrenda::getClave).toList();

        itemCarritoRepository.borrarPorPrendaIds(ids);
        alertaRepository.borrarPorPrendaIds(ids);
        productoVistaRepository.borrarPorPrendaIds(ids);
        prendaRepository.deleteAll(prendas);
        prendaRepository.flush();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                claves.forEach(clave -> {
                    try {
                        almacenamiento.eliminar(clave);
                    } catch (RuntimeException ex) {
                        log.warn("No se pudo eliminar del almacenamiento la imagen {}", clave, ex);
                    }
                });
            }
        });
    }
}
