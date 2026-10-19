package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DireccionClienteRepository;
import co.edu.uniquindio.littlestyle.shared.event.CuentaEliminadaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Borra la dirección guardada cuando el cliente elimina su cuenta. */
@Component
@RequiredArgsConstructor
public class EliminacionDireccionCuentaListener {

    private final DireccionClienteRepository direccionRepository;

    /** Elimina la dirección del usuario dentro de la transacción de eliminación de la cuenta. */
    @EventListener
    public void alEliminarCuenta(CuentaEliminadaEvent evento) {
        direccionRepository.deleteByClienteId(evento.usuarioId());
    }
}
