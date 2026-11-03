package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.repository.ItemCarritoRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ImagenPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.storage.AlmacenamientoImagenes;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.inventario.repository.AlertaReabastecimientoRepository;
import co.edu.uniquindio.littlestyle.modules.dashboard.repository.ProductoVistaRepository;
import co.edu.uniquindio.littlestyle.shared.event.CuentaEliminadaEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.VENDEDOR_ID;
import static co.edu.uniquindio.littlestyle.modules.catalogocompras.CatalogoFixtures.prenda;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EliminacionCatalogoVendedorListenerTest {

    @Mock private PrendaRepository prendaRepository;
    @Mock private ItemCarritoRepository itemCarritoRepository;
    @Mock private AlertaReabastecimientoRepository alertaRepository;
    @Mock private ProductoVistaRepository productoVistaRepository;
    @Mock private AlmacenamientoImagenes almacenamiento;
    @InjectMocks private EliminacionCatalogoVendedorListener listener;

    @BeforeEach
    void iniciarSincronizacion() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void limpiar() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void borraPrendasYDependenciasYLasImagenesSoloTrasElCommit() {
        Prenda prenda = prenda(10L, 5, 3, 3);
        prenda.getImagenes().add(ImagenPrenda.builder().id(5L).orden(0).url("u")
                .clave("prendas/10/a.png").fechaCarga(LocalDateTime.now()).build());
        when(prendaRepository.findAllByVendedorIdOrderByFechaActualizacionDesc(VENDEDOR_ID))
                .thenReturn(List.of(prenda));

        listener.alEliminarCuenta(new CuentaEliminadaEvent(VENDEDOR_ID));

        InOrder orden = inOrder(itemCarritoRepository, alertaRepository, productoVistaRepository, prendaRepository);
        orden.verify(itemCarritoRepository).borrarPorPrendaIds(List.of(10L));
        orden.verify(alertaRepository).borrarPorPrendaIds(List.of(10L));
        orden.verify(productoVistaRepository).borrarPorPrendaIds(List.of(10L));
        orden.verify(prendaRepository).deleteAll(List.of(prenda));
        verify(almacenamiento, never()).eliminar(anyString());

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        verify(almacenamiento).eliminar("prendas/10/a.png");
    }

    @Test
    void siElAlmacenamientoFallaNoRompeLaEliminacion() {
        Prenda prenda = prenda(10L, 5, 3, 3);
        prenda.getImagenes().add(ImagenPrenda.builder().id(5L).orden(0).url("u")
                .clave("prendas/10/a.png").fechaCarga(LocalDateTime.now()).build());
        when(prendaRepository.findAllByVendedorIdOrderByFechaActualizacionDesc(VENDEDOR_ID))
                .thenReturn(List.of(prenda));
        org.mockito.Mockito.doThrow(new RuntimeException("S3 caído")).when(almacenamiento).eliminar(anyString());

        listener.alEliminarCuenta(new CuentaEliminadaEvent(VENDEDOR_ID));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit); // no lanza
    }

    @Test
    void conUnClienteSinPrendasNoHaceNada() {
        when(prendaRepository.findAllByVendedorIdOrderByFechaActualizacionDesc(7L)).thenReturn(List.of());

        listener.alEliminarCuenta(new CuentaEliminadaEvent(7L));

        verifyNoInteractions(itemCarritoRepository, alertaRepository, productoVistaRepository, almacenamiento);
        verify(prendaRepository, never()).deleteAll(anyCollection());
        verify(prendaRepository, never()).flush();
    }
}