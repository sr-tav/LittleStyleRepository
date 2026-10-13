package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DireccionClienteRepository;
import co.edu.uniquindio.littlestyle.shared.event.CuentaEliminadaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EliminacionDireccionCuentaListener {

    private final DireccionClienteRepository direccionRepository;

    @EventListener
    public void alEliminarCuenta(CuentaEliminadaEvent evento) {
        direccionRepository.deleteByClienteId(evento.usuarioId());
    }
}
